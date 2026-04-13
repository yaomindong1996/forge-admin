<template>
  <n-modal v-model:show="showRef" class="go-ai-dialog" @afterLeave="closeHandle">
    <n-card class="ai-card" :bordered="false">
      <template #header>
        <n-space align="center">
          <n-icon size="22" color="#51d6a9">
            <SparklesIcon />
          </n-icon>
          <n-text class="card-title">{{ mode === 'create' ? $t('ai.create_title') : $t('ai.append_title') }}</n-text>
        </n-space>
      </template>
      <template #header-extra>
        <n-text @click="closeHandle" style="cursor: pointer">
          <n-icon size="20">
            <CloseIcon />
          </n-icon>
        </n-text>
      </template>

      <n-space vertical :size="16">
        <!-- 需求描述 -->
        <n-input
          v-model:value="promptRef"
          type="textarea"
          :placeholder="$t('ai.prompt_placeholder')"
          :rows="4"
          :disabled="aiStore.getGenerating"
        />

        <!-- 风格选择 -->
        <n-space align="center">
          <n-text depth="3">{{ $t('ai.style_label') }}</n-text>
          <n-radio-group v-model:value="styleRef" size="small" :disabled="aiStore.getGenerating">
            <n-radio-button value="dark">{{ $t('ai.style_dark') }}</n-radio-button>
            <n-radio-button value="light">{{ $t('ai.style_light') }}</n-radio-button>
          </n-radio-group>
        </n-space>

        <!-- 操作按钮 -->
        <n-space justify="end">
          <n-button @click="closeHandle">{{ $t('ai.cancel') }}</n-button>
          <n-button
            type="primary"
            :loading="aiStore.getGenerating"
            :disabled="!promptRef.trim()"
            @click="handleGenerate"
          >
            <template #icon>
              <n-icon><SparklesIcon /></n-icon>
            </template>
            {{ aiStore.getGenerating ? $t('ai.generating') : $t('ai.generate') }}
          </n-button>
        </n-space>

        <!-- 生成结果提示 -->
        <n-collapse-transition :show="!!aiStore.getLastResponse && !aiStore.getGenerating">
          <n-space vertical :size="8">
            <n-text depth="3">{{ $t('ai.result_preview') }}</n-text>
            <n-text>
              {{ $t('ai.result_components', { count: aiStore.getLastResponse?.components?.length || 0 }) }}
            </n-text>
            <n-space>
              <n-button size="small" type="primary" @click="handleApply">
                {{ $t('ai.apply') }}
              </n-button>
              <n-button size="small" @click="handleGenerate">
                {{ $t('ai.regenerate') }}
              </n-button>
            </n-space>
          </n-space>
        </n-collapse-transition>
      </n-space>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { aiGenerate } from '@/api/ai'
import { applyAIToCanvas } from './aiEngine'
import { AIGenerateResponse } from '@/api/ai/ai.d'
import { icon } from '@/plugins'

const { SparklesIcon, CloseIcon } = icon.ionicons5

const aiStore = useAIStore()
const chartEditStore = useChartEditStore()

const emit = defineEmits(['close', 'applied'])
const props = defineProps({
  show: Boolean,
  mode: {
    type: String as () => 'create' | 'append',
    default: 'create'
  }
})

const showRef = ref(false)
const promptRef = ref('')
const styleRef = ref<'dark' | 'light'>('dark')

watch(() => props.show, (val) => {
  showRef.value = val
  if (val && aiStore.getLastPrompt) {
    promptRef.value = aiStore.getLastPrompt
  }
})

const closeHandle = () => {
  emit('close', false)
}

// 获取画布尺寸（兼容项目页无编辑器场景）
const getCanvasSize = () => {
  try {
    const config = chartEditStore.getEditCanvasConfig
    if (config?.width && config?.height) {
      return { width: config.width, height: config.height }
    }
  } catch {}
  return { width: 1920, height: 1080 }
}

const handleGenerate = async () => {
  if (!promptRef.value.trim()) return

  aiStore.setGenerating(true)
  aiStore.setLastPrompt(promptRef.value)

  try {
    const { width, height } = getCanvasSize()

    const response = await aiGenerate({
      prompt: promptRef.value,
      style: styleRef.value,
      canvasWidth: width,
      canvasHeight: height
    }) as unknown as AIGenerateResponse

    aiStore.setLastResponse(response)
    aiStore.addHistory(promptRef.value, response)
  } catch (error) {
    console.error('[AI Dialog] 生成失败:', error)
    window['$message'].error('AI 生成失败，请稍后重试')
    aiStore.setLastResponse(null)
  } finally {
    aiStore.setGenerating(false)
  }
}

const handleApply = async () => {
  const response = aiStore.getLastResponse
  if (!response) return

  try {
    // 两种模式都调用 applyAIToCanvas 将组件写入 chartEditStore
    const replaceMode = props.mode === 'create'
    await applyAIToCanvas(response, replaceMode)
    window['$message'].success('AI 大屏生成成功！')
    emit('applied', response)
    closeHandle()
  } catch (error) {
    console.error('[AI Dialog] 应用失败:', error)
    window['$message'].error('应用 AI 生成结果失败')
  }
}
</script>

<style lang="scss" scoped>
$cardWidth: 560px;

@include go('ai-dialog') {
  position: fixed;
  top: 160px;
  left: 50%;
  transform: translateX(-50%);

  .ai-card {
    width: $cardWidth;
  }

  .card-title {
    font-size: 16px;
    font-weight: bold;
  }
}
</style>
