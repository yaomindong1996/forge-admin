package com.mdframe.forge.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.employee.service.IEmployeeService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工信息表Controller
 *
 * @author Forge Generator
 * @date 2026-04-26 19:08:42
 */
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class EmployeeController {

    private final IEmployeeService employeeService;

    /**
     * 分页查询员工信息表列表
     */
    @GetMapping("/page")
    @OperationLog(module = "员工信息表管理", type = OperationType.QUERY, desc = "分页查询员工信息表列表")
    public RespInfo<Page<Employee>> page(PageQuery pageQuery, EmployeeQuery query) {
        Page<Employee> page = employeeService.selectEmployeePage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询员工信息表列表
     */
    @GetMapping("/list")
    @OperationLog(module = "员工信息表管理", type = OperationType.QUERY, desc = "查询员工信息表列表")
    public RespInfo<List<Employee>> list(EmployeeQuery query) {
        List<Employee> list = employeeService.selectEmployeeList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询员工信息表详情
     */
    @PostMapping("/getById")
    public RespInfo<Employee> getById(@RequestParam Long id) {
        Employee employee = employeeService.selectEmployeeById(id);
        return RespInfo.success(employee);
    }

    /**
     * 新增员工信息表
     */
    @PostMapping("/add")
    @OperationLog(module = "员工信息表管理", type = OperationType.ADD, desc = "新增员工信息表")
    public RespInfo<Void> add(@RequestBody EmployeeDTO dto) {
        boolean result = employeeService.insertEmployee(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改员工信息表
     */
    @PostMapping("/edit")
    @OperationLog(module = "员工信息表管理", type = OperationType.UPDATE, desc = "修改员工信息表")
    public RespInfo<Void> edit(@RequestBody EmployeeDTO dto) {
        boolean result = employeeService.updateEmployee(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除员工信息表
     */
    @PostMapping("/remove/{id}")
    @OperationLog(module = "员工信息表管理", type = OperationType.DELETE, desc = "删除员工信息表")
    public RespInfo<Void> remove(@PathVariable Long id) {
        boolean result = employeeService.deleteEmployeeById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除员工信息表
     */
    @PostMapping("/removeBatch")
    @OperationLog(module = "员工信息表管理", type = OperationType.DELETE, desc = "批量删除员工信息表")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = employeeService.deleteEmployeeByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
