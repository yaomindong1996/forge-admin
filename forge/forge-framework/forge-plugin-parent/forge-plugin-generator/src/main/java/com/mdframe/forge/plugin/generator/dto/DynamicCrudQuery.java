package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DynamicCrudQuery {

    private Map<String, Object> searchParams;
}
