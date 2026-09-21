package com.mdframe.forge.plugin.generator.vo.audit;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataAuditFilterOptionsVO {

    private List<ApplicationOption> applications = new ArrayList<>();

    @Data
    public static class ApplicationOption {

        private String applicationId;

        private String applicationName;

        private String applicationCode;

        private List<PageOption> pages = new ArrayList<>();
    }

    @Data
    public static class PageOption {

        private String pageId;

        private String pageName;

        private String objectId;

        private String objectName;

        private List<FieldOption> fields = new ArrayList<>();
    }

    @Data
    public static class FieldOption {

        private String fieldCode;

        private String fieldLabel;

        private String columnName;
    }
}
