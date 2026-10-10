import { usePluginDetail } from './usePluginDetail'
import { usePluginList } from './usePluginList'

/** 单层列表/详情编排，不经过多层 props 中转。 */
export function usePluginCenter() {
  return { ...usePluginList(), ...usePluginDetail() }
}
