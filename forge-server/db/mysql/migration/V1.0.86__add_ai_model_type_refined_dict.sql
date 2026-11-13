-- 扩展 ai_model_type 字典：新增 rerank / image_generation / asr / tts
-- 存量 model_type 宽泛值映射：image → image_generation，audio → asr

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 5 dict_sort, '重排模型' dict_label, 'rerank' dict_value, 'ai_model_type' dict_type, 'success' list_class, 'N' is_default, '用于RAG结果重排' remark
  UNION ALL SELECT 1, 6, '图片生成', 'image_generation', 'ai_model_type', 'warning', 'N', '文生图模型'
  UNION ALL SELECT 1, 7, '语音识别', 'asr', 'ai_model_type', 'info', 'N', '语音转文字'
  UNION ALL SELECT 1, 8, '语音合成', 'tts', 'ai_model_type', 'info', 'N', '文字转语音'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- 存量 model_type 宽泛值映射（幂等：仅更新存在的 image/audio 值）：
-- image → image_generation（文生图），audio → asr（语音识别）。audio 中的 tts 场景由用户在新页面重建为 tts 模型。
UPDATE ai_model SET model_type = 'image_generation' WHERE model_type = 'image';
UPDATE ai_model SET model_type = 'asr' WHERE model_type = 'audio';
