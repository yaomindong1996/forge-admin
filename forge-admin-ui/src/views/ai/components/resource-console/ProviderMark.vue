<template>
  <span class="provider-mark" :style="{ '--provider-mark-color': brand.color }" :title="brand.label">
    <AuthImage
      v-if="provider?.logo && !logoFailed"
      :src="provider.logo"
      :img-style="{ width: '100%', height: '100%', objectFit: 'contain' }"
      @error="logoFailed = true"
    />
    <img
      v-else-if="brand.asset"
      :src="brand.asset"
      :style="brand.imageStyle"
      alt=""
      aria-hidden="true"
    >
    <i v-else :class="brand.icon" aria-hidden="true" />
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import deepseek from '@/assets/ai/providers/deepseek-color.svg'
import hunyuan from '@/assets/ai/providers/hunyuan-color.svg'
import kimi from '@/assets/ai/providers/kimi-color.svg'
import minimax from '@/assets/ai/providers/minimax-color.svg'
import siliconcloud from '@/assets/ai/providers/siliconcloud-color.svg'
import spark from '@/assets/ai/providers/spark-color.svg'
import vllm from '@/assets/ai/providers/vllm-color.svg'
import volcengine from '@/assets/ai/providers/volcengine-color.svg'
import wenxin from '@/assets/ai/providers/wenxin-color.svg'
import xinference from '@/assets/ai/providers/xinference-color.svg'
import zhipu from '@/assets/ai/providers/zhipu-color.svg'
import AuthImage from '@/components/common/AuthImage.vue'

const props = defineProps({ provider: { type: Object, default: null } })
const logoFailed = ref(false)
watch(() => props.provider?.logo, () => {
  logoFailed.value = false
})

const marks = {
  alibaba: { icon: 'i-simple-icons:alibabacloud', color: '#f27921', label: '阿里百炼' },
  openai: { icon: 'i-simple-icons:openai', color: 'var(--text-primary)', label: 'OpenAI' },
  deepseek: { asset: deepseek, label: 'DeepSeek' },
  anthropic: { icon: 'i-simple-icons:anthropic', color: '#bb7148', label: 'Anthropic' },
  azure: { icon: 'i-lucide:cloud', color: '#2486d5', label: 'Azure' },
  zhipu: { asset: zhipu, label: '智谱 AI' },
  moonshot: { asset: kimi, imageStyle: { background: '#25272d', padding: '3px', borderRadius: '4px', boxSizing: 'border-box' }, label: 'Kimi' },
  minimax: { asset: minimax, label: 'MiniMax' },
  volcengine: { asset: volcengine, label: '火山方舟' },
  siliconflow: { asset: siliconcloud, label: '硅基流动' },
  baidu: { asset: wenxin, label: '百度千帆' },
  tencent: { asset: hunyuan, label: '腾讯混元' },
  xunfei: { asset: spark, label: '讯飞星火' },
  xinference: { asset: xinference, label: 'Xinference' },
  vllm: { asset: vllm, label: 'vLLM' },
  localai: { icon: 'i-lucide:server-cog', color: '#259487', label: 'LocalAI' },
  custom: { icon: 'i-lucide:plug', color: 'var(--primary-color)', label: '自定义连接' },
  ollama: { icon: 'i-simple-icons:ollama', color: 'var(--text-primary)', label: 'Ollama' },
  google: { icon: 'i-simple-icons:googlegemini', color: '#4285f4', label: 'Google Gemini' },
}

const aliases = {
  dashscope: 'alibaba',
  kimi: 'moonshot',
  volcanic_engine: 'volcengine',
  silicon_flow: 'siliconflow',
  wenxin: 'baidu',
  hunyuan: 'tencent',
  spark: 'xunfei',
  gemini: 'google',
  claude: 'anthropic',
  local_ai: 'localai',
  chatglm: 'zhipu',
  bigmodel: 'zhipu',
}

function inferType(provider) {
  const raw = `${provider?.providerType || ''} ${provider?.templateKey || ''} ${provider?.name || ''} ${provider?.providerName || ''}`.toLowerCase()
  if (/deepseek/.test(raw))
    return 'deepseek'
  if (/zhipu|智谱|chatglm|bigmodel/.test(raw))
    return 'zhipu'
  if (/alibaba|dashscope|百炼|通义/.test(raw))
    return 'alibaba'
  if (/openai/.test(raw))
    return 'openai'
  if (/anthropic|claude/.test(raw))
    return 'anthropic'
  if (/moonshot|kimi/.test(raw))
    return 'moonshot'
  if (/minimax/.test(raw))
    return 'minimax'
  if (/volc|火山|方舟/.test(raw))
    return 'volcengine'
  if (/silicon|硅基/.test(raw))
    return 'siliconflow'
  if (/baidu|千帆|文心/.test(raw))
    return 'baidu'
  if (/tencent|混元|hunyuan/.test(raw))
    return 'tencent'
  if (/xunfei|讯飞|spark|星火/.test(raw))
    return 'xunfei'
  if (/gemini|google/.test(raw))
    return 'google'
  if (/ollama/.test(raw))
    return 'ollama'
  if (/vllm/.test(raw))
    return 'vllm'
  if (/xinference/.test(raw))
    return 'xinference'
  if (/azure/.test(raw))
    return 'azure'
  if (/localai|local_ai|本地/.test(raw))
    return 'localai'
  return provider?.providerType || 'custom'
}

const brand = computed(() => {
  const type = inferType(props.provider)
  const key = aliases[type] || type
  return marks[key] || { icon: 'i-lucide:boxes', color: 'var(--primary-color)', label: '供应商' }
})
</script>

<style scoped>
.provider-mark {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 6px;
  color: var(--provider-mark-color, var(--primary-color));
  background: color-mix(in srgb, var(--provider-mark-color, var(--primary-color)) 10%, transparent);
}

.provider-mark > i {
  width: 1em;
  height: 1em;
  font-size: 1em;
  line-height: 1;
}

.provider-mark > img,
.provider-mark :deep(.auth-image-host),
.provider-mark :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
