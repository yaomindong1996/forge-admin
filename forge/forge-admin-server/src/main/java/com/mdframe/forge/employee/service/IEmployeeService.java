package com.mdframe.forge.employee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 员工信息表Service接口
 * 
 * @author Forge Generator
 * @date 2026-04-26 19:08:42
 */
public interface IEmployeeService extends IService<Employee> {

    /**
     * 分页查询员工信息表列表
     */
    Page<Employee> selectEmployeePage(PageQuery pageQuery, EmployeeQuery query);

    /**
     * 查询员工信息表列表
     */
    List<Employee> selectEmployeeList(EmployeeQuery query);

    /**
     * 根据ID查询员工信息表详情
     */
    Employee selectEmployeeById(Long id);

    /**
     * 新增员工信息表
     */
    boolean insertEmployee(EmployeeDTO dto);

    /**
     * 修改员工信息表
     */
    boolean updateEmployee(EmployeeDTO dto);

    /**
     * 删除员工信息表
     */
    boolean deleteEmployeeById(Long id);

    /**
     * 批量删除员工信息表
     */
    boolean deleteEmployeeByIds(Long[] ids);
}
