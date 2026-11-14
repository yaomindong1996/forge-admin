-- 已发布应用页面菜单脱离页面管理运行页：对象页面进入独立 CRUD 地址，
-- 自定义内容页和页面组进入正式应用门户。新发布菜单由
-- BusinessApplicationPageMenuPublishService 直接写入同样的目标；本迁移负责修复
-- V1.0.29 以来已经落库的历史资源，重复执行不会改变结果。

-- 对象型页面可以脱离应用门户，直接进入自己的低代码运行页。
UPDATE sys_resource resource
INNER JOIN ai_business_application application_row
        ON application_row.tenant_id = resource.tenant_id
       AND application_row.del_flag = 0
       AND resource.perms LIKE CONCAT('ai:business:application:', application_row.application_code, ':page:%')
INNER JOIN JSON_TABLE(
        IF(JSON_VALID(application_row.options), application_row.options, '{}'),
        '$.inAppBuilder.nodes[*]' COLUMNS (
            node_id VARCHAR(128) PATH '$.id',
            config_key VARCHAR(128) PATH '$.objectRef.configKey',
            page_key VARCHAR(64) PATH '$.objectRef.pageKey',
            page_mode VARCHAR(64) PATH '$.objectRef.pageMode',
            form_key VARCHAR(128) PATH '$.objectRef.formKey'
        )
     ) page_node
        ON page_node.node_id = SUBSTRING_INDEX(resource.perms, ':page:', -1)
SET resource.path = CONCAT(
        '/ai/crud-page/', page_node.config_key,
        '?pageKey=', COALESCE(NULLIF(page_node.page_key, ''), 'list'),
        '&appId=', application_row.id,
        CASE WHEN NULLIF(page_node.form_key, '') IS NULL THEN ''
             ELSE CONCAT('&formKey=', page_node.form_key) END,
        CASE WHEN LOWER(COALESCE(page_node.page_mode, 'crud')) = 'form'
             THEN '&runtimeOpenMode=CREATE_FORM&mode=create' ELSE '' END
    ),
    resource.component = 'ai/crud-page'
WHERE resource.del_flag = 0
  AND COALESCE(resource.client_code, 'pc') = 'pc'
  AND resource.resource_type = 2
  AND page_node.config_key IS NOT NULL
  AND page_node.config_key <> ''
  AND (
      resource.path LIKE '/app-center/application/%/runtime%'
      OR resource.path LIKE '/app/%?pageId=%'
      OR resource.component = 'app-center/application-runtime.[applicationCode]'
  );

-- 自定义内容页和页面组没有独立 CRUD 路由，保留正式门户地址作为兜底。
UPDATE sys_resource resource
INNER JOIN ai_business_application application_row
        ON application_row.tenant_id = resource.tenant_id
       AND application_row.del_flag = 0
       AND resource.perms LIKE CONCAT('ai:business:application:', application_row.application_code, ':page:%')
SET resource.path = CASE
        WHEN resource.resource_type = 1
          THEN CONCAT('/app/', COALESCE(NULLIF(application_row.portal_slug, ''), application_row.application_code))
        ELSE CONCAT('/app/', COALESCE(NULLIF(application_row.portal_slug, ''), application_row.application_code),
                    '?pageId=', SUBSTRING_INDEX(resource.perms, ':page:', -1))
    END,
    resource.component = CASE
        WHEN resource.resource_type = 2 THEN 'app-center/application-portal'
        ELSE NULL
    END
WHERE resource.del_flag = 0
  AND COALESCE(resource.client_code, 'pc') = 'pc'
  AND resource.resource_type IN (1, 2)
  AND (
      resource.perms LIKE 'ai:business:application:%:page:root'
      OR resource.perms LIKE 'ai:business:application:%:page:%'
  )
  AND (
      resource.path LIKE '/app-center/application/%/runtime%'
      OR resource.component = 'app-center/application-runtime.[applicationCode]'
  );
