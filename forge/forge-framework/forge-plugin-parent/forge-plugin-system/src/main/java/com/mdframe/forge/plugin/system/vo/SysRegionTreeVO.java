package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 行政区划树形VO
 */
@Data
public class SysRegionTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行政区划代码
     */
    private String code;

    /**
     * 行政区划名称
     */
    private String name;

    /**
     * 行政级别(1-省,2-市,3-区/县,4-街道)
     */
    private Integer level;

    /**
     * 父级代码
     */
    private String parentCode;

    /**
     * 全名
     */
    private String fullName;

    /**
     * 地市编码
     */
    private String cityCode;

    /**
     * 子节点列表
     */
    private List<SysRegionTreeVO> children;
}