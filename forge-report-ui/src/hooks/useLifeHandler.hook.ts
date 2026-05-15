import { CreateComponentType, CreateComponentGroupType } from '@/packages/index.d'
import { BaseEvent, ComponentActionType, EventLife } from '@/enums/eventEnum'
import { switchPreviewPage } from '@/views/preview/utils/storage'
import * as echarts from 'echarts'

// 所有图表组件集合对象
const components: { [K in string]?: any } = {}

// 项目提供的npm 包变量
export const npmPkgs = { echarts }

// 组件事件处理 hook
export const useLifeHandler = (chartConfig: CreateComponentType | CreateComponentGroupType) => {
  if (!chartConfig.events) return {}

  // 处理基础事件
  const baseEvent: { [key: string]: any } = {}
  for (const key of Object.values(BaseEvent)) {
    const fnStr: string | undefined = (chartConfig.events.baseEvent as any)[key]
    const actions = (chartConfig.events.actions || []).filter(action => action.trigger === key)
    if (fnStr || actions.length) {
      baseEvent[key] = generateBaseFunc(fnStr, actions)
    }
  }

  // 生成生命周期事件
  const events = chartConfig.events.advancedEvents || {}
  const lifeEvents = {
    [EventLife.VNODE_BEFORE_MOUNT](e: any) {
      // 存储组件
      components[chartConfig.id] = e.component
      const fnStr = (events[EventLife.VNODE_BEFORE_MOUNT] || '').trim()
      generateFunc(fnStr, e)
    },
    [EventLife.VNODE_MOUNTED](e: any) {
      const fnStr = (events[EventLife.VNODE_MOUNTED] || '').trim()
      generateFunc(fnStr, e)
    }
  }
  return { ...baseEvent, ...lifeEvents }
}

/**
 * 生成基础函数
 * @param fnStr 用户方法体代码
 * @param event 鼠标事件
 */
 export function generateBaseFunc(fnStr = '', actions: CreateComponentType['events']['actions'] = []) {
  try {
    const customHandler = fnStr
      ? new Function(`
        return (
          async function(components,mouseEvent){
            ${fnStr}
          }
        )`)().bind(undefined, components)
      : undefined

    return async (mouseEvent: MouseEvent) => {
      if (customHandler) {
        await customHandler(mouseEvent)
      }
      for (const action of actions || []) {
        if (action.type === ComponentActionType.GO_PAGE && action.targetPageId) {
          await switchPreviewPage(action.targetPageId, {}, action.transition)
        }
      }
    }
  } catch (error) {
    console.error(error)
  }
}

/**
 * 生成高级函数
 * @param fnStr 用户方法体代码
 * @param e 执行生命周期的动态组件实例
 */
function generateFunc(fnStr: string, e: any) {
  try {
    // npmPkgs 便于拷贝 echarts 示例时设置option 的formatter等相关内容
    Function(`
      "use strict";
      return (
        async function(e, components, node_modules){
          const {${Object.keys(npmPkgs).join()}} = node_modules;
          ${fnStr}
        }
      )`)().bind(e?.component)(e, components, npmPkgs)
  } catch (error) {
    console.error(error)
  }
}
