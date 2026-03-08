<template>
  <CommonPage :show-header="false">
    <div>
      <n-result
        m-auto
        status="404"
        title="404 您访问的页面不存在"
        description="访问路径无效，可返回首页重新导航"
        size="medium"
      >
        <template #icon>
          <div style="width: 360px">
            <img src="@/assets/images/404.png" style="width: 100%"/>
          </div>
        </template>
        <template #footer>
          <n-button type="primary" ghost @click="toHome">
            返回首页
          </n-button>
        </template>
      </n-result>
    </div>
  </CommonPage>
</template>

<script setup>
const { replace } = useRouter()
const toHome = () =>{
  replace(window.$homePath)
}
</script>
