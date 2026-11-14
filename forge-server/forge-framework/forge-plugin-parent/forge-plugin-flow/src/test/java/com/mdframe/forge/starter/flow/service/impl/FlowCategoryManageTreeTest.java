package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowCategory;
import com.mdframe.forge.starter.flow.mapper.FlowCategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FlowCategoryServiceImpl.listManageTree")
class FlowCategoryManageTreeTest {

    @Test
    @DisplayName("management tree keeps disabled categories so they can be re-enabled")
    void manageTreeIncludesDisabledCategories() {
        FlowCategoryMapper mapper = mock(FlowCategoryMapper.class);
        FlowCategoryServiceImpl service = new FlowCategoryServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectManageList()).thenReturn(List.of(
                category("p", null, 0), category("c", "p", 1), category("q", null, 1)));

        List<FlowCategory> tree = service.listManageTree();

        assertThat(tree).extracting(FlowCategory::getId).containsExactly("p", "q");
        assertThat(tree.get(0).getStatus()).isZero();
        assertThat(tree.get(0).getChildren()).extracting(FlowCategory::getId).containsExactly("c");
    }

    @Test
    @DisplayName("management query must not filter by status")
    void manageQueryHasNoStatusFilter() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/FlowCategoryMapper.xml"));
        String select = xml.substring(xml.indexOf("id=\"selectManageList\""));
        assertThat(select).contains("FROM sys_flow_category").doesNotContain("WHERE").doesNotContain("${");
    }

    private FlowCategory category(String id, String parentId, int status) {
        FlowCategory category = new FlowCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setStatus(status);
        return category;
    }
}
