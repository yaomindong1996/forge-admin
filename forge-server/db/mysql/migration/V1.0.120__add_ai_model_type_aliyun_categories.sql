-- 扩展 ai_model_type 字典：按阿里百炼模型广场分类新增 视觉理解 / 视频理解 / 音频理解 / 视频生成
-- 对齐 AiModelType 枚举（VISION / VIDEO_UNDERSTANDING / AUDIO_UNDERSTANDING / VIDEO_GENERATION）与 inferFromModelId 推断结果

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 9 dict_sort, '视觉理解' dict_label, 'vision' dict_value, 'ai_model_type' dict_type, 'primary' list_class, 'N' is_default, '图像/视频多模态理解（qwen-vl 系列）' remark
  UNION ALL SELECT 1, 10, '视频理解', 'video_understanding', 'ai_model_type', 'error', 'N', '视频多模态理解（qwen-vl-video 系列）'
  UNION ALL SELECT 1, 11, '音频理解', 'audio_understanding', 'ai_model_type', 'info', 'N', '音频多模态理解（qwen-audio 系列）'
  UNION ALL SELECT 1, 12, '视频生成', 'video_generation', 'ai_model_type', 'error', 'N', '文生视频/图生视频（wan t2v/i2v 系列）'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);
