<template>
  <div v-if="cardData" class="go-items-list-card">
    <n-card hoverable size="small">
      <div class="list-content">
        <!-- 顶部按钮 -->
        <div class="list-content-top">
          <mac-os-control-btn
            class="top-btn"
            :hidden="['remove']"
            @close="deleteHanlde"
            @resize="resizeHandle"
         ></mac-os-control-btn>
        </div>
<!-- 中间 -->
        <div class="list-content-img" @click="resizeHandle">
          <n-image
            object-fit="contain"
            height="180"
            preview-disabled
            :src="imageSrc"
            :alt="cardData.title"
            :fallback-src="requireErrorImg()"
          ></n-image>
        </div>
      </div>
      <template #action>
        <div class="go-flex-items-center list-footer" justify="space-between">
          <n-text class="go-ellipsis-1" :title="cardData.title">
            {{ cardData.title || '' }}
          </n-text>
          <!-- 工具 -->
          <div class="go-flex-items-center list-footer-ri">
            <n-space>
              <n-text>
                <n-badge
                  class="go-animation-twinkle"
                  dot
                  :color="cardData.release ? '#34c749' : '#fcbc40'"
              ></n-badge>
                {{
                  cardData.release
                    ? $t('project.release')
                    : $t('project.unreleased')
                }}
              </n-text>

              <template v-for="item in fnBtnList" :key="item.key">
                <template v-if="item.key === 'select'">
                  <n-dropdown
                    trigger="hover"
                    placement="bottom"
                    :options="selectOptions"
                    :show-arrow="true"
                    @select="handleSelect"
                  >
                    <n-button size="small">
                      <template #icon>
                        <component :is="item.icon"></component>
                      </template>
                    </n-button>
                  </n-dropdown>
                </template>

                <n-tooltip v-else placement="bottom" trigger="hover">
                  <template #trigger>
                    <n-button size="small" @click="handleSelect(item.key)">
                      <template #icon>
                        <component :is="item.icon"></component>
                      </template>
                    </n-button>
                  </template>
                  <component :is="item.label"></component>
                </n-tooltip>
              </template>
            </n-space>
          </div>
          <!-- end -->
        </div>
      </template>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, PropType, watch, onUnmounted } from 'vue'
import { renderIcon, renderLang, requireErrorImg, fetchPathByName, routerTurnByPath, getLocalStorage } from '@/utils'
import { icon } from '@/plugins'
import { MacOsControlBtn } from '@/components/Tips/MacOsControlBtn/index'
import { Chartype } from '../../index'
import { PreviewEnum } from '@/enums/pageEnum'
import { StorageEnum } from '@/enums/storageEnum'
import { publishProjectApi } from '@/api/project'
const {
  EllipsisHorizontalCircleSharpIcon,
  CopyIcon,
  TrashIcon,
  PencilIcon,
  DownloadIcon,
  BrowsersOutlineIcon,
  HammerIcon,
  SendIcon
} = icon.ionicons5

const emit = defineEmits(['delete', 'resize', 'edit', 'refresh'])

const props = defineProps({
  cardData: Object as PropType<Chartype>
})

// 处理url获取
const requireUrl = (name: string) => {
  return new URL(`../../../../../assets/images/${name}`, import.meta.url).href
}

// 图片src（支持认证）
const imageSrc = ref('')
let currentBlobUrl: string | null = null

// 判断是否需要认证的图片URL
const needsAuth = (url: string) => {
  if (!url) return false
  if (url.startsWith('data:') || url.startsWith('blob:')) return false
  if (url.startsWith('http://') || url.startsWith('https://')) return false
  return url.includes('/api/file/')
}

