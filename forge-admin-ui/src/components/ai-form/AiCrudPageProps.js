/**
 * AiCrudPage 组件 Props 定义
 * @author AI Form Team
 * @version 1.0.0
 */

export const aiCrudPageProps = {
  // ========== 搜索相关 ==========
  /**
   * 搜索表单配置
   * @type {Array<Object>}
   */
  searchSchema: {
    type: Array,
    default: () => []
  },

  /**
   * 是否显示搜索表单
   * @type {Boolean}
   */
  showSearch: {
    type: Boolean,
    default: true
  },

  /**
   * 搜索表单栅格列数
   * @type {Number}
   */
  searchGridCols: {
    type: Number,
    default: 4
  },

  /**
   * 搜索表单标签宽度
   * @type {String|Number}
   */
  searchLabelWidth: {
    type: [String, Number],
    default: 'auto'
  },

  /**
   * 搜索表单是否启用折叠
   * @type {Boolean}
   */
  searchEnableCollapse: {
    type: Boolean,
    default: true
  },

  /**
   * 搜索表单最大显示字段数
   * @type {Number}
   */
  searchMaxVisibleFields: {
    type: Number,
    default: 3
  },

  /**
   * 搜索表单行间距
   * @type {Number}
   */
  searchYGap: {
    type: Number,
    default: 16
  },

  // ========== 表格相关 ==========
  /**
   * 表格列配置
   * @type {Array<Object>}
   */
  columns: {
    type: Array,
    default: () => [],
    required: true
  },

  /**
   * 行键字段名
   * @type {String|Function}
   */
  rowKey: {
    type: [String, Function],
    default: 'id'
  },

  /**
   * 是否隐藏多选
   * @type {Boolean}
   */
  hideSelection: {
    type: Boolean,
    default: false
  },

  /**
   * 是否显示斑马纹
   * @type {Boolean}
   */
  striped: {
    type: Boolean,
    default: false
  },

  /**
   * 是否显示边框
   * @type {Boolean}
   */
  bordered: {
    type: Boolean,
    default: false
  },

  /**
   * 表格尺寸
   * @type {'small'|'medium'|'large'}
   */
  tableSize: {
    type: String,
    default: 'small'
  },

  /**
   * 表格最大高度
   * @type {Number|String}
   */
  maxHeight: {
    type: [Number, String],
    default: undefined
  },

  /**
   * 横向滚动宽度
   * @type {Number}
   */
  scrollX: {
    type: Number,
    default: undefined
  },

  /**
   * 表格其他属性
   * @type {Object}
   */
  tableProps: {
    type: Object,
    default: () => ({})
  },

  // ========== 编辑表单相关 ==========
  /**
   * 编辑表单配置
   * @type {Array<Object>}
   */
  editSchema: {
    type: Array,
    default: () => []
  },

  /**
   * 编辑表单栅格列数
   * @type {Number}
   */
  editGridCols: {
    type: Number,
    default: 1
  },

  /**
   * 编辑表单标签宽度
   * @type {String|Number}
   */
  editLabelWidth: {
    type: [String, Number],
    default: 'auto'
  },

  /**
   * 弹窗宽度
   * @type {String}
   */
  modalWidth: {
    type: String,
    default: '800px'
  },

  /**
   * 是否隐藏弹窗底部按钮
   * @type {Boolean}
   */
  hideModalFooter: {
    type: Boolean,
    default: false
  },

  /**
   * 弹窗类型：'modal' | 'drawer'
   * @type {String}
   */
  modalType: {
    type: String,
    default: 'drawer',
    validator: (value) => ['modal', 'drawer'].includes(value)
  },

  /**
   * 抽屉位置：'left' | 'right' | 'top' | 'bottom'
   * @type {String}
   */
  drawerPlacement: {
    type: String,
    default: 'right',
    validator: (value) => ['left', 'right', 'top', 'bottom'].includes(value)
  },

  // ========== 工具栏相关 ==========
  /**
   * 是否隐藏工具栏
   * @type {Boolean}
   */
  hideToolbar: {
    type: Boolean,
    default: false
  },

  /**
   * 是否隐藏新增按钮
   * @type {Boolean}
   */
  hideAdd: {
    type: Boolean,
    default: false
  },

  /**
   * 新增按钮文本
   * @type {String}
   */
  addButtonText: {
    type: String,
    default: '新增'
  },

  /**
   * 是否隐藏批量删除按钮
   * @type {Boolean}
   */
  hideBatchDelete: {
    type: Boolean,
    default: false
  },

  /**
   * 是否显示导入按钮
   * @type {Boolean}
   */
  showImport: {
    type: Boolean,
    default: false
  },

  /**
   * 是否显示导出按钮
   * @type {Boolean}
   */
  showExport: {
    type: Boolean,
    default: false
  },

  /**
   * 导出按钮文本
   * @type {String}
   */
  exportButtonText: {
    type: String,
    default: '导出'
  },

  // ========== API 相关 ==========
  /**
   * RESTful API 基础路径
   * @type {String}
   * @example '/api/users'
   */
  api: {
    type: String,
    default: ''
  },

  /**
   * 是否使用加密请求
   * @type {Boolean}
   */
  isEncrypt: {
    type: Boolean,
    default: false
  },

  /**
   * 自定义 API 配置
   * @type {Object}
   * @example {
   *   list: 'get@/api/users',
   *   create: 'post@/api/users',
   *   update: 'put@/api/users/:id',
   *   delete: 'delete@/api/users/:id',
   *   detail: 'get@/api/users/:id'
   * }
   */
  apiConfig: {
    type: Object,
    default: () => ({})
  },

  /**
   * 列表请求方法
   * @type {'get'|'post'}
   */
  listMethod: {
    type: String,
    default: 'get'
  },

  /**
   * 列表数据字段名
   * @type {String}
   */
  listDataField: {
    type: String,
    default: 'records'
  },

  /**
   * 总数字段名
   * @type {String}
   */
  listTotalField: {
    type: String,
    default: 'total'
  },

  /**
   * 导入 API
   * @type {String}
   */
  importApi: {
    type: String,
    default: ''
  },

  /**
   * 导入请求头
   * @type {Object}
   */
  importHeaders: {
    type: Object,
    default: () => ({})
  },

  /**
   * 导入额外数据
   * @type {Object}
   */
  importData: {
    type: Object,
    default: () => ({})
  },

  /**
   * 导入模板下载地址
   * @type {String}
   */
  importTemplateUrl: {
    type: String,
    default: ''
  },

  /**
   * 导出 API
   * @type {String}
   */
  exportApi: {
    type: String,
    default: ''
  },

  /**
   * 导出文件名
   * @type {String}
   */
  exportFileName: {
    type: String,
    default: ''
  },

  // ========== 分页相关 ==========
  /**
   * 是否显示分页
   * @type {Boolean}
   */
  showPagination: {
    type: Boolean,
    default: true
  },

  /**
   * 初始页码
   * @type {Number}
   */
  pageNum: {
    type: Number,
    default: 1
  },

  /**
   * 每页条数
   * @type {Number}
   */
  pageSize: {
    type: Number,
    default: 10
  },

  /**
   * 每页条数选项
   * @type {Array<Number>}
   */
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  },

  // ========== 钩子函数 ==========
  /**
   * 列表数据加载前钩子
   * @type {Function}
   * @param {Object} params - 请求参数
   * @returns {Object|Promise<Object>} 处理后的参数
   */
  beforeLoadList: {
    type: Function,
    default: null
  },

  /**
   * 列表数据渲染前钩子
   * @type {Function}
   * @param {Array} list - 列表数据
   * @returns {Array} 处理后的列表
   */
  beforeRenderList: {
    type: Function,
    default: null
  },

  /**
   * 表单提交前钩子
   * @type {Function}
   * @param {Object} formData - 表单数据
   * @returns {Object|Promise<Object>|false} 处理后的数据，返回 false 则中断提交
   */
  beforeSubmit: {
    type: Function,
    default: null
  },

  /**
   * 删除前钩子
   * @type {Function}
   * @param {Array} rows - 待删除的行数据
   * @returns {Boolean|Promise<Boolean>} 返回 false 则中断删除
   */
  beforeDelete: {
    type: Function,
    default: null
  },

  /**
   * 搜索重置前钩子
   * @type {Function}
   * @returns {void|Promise<void>}
   */
  beforeRenderReset: {
    type: Function,
    default: null
  },

  /**
   * 搜索前钩子
   * @type {Function}
   * @param {Object} params - 搜索参数
   * @returns {Object|Promise<Object>|false} 处理后的参数，返回 false 则中断搜索
   */
  beforeSearch: {
    type: Function,
    default: null
  },

  /**
   * 详情数据渲染前钩子
   * @type {Function}
   * @param {Object} data - 详情数据
   * @returns {Object} 处理后的数据
   */
  beforeRenderDetail: {
    type: Function,
    default: null
  },

  /**
   * 表单渲染前钩子
   * @type {Function}
   * @param {Object} data - 表单数据
   * @returns {Object} 处理后的数据
   */
  beforeRenderForm: {
    type: Function,
    default: null
  },

  // ========== 其他配置 ==========
  /**
   * 是否懒加载（初始不加载数据）
   * @type {Boolean}
   */
  lazy: {
    type: Boolean,
    default: false
  },

  /**
   * 是否通过主键查询详情
   * 为 true 时编辑会调用详情接口，为 false 时直接使用列表数据
   * @type {Boolean}
   */
  loadDetailOnEdit: {
    type: Boolean,
    default: false
  },

  /**
   * 公共查询参数（会拼接到 URL）
   * @type {Object}
   */
  publicQuery: {
    type: Object,
    default: () => ({})
  },

  /**
   * 公共请求参数（会放到 body）
   * @type {Object}
   */
  publicParams: {
    type: Object,
    default: () => ({})
  }
}
