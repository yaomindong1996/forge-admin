package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysNoticeDTO;
import com.mdframe.forge.plugin.system.dto.SysNoticeQuery;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.ISysNoticeService;
import com.mdframe.forge.plugin.system.vo.SysNoticeVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.trans.annotation.DictTranslate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知公告Service实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysFileMetadataMapper fileMetadataMapper;
    private final SysNoticeOrgMapper noticeOrgMapper;
    private final SysNoticeReadRecordMapper noticeReadRecordMapper;
    private final SysOrgMapper orgMapper;

    @Override
    @DictTranslate
    public Page<SysNotice> selectNoticePage(PageQuery pageQuery, SysNoticeQuery query) {
        log.info("分页查询参数:{}, 查询条件:{}", pageQuery, query);
        LambdaQueryWrapper<SysNotice> wrapper = buildQueryWrapper(query);
        // 排序：置顶优先，然后按发布时间倒序
        wrapper.orderByDesc(SysNotice::getIsTop, SysNotice::getTopSort, SysNotice::getPublishTime);
        return noticeMapper.selectPage(pageQuery.toPage(), wrapper);
    }

    @Override
    @DictTranslate
    public List<SysNotice> selectNoticeList(SysNoticeQuery query) {
        log.info("查询参数:{}", query);
        LambdaQueryWrapper<SysNotice> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SysNotice::getIsTop, SysNotice::getTopSort, SysNotice::getPublishTime);
        return noticeMapper.selectList(wrapper);
    }

    @Override
    @DictTranslate
    public SysNoticeVO selectNoticeById(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            return null;
        }

        SysNoticeVO vo = new SysNoticeVO();
        BeanUtil.copyProperties(notice, vo);

        // 处理附件信息
        if (StrUtil.isNotBlank(notice.getAttachmentIds())) {
            String[] fileIds = notice.getAttachmentIds().split(",");
            List<Long> fileIdList = new ArrayList<>();
            for (String fileIdStr : fileIds) {
                try {
                    fileIdList.add(Long.parseLong(fileIdStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("无效的文件ID: {}", fileIdStr);
                }
            }
            if (!fileIdList.isEmpty()) {
                List<SysFileMetadata> files = fileMetadataMapper.selectBatchIds(fileIdList);
                Map<Long, SysFileMetadata> fileMap = files.stream()
                        .collect(Collectors.toMap(SysFileMetadata::getId, Function.identity(), (left, right) -> left));
                List<SysNoticeVO.AttachmentInfo> attachments = new ArrayList<>();
                for (Long fileId : fileIdList) {
                    SysFileMetadata fileMetadata = fileMap.get(fileId);
                    if (fileMetadata != null) {
                        SysNoticeVO.AttachmentInfo attachment = new SysNoticeVO.AttachmentInfo();
                        attachment.setFileId(fileId);
                        attachment.setFileName(fileMetadata.getOriginalName());
                        attachment.setFileSize(fileMetadata.getFileSize());
                        attachments.add(attachment);
                    }
                }
                vo.setAttachments(attachments);
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertNotice(SysNoticeDTO dto) {
        SysNotice notice = new SysNotice();
        BeanUtil.copyProperties(dto, notice);
        
        // 设置默认值
        if (notice.getPublishStatus() == null) {
            notice.setPublishStatus(0); // 默认草稿状态
        }
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        if (notice.getTopSort() == null) {
            notice.setTopSort(0);
        }
        if (notice.getReadCount() == null) {
            notice.setReadCount(0);
        }
        if (notice.getPublishScope() == null) {
            notice.setPublishScope(0); // 默认全部组织
        }
        
        boolean result = noticeMapper.insert(notice) > 0;
        
        // 如果指定了组织，保存组织关联
        if (result && dto.getPublishScope() != null && dto.getPublishScope() == 1 
                && dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
            saveNoticeOrgRelations(notice.getNoticeId(), dto.getOrgIds());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNotice(SysNoticeDTO dto) {
        SysNotice notice = new SysNotice();
        BeanUtil.copyProperties(dto, notice);
        boolean result = noticeMapper.updateById(notice) > 0;
        
        // 更新组织关联
        if (result && dto.getPublishScope() != null) {
            // 先删除原有关联
            LambdaQueryWrapper<SysNoticeOrg> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysNoticeOrg::getNoticeId, dto.getNoticeId());
            noticeOrgMapper.delete(wrapper);
            
            // 如果是指定组织，重新保存关联
            if (dto.getPublishScope() == 1 && dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
                saveNoticeOrgRelations(dto.getNoticeId(), dto.getOrgIds());
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNoticeById(Long noticeId) {
        return noticeMapper.deleteById(noticeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNoticeByIds(Long[] noticeIds) {
        return noticeMapper.deleteBatchIds(Arrays.asList(noticeIds)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishNotice(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            log.warn("公告不存在: {}", noticeId);
            return false;
        }

        // 更新发布状态
        notice.setPublishStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        
        // 设置发布人信息
        try {
            Long userId = SessionHelper.getUserId();
            String username = SessionHelper.getUsername();
            notice.setPublisherId(userId);
            notice.setPublisherName(username);
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }

        return noticeMapper.updateById(notice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeNotice(Long noticeId) {
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setPublishStatus(2); // 已撤回
        return noticeMapper.updateById(notice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean topNotice(Long noticeId, Integer isTop, Integer topSort) {
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setIsTop(isTop);
        if (topSort != null) {
            notice.setTopSort(topSort);
        }
        return noticeMapper.updateById(notice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseReadCount(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            return false;
        }
        notice.setReadCount(notice.getReadCount() == null ? 1 : notice.getReadCount() + 1);
        return noticeMapper.updateById(notice) > 0;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SysNotice> buildQueryWrapper(SysNoticeQuery query) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getNoticeTitle()), SysNotice::getNoticeTitle, query.getNoticeTitle())
                .eq(StringUtils.isNotBlank(query.getNoticeType()), SysNotice::getNoticeType, query.getNoticeType())
                .eq(query.getPublishStatus() != null, SysNotice::getPublishStatus, query.getPublishStatus())
                .eq(query.getIsTop() != null, SysNotice::getIsTop, query.getIsTop())
                .like(StringUtils.isNotBlank(query.getPublisherName()), SysNotice::getPublisherName, query.getPublisherName())
                .ge(query.getEffectiveTimeStart() != null, SysNotice::getEffectiveTime, query.getEffectiveTimeStart())
                .le(query.getEffectiveTimeEnd() != null, SysNotice::getEffectiveTime, query.getEffectiveTimeEnd());
        return wrapper;
    }

    /**
     * 保存公告-组织关联
     */
    private void saveNoticeOrgRelations(Long noticeId, List<Long> orgIds) {
        for (Long orgId : orgIds) {
            SysNoticeOrg noticeOrg = new SysNoticeOrg();
            noticeOrg.setNoticeId(noticeId);
            noticeOrg.setOrgId(orgId);
            noticeOrgMapper.insert(noticeOrg);
        }
    }

    @Override
    public Page<SysNoticeVO> selectUserNoticePage(PageQuery pageQuery, SysNoticeQuery query) {
        return selectUserNoticePage(pageQuery, query, null);
    }

    @Override
    public SysNoticeVO selectUserNoticeById(Long noticeId) {
        if (noticeId == null) {
            return null;
        }
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageSize(1);
        List<SysNoticeVO> records = selectUserNoticePage(pageQuery, new SysNoticeQuery(), noticeId).getRecords();
        return records.isEmpty() ? null : records.get(0);
    }

    private Page<SysNoticeVO> selectUserNoticePage(PageQuery pageQuery, SysNoticeQuery query, Long noticeId) {
        try {
            Long userId = SessionHelper.getUserId();
            Page<SysNotice> page = noticeMapper.selectUserNoticePage(pageQuery.toPage(), query, userId, noticeId);
            
            // 批量查询已读状态：一次 IN 查询返回用户已读的 noticeId 集合
            List<Long> noticeIds = page.getRecords().stream()
                    .map(SysNotice::getNoticeId)
                    .collect(Collectors.toList());
            Set<Long> readNoticeIds;
            if (noticeIds.isEmpty()) {
                readNoticeIds = Collections.emptySet();
            } else {
                readNoticeIds = new HashSet<>(noticeReadRecordMapper.selectReadNoticeIds(userId, noticeIds));
            }

            // 批量查询附件：收集所有 fileId，一次 selectBatchIds
            List<Long> allFileIds = page.getRecords().stream()
                    .filter(n -> StrUtil.isNotBlank(n.getAttachmentIds()))
                    .flatMap(n -> Arrays.stream(n.getAttachmentIds().split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
                    })
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, SysFileMetadata> fileMap;
            if (allFileIds.isEmpty()) {
                fileMap = Collections.emptyMap();
            } else {
                fileMap = fileMetadataMapper.selectBatchIds(allFileIds).stream()
                        .collect(Collectors.toMap(SysFileMetadata::getId, Function.identity(), (left, right) -> left));
            }
            
            final Set<Long> finalReadNoticeIds = readNoticeIds;
            final Map<Long, SysFileMetadata> finalFileMap = fileMap;
            List<SysNoticeVO> voList = page.getRecords().stream().map(notice -> {
                SysNoticeVO vo = new SysNoticeVO();
                BeanUtil.copyProperties(notice, vo);
                
                // 已读状态从批量查询结果中获取
                vo.setIsRead(finalReadNoticeIds.contains(notice.getNoticeId()) ? 1 : 0);
                
                // 附件信息从批量查询结果中获取
                if (StrUtil.isNotBlank(notice.getAttachmentIds())) {
                    String[] fileIds = notice.getAttachmentIds().split(",");
                    List<SysNoticeVO.AttachmentInfo> attachments = new ArrayList<>();
                    for (String fileIdStr : fileIds) {
                        try {
                            Long fileId = Long.parseLong(fileIdStr.trim());
                            SysFileMetadata fileMetadata = finalFileMap.get(fileId);
                            if (fileMetadata != null) {
                                SysNoticeVO.AttachmentInfo attachment = new SysNoticeVO.AttachmentInfo();
                                attachment.setFileId(fileId);
                                attachment.setFileName(fileMetadata.getOriginalName());
                                attachment.setFileSize(fileMetadata.getFileSize());
                                attachments.add(attachment);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("无效的文件ID: {}", fileIdStr);
                        }
                    }
                    vo.setAttachments(attachments);
                }
                
                return vo;
            }).collect(Collectors.toList());
            
            Page<SysNoticeVO> voPage = new Page<>();
            voPage.setCurrent(page.getCurrent());
            voPage.setSize(page.getSize());
            voPage.setTotal(page.getTotal());
            voPage.setRecords(voList);
            return voPage;
        } catch (Exception e) {
            log.error("查询用户公告列表失败", e);
            throw e;
        }
    }

    @Override
    public Integer getUserUnreadCount() {
        return noticeMapper.countUserUnreadNotices(SessionHelper.getUserId());
    }
}