// 加载带认证的图片
const loadAuthImage = async (url: string) => {
  if (currentBlobUrl) {
    URL.revokeObjectURL(currentBlobUrl)
    currentBlobUrl = null
  }

  if (!url) {
    imageSrc.value = requireUrl('project/moke-20211219181327.png')
    return
  }

  if (!needsAuth(url)) {
    imageSrc.value = url
    return
  }

  try {
    const token = getLocalStorage(StorageEnum.GO_ACCESS_TOKEN_STORE)
    const response = await fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : ''
      }
    })

    if (response.ok) {
      const blob = await response.blob()
      currentBlobUrl = URL.createObjectURL(blob)
      imageSrc.value = currentBlobUrl
    } else {
      console.warn('[AuthImage] 图片加载失败', url, response.status)
      imageSrc.value = requireUrl('project/moke-20211219181327.png')
    }
  } catch (error) {
    console.warn('[AuthImage] 图片加载异常', error)
    imageSrc.value = requireUrl('project/moke-20211219181327.png')
  }
}

// 监听 cardData 变化，加载图片
watch(() => props.cardData?.indexImg, (newUrl) => {
  loadAuthImage(newUrl)
}, { immediate: true })

// 组件卸载时清理 blob URL
onUnmounted(() => {
  if (currentBlobUrl) {
    URL.revokeObjectURL(currentBlobUrl)
  }
})

const fnBtnList = reactive([
  {
    label: renderLang('global.r_edit'),
    key: 'edit',
    icon: renderIcon(HammerIcon)
  },
  {
    lable: renderLang('global.r_more'),
    key: 'select',
    icon: renderIcon(EllipsisHorizontalCircleSharpIcon)
  }
])

const selectOptions = ref([
  {
    label: renderLang('global.r_preview'),
    key: 'preview',
    icon: renderIcon(BrowsersOutlineIcon)
  },
  {
    label: props.cardData?.release
      ? '重新发布'
      : renderLang('global.r_publish'),
    key: 'send',
    icon: renderIcon(SendIcon)
  },
  {
    label: renderLang('global.r_delete'),
    key: 'delete',
    icon: renderIcon(TrashIcon)
  }
])

const handleSelect = async (key: string) => {
  switch (key) {
    case 'delete':
      deleteHanlde()
      break
    case 'edit':
      editHandle()
      break
    case 'preview':
      previewHandle()
      break
    case 'send':
      await publishHandle()
      break
  }
}

// 预览处理
const previewHandle = () => {
  const path = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
  if (!path) return
  routerTurnByPath(path, [String(props.cardData?.id)], undefined, true)
}

// 发布处理
const publishHandle = async () => {
  try {
    const path = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
    if (!path || !props.cardData?.id) return
    const previewUrl = `${window.location.origin}${path}/${props.cardData.id}`
    await publishProjectApi(props.cardData.id, previewUrl)
    window.$message.success(props.cardData.release ? '重新发布成功' : '发布成功')
    emit('refresh')
  } catch (error: any) {
    window.$message.error(error?.message || '发布失败')
  }
}

// 删除处理
const deleteHanlde = () => {
  emit('delete', props.cardData)
}

// 编辑处理
const editHandle = () => {
  emit('edit', props.cardData)
}

// 放大处理
const resizeHandle = () => {
  emit('resize', props.cardData)
}
</script>

<style lang="scss" scoped>
$contentHeight: 180px;
@include go('items-list-card') {
  position: relative;
  border-radius: $--border-radius-base;
  border: 1px solid rgba(0, 0, 0, 0);
  @extend .go-transition;
  &:hover {
    @include hover-border-color('hover-border-color');
  }
  .list-content {
    margin-top: 20px;
    margin-bottom: 5px;
    cursor: pointer;
    border-radius: $--border-radius-base;
    @include background-image('background-point');
    @extend .go-point-bg;
    &-top {
      position: absolute;
      top: 10px;
      left: 10px;
      height: 22px;
    }
    &-img {
      height: $contentHeight;
      @extend .go-flex-center;
      @extend .go-border-radius;
      @include deep() {
        img {
          @extend .go-border-radius;
        }
      }
    }
  }
  .list-footer {
    flex-wrap: nowrap;
    justify-content: space-between;
    line-height: 30px;
    &-ri {
      justify-content: flex-end;
      min-width: 180px;
    }
  }
}
</style>
