<template>
  <CommonPage :show-header="false">
    <div class="wh-full flex">
      <n-result
        class="m-auto"
        status="403"
        title="403 禁止访问"
        description="抱歉，您暂无权限访问，请联系管理员开通权限。"
        size="large"
      >
        <template #icon>
          <div style="width: 360px">
            <img src="@/assets/images/403.png" style="width: 100%">
          </div>
        </template>
        <template #footer>
          <n-button v-if="back" type="primary" ghost @click="router.replace(back)">
            返回上一页
          </n-button>
          <n-button type="primary" class="ml-20" @click="toHome">
            返回首页
          </n-button>
        </template>
      </n-result>
    </div>
  </CommonPage>
</template>

<script setup>
const router = useRouter()
const route = useRoute()

const back = history.state.back

if (history.state.from === 'permission-guard') {
  delete history.state.from
}
else if (route.query.path) {
  router.replace(route.query.path)
}
function toHome() {
  replace(window.$homePath)
}
</script>
