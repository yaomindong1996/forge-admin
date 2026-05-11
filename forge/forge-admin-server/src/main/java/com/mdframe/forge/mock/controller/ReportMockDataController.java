package com.mdframe.forge.mock.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大屏数据渲染测试 Mock 接口。
 */
@RestController
@RequestMapping("/mock")
@ApiDecrypt
@ApiEncrypt
public class ReportMockDataController {

    /**
     * 财务月度营收支出数据。
     */
    @GetMapping("/finance/monthly-revenue-expense")
    public RespInfo<Map<String, Object>> monthlyRevenueExpense() {
        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("dimensions", Arrays.asList("月份", "营收(万元)", "支出(万元)"));
        dataset.put("source", buildFinanceRows());
        return RespInfo.success(dataset);
    }

    private List<Map<String, Object>> buildFinanceRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildFinanceRow("1月", 820, 580));
        rows.add(buildFinanceRow("2月", 930, 610));
        rows.add(buildFinanceRow("3月", 1020, 650));
        rows.add(buildFinanceRow("4月", 1150, 690));
        rows.add(buildFinanceRow("5月", 1280, 720));
        rows.add(buildFinanceRow("6月", 200, 750));
        return rows;
    }

    private Map<String, Object> buildFinanceRow(String month, Integer revenue, Integer expense) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("月份", month);
        row.put("营收(万元)", revenue);
        row.put("支出(万元)", expense);
        return row;
    }
}
