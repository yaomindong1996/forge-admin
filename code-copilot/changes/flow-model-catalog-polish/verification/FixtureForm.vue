<script setup>
defineExpose({ validate: async () => { throw new Error('业务表单不在本轮隔离验收范围') } })
</script>
<template><p>本轮仅验收模型列表；此业务表单需在真实环境另行验证。</p></template>
