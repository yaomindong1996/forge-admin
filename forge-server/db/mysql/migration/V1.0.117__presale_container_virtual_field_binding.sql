-- 修复 V1.0.116 种子的布局容器缺少 virtual fieldBinding：
-- 发布检查对 fieldBinding.mode 缺省的组件默认按业务字段校验，导致 card/subTable 容器
-- 被误报「表单字段未绑定」。前端保存时会 normalize 为 virtual，种子数据需补齐同形态。
-- 按组件位置（V1.0.116 写入顺序：索引 4-7 card、8-10 subTable）条件更新，
-- WHERE 校验组件类型与 mode 缺省，重复执行不命中，幂等安全。

SET @vf := JSON_OBJECT('mode', 'virtual', 'fieldCode', '');

-- 1. 低代码 CRUD 当前配置
UPDATE ai_crud_config
SET options = JSON_SET(options,
      '$.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    page_schema = JSON_SET(page_schema,
      '$.zones[2].props.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(options)
  AND JSON_VALID(page_schema)
  AND JSON_LENGTH(options, '$.formDesignerSchema.components') = 11
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[4].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[5].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[6].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[7].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[8].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[9].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(options, '$.formDesignerSchema.components[10].componentKey')) = 'subTable'
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[4].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[5].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[6].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[7].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[8].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[9].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(options, '$.formDesignerSchema.components[10].fieldBinding.mode') IS NULL;

-- 2. 业务对象设计态配置
UPDATE ai_business_object
SET designer_options = JSON_SET(designer_options,
      '$.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1'
  AND JSON_VALID(designer_options)
  AND JSON_LENGTH(designer_options, '$.formDesignerSchema.components') = 11
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[4].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[5].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[6].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[7].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[8].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[9].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[10].componentKey')) = 'subTable'
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[4].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[5].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[6].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[7].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[8].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[9].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options, '$.formDesignerSchema.components[10].fieldBinding.mode') IS NULL;

-- 3. 业务对象已发布设计版本快照
UPDATE ai_business_object_design_version
SET page_snapshot = JSON_SET(page_snapshot,
      '$.zones[2].props.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    designer_options_snapshot = JSON_SET(designer_options_snapshot,
      '$.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'PS_PRESALE_ORDER'
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(page_snapshot)
  AND JSON_VALID(designer_options_snapshot)
  AND JSON_LENGTH(designer_options_snapshot, '$.formDesignerSchema.components') = 11
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[4].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[7].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[8].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[10].componentKey')) = 'subTable'
  AND JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[4].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(designer_options_snapshot, '$.formDesignerSchema.components[10].fieldBinding.mode') IS NULL;

-- 4. 低代码 CRUD 已发布版本快照
UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
SET version_row.options = JSON_SET(version_row.options,
      '$.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    version_row.page_schema = JSON_SET(version_row.page_schema,
      '$.zones[2].props.formDesignerSchema.components[4].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[5].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[6].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[7].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[8].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[9].fieldBinding', JSON_EXTRACT(@vf, '$'),
      '$.zones[2].props.formDesignerSchema.components[10].fieldBinding', JSON_EXTRACT(@vf, '$')),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.options)
  AND JSON_VALID(version_row.page_schema)
  AND JSON_LENGTH(version_row.options, '$.formDesignerSchema.components') = 11
  AND JSON_UNQUOTE(JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[4].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[7].componentKey')) = 'card'
  AND JSON_UNQUOTE(JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[8].componentKey')) = 'subTable'
  AND JSON_UNQUOTE(JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[10].componentKey')) = 'subTable'
  AND JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[4].fieldBinding.mode') IS NULL
  AND JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components[10].fieldBinding.mode') IS NULL;
