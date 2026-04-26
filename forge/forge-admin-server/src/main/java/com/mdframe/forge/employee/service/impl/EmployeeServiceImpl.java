package com.mdframe.forge.employee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.employee.mapper.EmployeeMapper;
import com.mdframe.forge.employee.service.IEmployeeService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.trans.annotation.DictTranslate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 员工信息表Service实现类
 * 
 * @author Forge Generator
 * @date 2026-04-26 19:08:42
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements IEmployeeService {

    private final EmployeeMapper employeeMapper;

    @Override
    @DictTranslate
    public Page<Employee> selectEmployeePage(PageQuery pageQuery, EmployeeQuery query) {
        log.info("分页查询参数:{}, 查询条件:{}", pageQuery, query);
        LambdaQueryWrapper<Employee> wrapper = buildQueryWrapper(query);
        return employeeMapper.selectPage(pageQuery.toPage(), wrapper);
    }

    @Override
    @DictTranslate
    public List<Employee> selectEmployeeList(EmployeeQuery query) {
        log.info("查询参数:{}", query);
        LambdaQueryWrapper<Employee> wrapper = buildQueryWrapper(query);
        return employeeMapper.selectList(wrapper);
    }

    @Override
    public Employee selectEmployeeById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertEmployee(EmployeeDTO dto) {
        Employee employee = new Employee();
        BeanUtil.copyProperties(dto, employee);
        return employeeMapper.insert(employee) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEmployee(EmployeeDTO dto) {
        Employee employee = new Employee();
        BeanUtil.copyProperties(dto, employee);
        return employeeMapper.updateById(employee) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEmployeeById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEmployeeByIds(Long[] ids) {
        return employeeMapper.deleteBatchIds(Arrays.asList(ids)) > 0;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Employee> buildQueryWrapper(EmployeeQuery query) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getId() != null, Employee::getId, query.getId());
        wrapper.like(StringUtils.isNotBlank(query.getEmpName()), Employee::getEmpName, query.getEmpName());
        wrapper.like(StringUtils.isNotBlank(query.getEmpNo()), Employee::getEmpNo, query.getEmpNo());
        wrapper.eq(query.getDeptId() != null, Employee::getDeptId, query.getDeptId());
        wrapper.like(StringUtils.isNotBlank(query.getPosition()), Employee::getPosition, query.getPosition());
        wrapper.eq(query.getHireDate() != null, Employee::getHireDate, query.getHireDate());
        wrapper.like(StringUtils.isNotBlank(query.getPhone()), Employee::getPhone, query.getPhone());
        wrapper.like(StringUtils.isNotBlank(query.getEmail()), Employee::getEmail, query.getEmail());
        wrapper.eq(query.getStatus() != null, Employee::getStatus, query.getStatus());
        wrapper.eq(query.getTenantId() != null, Employee::getTenantId, query.getTenantId());
        wrapper.eq(query.getCreateDept() != null, Employee::getCreateDept, query.getCreateDept());
        wrapper.eq(query.getDelFlag() != null, Employee::getDelFlag, query.getDelFlag());
        return wrapper;
    }
}
