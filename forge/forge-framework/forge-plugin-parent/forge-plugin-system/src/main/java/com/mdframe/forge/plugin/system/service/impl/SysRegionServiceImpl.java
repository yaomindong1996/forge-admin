package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.service.ISysRegionService;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;
import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 行政区划Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

    private final SysRegionMapper regionMapper;
    private final ICacheService cacheService;

    private static final String REGION_TREE_CACHE_PREFIX = "region:treeAll:";
    private static final long REGION_TREE_CACHE_TTL_HOURS = 24;

    @Override
    public List<SysRegionTreeVO> selectRegionTree() {
        // 1. 查询第一级（省级，parentCode为空或null）
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(SysRegion::getParentCode).or().eq(SysRegion::getParentCode, ""))
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> rootRegions = regionMapper.selectList(wrapper);

        if (rootRegions.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 一次性查询所有parentCode，判断哪些code有子节点
        Set<String> codesWithChildren = getCodesWithChildren(rootRegions.stream()
                .map(SysRegion::getCode)
                .collect(Collectors.toList()));

        // 3. 构建VO
        return rootRegions.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            vo.setHasChildren(codesWithChildren.contains(region.getCode()));
            return vo;
        }).toList();
    }

    /**
     * 批量查询哪些code有子节点（一次SQL）
     */
    private Set<String> getCodesWithChildren(List<String> parentCodes) {
        if (parentCodes.isEmpty()) {
            return new HashSet<>();
        }

        // 查询所有parentCode在这些code中的记录
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysRegion::getParentCode)
                .in(SysRegion::getParentCode, parentCodes)
                .isNotNull(SysRegion::getParentCode);

        List<SysRegion> children = regionMapper.selectList(wrapper);

        // 返回有子节点的code集合
        return children.stream()
                .map(SysRegion::getParentCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    @Override
    public SysRegion selectRegionByCode(String code) {
        return regionMapper.selectById(code);
    }

    @Override
    public boolean insertRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        boolean result = this.save(region);
        if (result) {
            refreshRegionTreeCache();
        }
        return result;
    }

    @Override
    public boolean updateRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        boolean result = this.updateById(region);
        if (result) {
            refreshRegionTreeCache();
        }
        return result;
    }

    @Override
    public boolean deleteRegionByCode(String code) {
        boolean result = regionMapper.deleteById(code) > 0;
        if (result) {
            refreshRegionTreeCache();
        }
        return result;
    }

    @Override
    public List<SysRegion> selectChildrenByParentCode(String parentCode) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode).orderByAsc(SysRegion::getLevel).orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }

    @Override
    public List<SysRegionTreeVO> selectChildrenVOByParentCode(String parentCode) {
        // 1. 查询子节点
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> children = regionMapper.selectList(wrapper);

        if (children.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查询哪些code有子节点
        Set<String> codesWithChildren = getCodesWithChildren(children.stream()
                .map(SysRegion::getCode)
                .collect(Collectors.toList()));

        // 3. 构建VO
        return children.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            vo.setHasChildren(codesWithChildren.contains(region.getCode()));
            return vo;
        }).toList();
    }

    @Override
    public List<SysRegion> searchRegionByName(String name) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SysRegion::getName, name).orderByAsc(SysRegion::getLevel).orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }

    @Override
    public String buildAncestors(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        StringBuilder ancestors = new StringBuilder();
        String currentCode = code;
        while (StrUtil.isNotBlank(currentCode)) {
            SysRegion region = regionMapper.selectById(currentCode);
            if (region == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = region.getParentCode();
        }
        return ancestors.toString();
    }

    // ==================== 完整区划树加载（含虚拟组织+数据权限） ====================

    @Override
    public List<SysRegionTreeVO> selectRegionTreeAll(String rootCode, Boolean dataRight) {
        long startTime = System.currentTimeMillis();
        log.debug("开始查询行政区划树, rootCode={}, dataRight={}", rootCode, dataRight);

        try {
            List<SysRegion> allList;

            // ========== 数据权限场景 ==========
            if (dataRight != null && dataRight) {
                // 有数据权限限制：通过 listSysRegion 查询，数据权限拦截器会自动追加过滤条件
                if (StrUtil.isBlank(rootCode)) {
                    rootCode = "150000";
                }
                allList = regionMapper.listSysRegion(null, null, rootCode);
                log.debug("查询到 {} 条数据（含数据权限）", allList != null ? allList.size() : 0);

                // 添加本级虚拟节点
                List<SysRegionTreeVO> voList = convertToVoList(allList);
                addVirtualLevelNodes(voList);
                List<SysRegionTreeVO> tree = buildRegionTree(voList);
                log.debug("构建树完成，耗时: {} ms", System.currentTimeMillis() - startTime);
                return tree;
            }

            // ========== 无数据权限场景 ==========
            // 查缓存
            String cacheKey = REGION_TREE_CACHE_PREFIX + (StrUtil.isBlank(rootCode) ? "all" : rootCode);
            List<SysRegionTreeVO> cached = cacheService.get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }

            // 通过 listSysRegionNoRight 查询，不受数据权限拦截器控制
            allList = regionMapper.listSysRegionNoRight(null, null, rootCode);
            log.debug("查询到 {} 条数据（无数据权限）", allList != null ? allList.size() : 0);

            // 添加本级虚拟节点
            List<SysRegionTreeVO> voList = convertToVoList(allList);
            addVirtualLevelNodes(voList);
            List<SysRegionTreeVO> tree = buildRegionTree(voList);

            // 写缓存
            cacheService.set(cacheKey, tree, REGION_TREE_CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("构建树完成，耗时: {} ms", System.currentTimeMillis() - startTime);
            return tree;
        } catch (Exception e) {
            log.error("查询行政区划树失败", e);
            throw e;
        }
    }

    /**
     * 将实体列表转换为VO列表
     */
    private List<SysRegionTreeVO> convertToVoList(List<SysRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return new ArrayList<>();
        }
        return regions.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void refreshRegionTreeCache() {
        cacheService.deletePattern(REGION_TREE_CACHE_PREFIX + "*");
    }

    /**
     * 添加虚拟本级节点
     * 为自治区级和盟市级账号添加虚拟顶级节点
     *
     * 自治区账号场景：
     *   - 创建虚拟顶级节点 150000ALL（内蒙古自治区）
     *   - 实际自治区节点改名为"自治区本级"，parentCode指向150000ALL
     *   - 每个盟市创建虚拟节点 cityCode+"ALL"
     *   - 实际盟市节点改名加"本级"后缀，parentCode指向cityCode+"ALL"
     *   - 区县节点parentCode指向对应盟市虚拟节点 cityCode+"ALL"
     *
     * 盟市账号场景：
     *   - 创建虚拟顶级节点 cityCode+"ALL"
     *   - 实际盟市节点改名加"本级"后缀，parentCode指向cityCode+"ALL"
     *   - 区县节点parentCode指向cityCode+"ALL"
     */
    private void addVirtualLevelNodes(List<SysRegionTreeVO> originalList) {
        if (originalList == null || originalList.isEmpty()) {
            return;
        }

        List<SysRegionTreeVO> cityRegions = new ArrayList<>();
        List<SysRegionTreeVO> districtRegions = new ArrayList<>();
        SysRegionTreeVO autonomousRegion = null;

        // 一次遍历完成分类
        for (SysRegionTreeVO region : originalList) {
            String code = region.getCode();

            if ("150000".equals(code)) {
                autonomousRegion = region;
            } else if (region.getLevel() != null && region.getLevel() == 2
                    && "150000".equals(region.getParentCode())) {
                cityRegions.add(region);
            } else if (region.getLevel() != null && region.getLevel() == 3) {
                districtRegions.add(region);
            }
        }

        boolean hasAutonomousRegion = (autonomousRegion != null);

        if (hasAutonomousRegion) {
            // ========== 自治区账号逻辑 ==========
            // 创建虚拟顶级节点 150000ALL
            SysRegionTreeVO virtualTopRegion = new SysRegionTreeVO();
            virtualTopRegion.setCode("150000ALL");
            virtualTopRegion.setName("内蒙古自治区");
            virtualTopRegion.setLevel(1);
            virtualTopRegion.setParentCode(null);
            virtualTopRegion.setFullName("内蒙古自治区");
            virtualTopRegion.setCityCode("150000ALL");
            virtualTopRegion.setSortOrder(0);
            originalList.add(virtualTopRegion);

            // 修改实际自治区节点 - 标记为本级节点
            autonomousRegion.setName("自治区本级");
            autonomousRegion.setParentCode("150000ALL");
            autonomousRegion.setSortOrder(1);

            // 处理盟市节点
            int cityIndex = 0;
            for (SysRegionTreeVO cityRegion : cityRegions) {
                String cityCode = cityRegion.getCode();

                // 创建虚拟盟市节点
                SysRegionTreeVO virtualCityRegion = new SysRegionTreeVO();
                virtualCityRegion.setCode(cityCode + "ALL");
                virtualCityRegion.setName(cityRegion.getName());
                virtualCityRegion.setLevel(2);
                virtualCityRegion.setParentCode("150000ALL");
                virtualCityRegion.setFullName(cityRegion.getFullName());
                virtualCityRegion.setCityCode(cityCode + "ALL");
                virtualCityRegion.setSortOrder(1000 + cityIndex * 1000);
                originalList.add(virtualCityRegion);

                // 修改实际盟市节点 - 标记为本级节点
                cityRegion.setName(cityRegion.getName() + "本级");
                cityRegion.setParentCode(cityCode + "ALL");
                cityRegion.setSortOrder(1);

                cityIndex++;
            }

            // 修改区县节点的父节点，并设置排序值
            for (SysRegionTreeVO districtRegion : districtRegions) {
                String parentCode = districtRegion.getParentCode();
                if (parentCode != null && parentCode.length() == 6 && parentCode.startsWith("150")) {
                    districtRegion.setParentCode(parentCode + "ALL");
                    districtRegion.setSortOrder(Integer.parseInt(districtRegion.getCode()));
                }
            }
        } else {
            // ========== 盟市账号逻辑 ==========
            SysRegionTreeVO cityRegion = null;
            for (SysRegionTreeVO region : originalList) {
                if (region.getLevel() != null && region.getLevel() == 2) {
                    cityRegion = region;
                    break;
                }
            }

            if (cityRegion != null) {
                String cityCode = cityRegion.getCode();

                // 创建虚拟顶级节点
                SysRegionTreeVO virtualCityRegion = new SysRegionTreeVO();
                virtualCityRegion.setCode(cityCode + "ALL");
                virtualCityRegion.setName(cityRegion.getName());
                virtualCityRegion.setLevel(2);
                virtualCityRegion.setParentCode(null);
                virtualCityRegion.setFullName(cityRegion.getFullName());
                virtualCityRegion.setCityCode(cityCode + "ALL");
                virtualCityRegion.setSortOrder(0);
                originalList.add(virtualCityRegion);

                // 修改实际盟市节点 - 标记为本级节点
                cityRegion.setName(cityRegion.getName() + "本级");
                cityRegion.setParentCode(cityCode + "ALL");
                cityRegion.setSortOrder(1);

                // 修改区县节点的父节点
                for (SysRegionTreeVO districtRegion : districtRegions) {
                    if (cityCode.equals(districtRegion.getParentCode())) {
                        districtRegion.setParentCode(cityCode + "ALL");
                        districtRegion.setSortOrder(Integer.parseInt(districtRegion.getCode()));
                    }
                }
            }
        }
    }

    /**
     * 构建树形结构（含虚拟节点合并逻辑）
     *
     * 关键点：虚拟节点(code以"ALL"结尾)的子节点合并逻辑
     * 例如 150100ALL 的子节点 = 自身的children + 实际150100的children
     */
    private List<SysRegionTreeVO> buildRegionTree(List<SysRegionTreeVO> allList) {
        if (allList == null || allList.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用HashMap存储节点，提高查找效率
        Map<String, SysRegionTreeVO> codeMap = new HashMap<>((int) (allList.size() / 0.75) + 1);
        Map<String, List<SysRegionTreeVO>> parentCodeMap = new HashMap<>();
        Set<String> existCodeSet = new HashSet<>((int) (allList.size() / 0.75) + 1);

        // 第一次遍历：构建索引
        for (SysRegionTreeVO region : allList) {
            String code = region.getCode();
            codeMap.put(code, region);
            existCodeSet.add(code);

            String parentCode = region.getParentCode();
            String parentKey = parentCode == null ? "" : parentCode;
            parentCodeMap.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(region);
        }

        List<SysRegionTreeVO> rootList = new ArrayList<>();
        boolean hasNativeRoot = parentCodeMap.containsKey("");

        // 第二次遍历：设置子节点和根节点
        for (SysRegionTreeVO region : allList) {
            String regionCode = region.getCode();

            // 设置子节点
            List<SysRegionTreeVO> children = parentCodeMap.getOrDefault(regionCode, Collections.emptyList());

            // 处理虚拟节点的子节点合并
            if (regionCode != null && regionCode.endsWith("ALL")) {
                String actualCode = regionCode.replace("ALL", "");
                List<SysRegionTreeVO> actualChildren = parentCodeMap.getOrDefault(actualCode, Collections.emptyList());
                if (!actualChildren.isEmpty()) {
                    List<SysRegionTreeVO> allChildren = new ArrayList<>(children);
                    allChildren.addAll(actualChildren);
                    children = allChildren;
                }
            }

            // 按sortOrder排序，sortOrder为null则按code排序
            if (!children.isEmpty()) {
                children = new ArrayList<>(children);
                children.sort((a, b) -> {
                    Integer sortOrderA = a.getSortOrder();
                    Integer sortOrderB = b.getSortOrder();

                    if (sortOrderA != null && sortOrderB != null) {
                        return sortOrderA.compareTo(sortOrderB);
                    }
                    if (sortOrderA != null) return -1;
                    if (sortOrderB != null) return 1;
                    return a.getCode().compareTo(b.getCode());
                });
            }

            region.setChildren(children);
            region.setHasChildren(!children.isEmpty());

            // 判断是否为根节点
            if (!hasNativeRoot) {
                // 虚拟组织场景（无原生根节点）
                boolean isRoot = false;
                if (region.getLevel() != null && region.getLevel() == 2) {
                    isRoot = true;
                } else if (region.getLevel() != null && region.getLevel() == 3) {
                    String parentCode = region.getParentCode();
                    isRoot = parentCode == null || !existCodeSet.contains(parentCode);
                } else if (regionCode.endsWith("ALL")) {
                    String actualParentCode = getActualParentCode(regionCode);
                    isRoot = actualParentCode == null || !existCodeSet.contains(actualParentCode);
                }

                if (isRoot) {
                    rootList.add(region);
                }
            } else {
                // 原生顶级节点
                if (region.getParentCode() == null) {
                    rootList.add(region);
                }
            }
        }

        return rootList;
    }

    /**
     * 获取虚拟节点的实际父节点编码
     */
    private String getActualParentCode(String virtualCode) {
        if (virtualCode == null) return null;

        // 自治区虚拟顶级节点 150000ALL
        if ("150000ALL".equals(virtualCode)) {
            return null;
        }

        // 盟市虚拟顶级节点 如150100ALL
        if (virtualCode.endsWith("ALL") && virtualCode.length() == 8) {
            return "150000ALL";
        }

        return null;
    }
}
