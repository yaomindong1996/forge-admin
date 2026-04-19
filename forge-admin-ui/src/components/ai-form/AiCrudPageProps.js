/**
 * AiCrudPage 组件 Props 定义
 * @author AI Form Team
 * @version 1.0.0
 */

export const aiCrudPageProps = {
  // ========== 搜索相关 ==========
  /**
   * 搜索表单配置
   * @type {Array<object>}
   */
  searchSchema: {
    type: Array,
    default: () => [],
  },

  /**
   * 是否显示搜索表单
   * @type {boolean}
   */
  showSearch: {
    type: Boolean,
    default: true,
  },

  /**
   * 搜索表单栅格列数
   * @type {number}
   */
  searchGridCols: {
    type: Number,
    default: 4,
  },

  /**
   * 搜索表单标签宽度
   * @type {string | number}
   */
  searchLabelWidth: {
    type: [String, Number],
    default: 'auto',
  },

  /**
   * 搜索表单是否启用折叠
   * @type {boolean}
   */
  searchEnableCollapse: {
    type: Boolean,
    default: true,
  },

  /**
   * 搜索表单最大显示字段数
   * @type {number}
   */
  searchMaxVisibleFields: {
    type: Number,
    default: 3,
  },

  /**
   * 搜索表单行间距
   * @type {number}
   */
  searchYGap: {
    type: Number,
    default: 16,
  },

  // ========== 表格相关 ==========
  /**
   * 表格列配置
   * @type {Array<object>}
   */
  columns: {
    type: Array,
    default: () => [],
    required: true,
  },

  /**
   * 行键字段名
   * @type {string | Function}
   */
  rowKey: {
    type: [String, Function],
    default: 'id',
  },

  /**
   * 是否隐藏多选
   * @type {boolean}
   */
  hideSelection: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示斑马纹
   * @type {boolean}
   */
  striped: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示边框
   * @type {boolean}
   */
  bordered: {
    type: Boolean,
    default: false,
  },

  /**
   * 表格尺寸
   * @type {'small'|'medium'|'large'}
   */
  tableSize: {
    type: String,
    default: 'small',
  },

  /**
   * 表格最大高度
   * @type {number | string}
   */
  maxHeight: {
    type: [Number, String],
    default: undefined,
  },

  /**
   * 横向滚动宽度
   * @type {number}
   */
  scrollX: {
    type: Number,
    default: undefined,
  },

  /**
   * 表格其他属性
   * @type {object}
   */
  tableProps: {
    type: Object,
    default: () => ({}),
  },

  // ========== 编辑表单相关 ==========
  /**
   * 编辑表单配置
   * @type {Array<object>}
   */
  editSchema: {
    type: Array,
    default: () => [],
  },

  /**
   * 编辑表单栅格列数
   * @type {number}
   */
  editGridCols: {
    type: Number,
    default: 1,
  },

  /**
   * 编辑表单标签宽度
   * @type {string | number}
   */
  editLabelWidth: {
    type: [String, Number],
    default: 'auto',
  },

  /**
   * 弹窗宽度
   * @type {string}
   */
  modalWidth: {
    type: String,
    default: '800px',
  },

  /**
   * 是否隐藏弹窗底部按钮
   * @type {boolean}
   */
  hideModalFooter: {
    type: Boolean,
    default: false,
  },

  /**
   * 弹窗类型：'modal' | 'drawer'
   * @type {string}
   */
  modalType: {
    type: String,
    default: 'drawer',
    validator: value => ['modal', 'drawer'].includes(value),
  },

  /**
   * 抽屉位置：'left' | 'right' | 'top' | 'bottom'
   * @type {string}
   */
  drawerPlacement: {
    type: String,
    default: 'right',
    validator: value => ['left', 'right', 'top', 'bottom'].includes(value),
  },

  // ========== 工具栏相关 ==========
  /**
   * 是否隐藏工具栏
   * @type {boolean}
   */
  hideToolbar: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否隐藏新增按钮
   * @type {boolean}
   */
  hideAdd: {
    type: Boolean,
    default: false,
  },

  /**
   * 新增按钮文本
   * @type {string}
   */
  addButtonText: {
    type: String,
    default: '新增',
  },

  /**
   * 是否隐藏批量删除按钮
   * @type {boolean}
   */
  hideBatchDelete: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示导入按钮
   * @type {boolean}
   */
  showImport: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示导出按钮
   * @type {boolean}
   */
  showExport: {
    type: Boolean,
    default: false,
  },

  /**
   * 导出按钮文本
   * @type {string}
   */
  exportButtonText: {
    type: String,
    default: '导出',
  },

  // ========== API 相关 ==========
  /**
   * RESTful API 基础路径
   * @type {string}
   * @example '/api/users'
   */
  api: {
    type: String,
    default: '',
  },

  /**
   * 是否使用加密请求
   * @type {boolean}
   */
  isEncrypt: {
    type: Boolean,
    default: false,
  },

  /**
   * 自定义 API 配置
   * @type {object}
   * @example {
   *   list: 'get@/api/users',
   *   create: 'post@/api/users',
   *   update: 'put@/api/users/:id',
   *   delete: 'delete@/api/users/:id',
   *   detail: 'get@/api/users/:id'
   * }
   * @description 支持 URL 占位符替换，如 :id、:dictId 等，会自动替换为对应的主键值。
   *              占位符名称应与 rowKey 属性一致，如 rowKey="dictId" 则使用 :dictId
   */
  apiConfig: {
    type: Object,
    default: () => ({}),
  },

  /**
   * 列表请求方法
   * @type {'get'|'post'}
   */
  listMethod: {
    type: String,
    default: 'get',
  },

  /**
   * 列表数据字段名
   * @type {string}
   */
  listDataField: {
    type: String,
    default: 'records',
  },

  /**
   * 总数字段名
   * @type {string}
   */
  listTotalField: {
    type: String,
    default: 'total',
  },

  /**
   * 导入 API
   * @type {string}
   */
  importApi: {
    type: String,
    default: '',
  },

  /**
   * 导入请求头
   * @type {object}
   */
  importHeaders: {
    type: Object,
    default: () => ({}),
  },

  /**
   * 导入额外数据
   * @type {object}
   */
  importData: {
    type: Object,
    default: () => ({}),
  },

  /**
   * 导入模板下载地址
   * @type {string}
   */
  importTemplateUrl: {
    type: String,
    default: '',
  },

  /**
   * 导出 API
   * @type {string}
   */
  exportApi: {
    type: String,
    default: '',
  },

  /**
   * 导出文件名
   * @type {string}
   */
  exportFileName: {
    type: String,
    default: '',
  },

  // ========== 分页相关 ==========
  /**
   * 是否显示分页
   * @type {boolean}
   */
  showPagination: {
    type: Boolean,
    default: true,
  },

  /**
   * 初始页码
   * @type {number}
   */
  pageNum: {
    type: Number,
    default: 1,
  },

  /**
   * 每页条数
   * @type {number}
   */
  pageSize: {
    type: Number,
    default: 10,
  },

  /**
   * 每页条数选项
   * @type {Array<number>}
   */
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100],
  },

  // ========== 钩子函数 ==========
  /**
   * 列表数据加载前钩子
   * @type {Function}
   * @param {object} params - 请求参数
   * @returns {object | Promise<object>} 处理后的参数
   */
  beforeLoadList: {
    type: Function,
    default: null,
  },

  /**
   * 列表数据渲染前钩子
   * @type {Function}
   * @param {Array} list - 列表数据
   * @returns {Array} 处理后的列表
   */
  beforeRenderList: {
    type: Function,
    default: null,
  },

  /**
   * 表单提交前钩子
   * @type {Function}
   * @param {object} formData - 表单数据
   * @returns {object | Promise<object> | false} 处理后的数据，返回 false 则中断提交
   */
  beforeSubmit: {
    type: Function,
    default: null,
  },

  /**
   * 删除前钩子
   * @type {Function}
   * @param {Array} rows - 待删除的行数据
   * @returns {boolean | Promise<boolean>} 返回 false 则中断删除
   */
  beforeDelete: {
    type: Function,
    default: null,
  },

  /**
   * 搜索重置前钩子
   * @type {Function}
   * @returns {void|Promise<void>}
   */
  beforeRenderReset: {
    type: Function,
    default: null,
  },

  /**
   * 搜索前钩子
   * @type {Function}
   * @param {object} params - 搜索参数
   * @returns {object | Promise<object> | false} 处理后的参数，返回 false 则中断搜索
   */
  beforeSearch: {
    type: Function,
    default: null,
  },

  /**
   * 详情数据渲染前钩子
   * @type {Function}
   * @param {object} data - 详情数据
   * @returns {object} 处理后的数据
   */
  beforeRenderDetail: {
    type: Function,
    default: null,
  },

  /**
   * 表单渲染前钩子
   * @type {Function}
   * @param {object} data - 表单数据
   * @returns {object} 处理后的数据
   */
  beforeRenderForm: {
    type: Function,
    default: null,
  },

  // ========== 其他配置 ==========
  /**
   * 是否懒加载（初始不加载数据）
   * @type {boolean}
   */
  lazy: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否通过主键查询详情
   * 为 true 时编辑会调用详情接口，为 false 时直接使用列表数据
   * @type {boolean}
   */
  loadDetailOnEdit: {
    type: Boolean,
    default: false,
  },

  /**
   * 公共查询参数（会拼接到 URL）
   * @type {object}
   */
  publicQuery: {
    type: Object,
    default: () => ({}),
  },

  /**
   * 公共请求参数（会放到 body）
   * @type {object}
   */
  publicParams: {
    type: Object,
    default: () => ({}),
  },
}
