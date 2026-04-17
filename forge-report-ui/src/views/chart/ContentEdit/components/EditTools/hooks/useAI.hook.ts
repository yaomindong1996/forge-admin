import { ref } from 'vue'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { aiGenerate } from '@/api/ai'
import { applyAIToCanvas } from '@/components/GoAI/aiEngine'
import { AIGenerateResponse } from '@/api/ai/ai.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

/**
 * 编辑器内 AI 生成 Hook
 */
export const useAI = () => {
  const aiStore = useAIStore()
  const chartEditStore = useChartEditStore()
  const showAIDialog = ref(false)

  const openAIDialog = () => {
    showAIDialog.value = true
  }

  const closeAIDialog = () => {
    showAIDialog.value = false
  }

  return {
    showAIDialog,
    openAIDialog,
    closeAIDialog
  }
}
