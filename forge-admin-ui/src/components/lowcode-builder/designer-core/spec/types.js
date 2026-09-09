/**
 * @fileoverview 统一组件物料协议 — 类型定义
 * @description ComponentSpec / DataSourceSpec / PropsSchema 的 JSDoc 类型声明。
 *   所有 designer-core 模块共享这些类型，确保 102 个统一组件使用同一套协议。
 */

// ─── 数据源 ───────────────────────────────────────────────

/**
 * 数据源类型枚举
 * @typedef {'static' | 'dict' | 'managed' | 'remote' | 'context' | 'relation' | 'builtin'} DataSourceKind
 */

/**
 * 数据源配置
 * @typedef {Object} DataSourceConfig
 * @property {boolean} enabled - 是否启用数据绑定
 * @property {DataSourceKind} sourceType - 数据源类型
 * @property {string} [api] - 远程接口地址
 * @property {string} [method] - HTTP 方法（get/post）
 * @property {string} [paramsText] - 请求参数 JSON 文本
 * @property {string} [dataPath] - 响应数据路径
 * @property {string} [dictType] - 字典类型编码
 * @property {string} [labelField] - 显示字段映射
 * @property {string} [valueField] - 值字段映射
 * @property {string} [contextPath] - 当前记录字段路径
 */

// ─── 属性 Schema ──────────────────────────────────────────

/**
 * 单个属性的 Schema 定义
 * @typedef {Object} PropSchemaItem
 * @property {string} type - 编辑器类型（string/number/boolean/select/color/json/code/customEditor/section）
 * @property {string} title - 属性显示名称
 * @property {*} [default] - 默认值
 * @property {number} [min] - 数值最小值
 * @property {number} [max] - 数值最大值
 * @property {number} [step] - 数值步进
 * @property {Array<{label: string, value: *}>} [options] - select 类型的选项列表
 * @property {string} [editor] - customEditor 类型的编辑器组件名
 * @property {string} [format] - 格式提示（html/markdown/json）
 * @property {string} [placeholder] - 输入占位提示
 * @property {boolean} [disabled] - 是否禁用编辑
 * @property {string} [description] - 属性说明
 */

/**
 * 组件属性 Schema
 * @typedef {Object} PropsSchema
 * @property {Object<string, PropSchemaItem>} properties - 属性映射表
 * @property {string[]} [required] - 必填属性列表
 */

// ─── 布局 ─────────────────────────────────────────────────

/**
 * 组件布局配置
 * @typedef {Object} LayoutSpec
 * @property {number} defaultSpan - 默认栅格宽度（/24）
 * @property {number} [minSpan] - 最小栅格宽度
 * @property {number} [maxSpan] - 最大栅格宽度
 */

// ─── 打印 ─────────────────────────────────────────────────

/**
 * 组件打印配置
 * @typedef {Object} PrintSpec
 * @property {boolean} hidden - 打印时隐藏（按钮等交互组件默认 true）
 * @property {boolean} breakAvoid - 不跨页截断（卡片/分区默认 true）
 */

// ─── 字段默认值 ───────────────────────────────────────────

/**
 * 字段组件的数据库 / 业务默认值
 * @typedef {Object} FieldDefaults
 * @property {string} fieldType - 字段类型（TEXT/NUMBER/DATE/DICT/SWITCH/...）
 * @property {string} businessFieldType - 业务字段类型
 * @property {string} dataType - 数据库列类型（varchar/int/decimal/date/text/...）
 * @property {string} componentType - 默认渲染组件 key
 * @property {number|null} length - 字段长度
 * @property {number|null} precision - 精度
 * @property {string} queryType - 查询匹配方式（eq/like/in/between/...）
 */

// ─── 容器 ─────────────────────────────────────────────────

/**
 * 容器配置
 * @typedef {Object} ContainerSpec
 * @property {boolean} container - 是否为容器
 * @property {number} [maxDepth] - 最大嵌套深度（容器时有效，默认 4）
 * @property {string[]} [accept] - 可接受的子组件类型列表（空 = 全部接受）
 */

// ─── 组件 Spec ────────────────────────────────────────────

/**
 * 统一组件规格定义（102 个组件的核心协议）
 * @typedef {Object} ComponentSpec
 * @property {string} type - 统一类型名（唯一标识）
 * @property {string[]} [aliases] - 存量兼容别名列表
 * @property {'F' | 'L' | 'F+L'} scope - 可用设计器范围
 * @property {'field' | 'layout' | 'business' | 'page' | 'media' | 'widget'} category - 组件分类
 * @property {string} group - 分组名（输入/选择/业务/布局/数据/操作/页面/媒体/导航/内容/...）
 * @property {string} label - 显示名称
 * @property {string} [desc] - 功能描述
 * @property {LayoutSpec} [layout] - 布局配置
 * @property {boolean} [container] - 是否容器
 * @property {number} [maxDepth] - 最大嵌套深度
 * @property {string[]} [accept] - 容器可接受的子组件类型
 * @property {PropsSchema} [propsSchema] - 属性面板 Schema
 * @property {DataSourceKind[]} [dataSources] - 支持的数据源类型
 * @property {PrintSpec} [print] - 打印配置
 * @property {FieldDefaults} [fieldDefaults] - 字段组件默认值（仅 field 类组件）
 * @property {Object} [meta] - 额外元数据（unique/multiField/requireFields/onlyFor/zones 等）
 */
