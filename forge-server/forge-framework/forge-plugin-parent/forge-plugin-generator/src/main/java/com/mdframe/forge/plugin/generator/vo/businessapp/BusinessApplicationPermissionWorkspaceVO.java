package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用权限工作台目录。
 */
@Data
public class BusinessApplicationPermissionWorkspaceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long applicationId;

    private String applicationCode;

    private String applicationName;

    private Integer publishVersion;

    private List<RoleOption> roles = new ArrayList<>();

    private List<PagePermission> pages = new ArrayList<>();

    private List<ObjectPermission> objects = new ArrayList<>();

    @Data
    public static class RoleOption implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;

        private String roleName;

        private String roleKey;

        private Integer roleType;

        private Integer defaultDataScope;
    }

    @Data
    public static class PagePermission implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String pageId;

        private String pageName;

        private String pageType;

        private Integer sort;

        private String permissionCode;

        private Long resourceId;

        private boolean registered;

        private String pendingLabel;
    }

    @Data
    public static class ObjectPermission implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long objectId;

        private String objectCode;

        private String objectName;

        private String moduleCode;

        private String dataScopeMode;

        private boolean dataScopeReady;

        private Long sharedApplicationCount;

        private DataScopeAdapter dataScopeAdapter;

        private List<ActionPermission> actions = new ArrayList<>();
    }

    @Data
    public static class DataScopeAdapter implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String dataScope;

        private String userField;

        private String orgField;

        private String regionField;

        private Boolean flowRelatedVisible;

        private List<FieldOption> fields = new ArrayList<>();
    }

    @Data
    public static class FieldOption implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String field;

        private String columnName;

        private String label;
    }

    @Data
    public static class ActionPermission implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String actionCode;

        private String actionName;

        private String permissionCode;

        private Long resourceId;

        private boolean registered;
    }
}
