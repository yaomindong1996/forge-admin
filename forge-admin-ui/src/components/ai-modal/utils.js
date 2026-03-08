/**
 * AI模态框工具函数
 * 
 * 提供模态框拖拽功能的实现
 */

/**
 * 获取元素的CSS样式值
 * 
 * @param {HTMLElement} element - 目标DOM元素
 * @param {String} key - CSS属性名（如'left'、'top'等）
 * @returns {String} CSS属性值
 * 
 * 说明：
 * - 兼容IE浏览器（使用currentStyle）和现代浏览器（使用getComputedStyle）
 * - currentStyle是IE浏览器的私有属性
 * - getComputedStyle是W3C标准方法
 */
function getCss(element, key) {
  return element.currentStyle
    ? element.currentStyle[key]
    : window.getComputedStyle(element, null)[key]
}

/**
 * 初始化模态框拖拽功能
 * 
 * @param {HTMLElement} bar - 拖拽触发元素（通常是模态框的头部）
 * @param {HTMLElement} box - 被拖拽的元素（通常是整个模态框容器）
 * 
 * 功能说明：
 * 1. 通过鼠标在bar元素上按下、移动、释放来拖拽box元素
 * 2. 计算鼠标移动距离，更新box元素的left和top值
 * 3. 支持多个模态框实例的拖拽
 * 
 * 使用场景：
 * - 在模态框打开时调用，使模态框可拖拽移动
 * - 通常在组件的open方法中调用
 * 
 * @example
 * initDrag(
 *   document.querySelector('.modal-header'),
 *   document.querySelector('.modal-box')
 * )
 */
export function initDrag(bar, box) {
  // 安全检查：如果元素不存在则直接返回
  if (!bar || !box)
    return
  
  /**
   * 拖拽参数对象
   * 
   * @property {Number} left - box元素的初始left值
   * @property {Number} top - box元素的初始top值
   * @property {Number} currentX - 鼠标按下时的X坐标
   * @property {Number} currentY - 鼠标按下时的Y坐标
   * @property {Boolean} flag - 拖拽状态标志（true表示正在拖拽）
   */
  const params = {
    left: 0,
    top: 0,
    currentX: 0,
    currentY: 0,
    flag: false,
  }

  // 初始化box元素的left位置
  if (getCss(box, 'left') !== 'auto') {
    params.left = getCss(box, 'left')
  }
  
  // 初始化box元素的top位置
  if (getCss(box, 'top') !== 'auto') {
    params.top = getCss(box, 'top')
  }

  // 设置触发拖动元素的鼠标样式为移动图标
  bar.style.cursor = 'move'
  
  /**
   * 鼠标按下事件处理函数
   * 
   * 功能：
   * 1. 开启拖拽状态
   * 2. 阻止默认事件（防止文本选中等）
   * 3. 记录鼠标当前位置
   */
  bar.onmousedown = function (e) {
    params.flag = true // 设置拖拽标志为true
    e.preventDefault() // 阻止默认事件（如文本选中）
    params.currentX = e.clientX // 记录鼠标当前位置的X坐标
    params.currentY = e.clientY // 记录鼠标当前位置的Y坐标
  }
  
  /**
   * 鼠标释放事件处理函数（全局）
   * 
   * 功能：
   * 1. 关闭拖拽状态
   * 2. 更新box元素的位置参数（为下次拖拽做准备）
   * 
   * 说明：绑定在document上，确保鼠标在任何位置释放都能触发
   */
  document.onmouseup = function () {
    params.flag = false // 设置拖拽标志为false，结束拖拽
    
    // 更新位置参数为当前位置，作为下次拖拽的起始位置
    if (getCss(box, 'left') !== 'auto') {
      params.left = getCss(box, 'left')
    }
    if (getCss(box, 'top') !== 'auto') {
      params.top = getCss(box, 'top')
    }
  }
  
  /**
   * 鼠标移动事件处理函数（全局）
   * 
   * 功能：
   * 1. 检查是否处于拖拽状态
   * 2. 计算鼠标移动距离
   * 3. 更新box元素的位置
   * 
   * 说明：绑定在document上，确保鼠标移出bar元素时仍能继续拖拽
   */
  document.onmousemove = function (e) {
    // 如果鼠标不在bar元素上且未处于拖拽状态，则不处理
    if (e.target !== bar && !params.flag)
      return

    e.preventDefault() // 阻止默认事件
    
    // 如果拖拽标志为true，执行拖拽逻辑
    if (params.flag) {
      const nowX = e.clientX // 获取鼠标当前位置的X坐标
      const nowY = e.clientY // 获取鼠标当前位置的Y坐标
      const disX = nowX - params.currentX // 计算鼠标X方向移动的距离
      const disY = nowY - params.currentY // 计算鼠标Y方向移动的距离

      // 计算box元素的新位置
      const left = Number.parseInt(params.left) + disX // 计算新的left值
      const top = Number.parseInt(params.top) + disY // 计算新的top值

      // 更新box元素的位置
      box.style.left = `${left}px`
      box.style.top = `${top}px`
    }
  }
}
