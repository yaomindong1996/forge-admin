# 通用工具函数

## 日期格式化

```js
import { formatDateTime, formatDate } from '@/utils/common'

// 日期时间
formatDateTime(new Date())           // '2024-01-15 10:30:00'
formatDateTime(Date.now(), 'YYYY-MM-DD HH:mm')

// 日期
formatDate(new Date())               // '2024-01-15'
formatDate(new Date(), 'YYYY/MM/DD')
```

## 防抖与节流

```js
import { debounce, throttle } from '@/utils/common'

// 防抖：延迟执行，重复调用会重置计时
const handleSearch = debounce((keyword) => {
  console.log('搜索:', keyword)
}, 300)

// 立即执行模式
const handleSave = debounce(() => {
  saveData()
}, 300, true)

// 节流：固定间隔执行
const handleScroll = throttle(() => {
  console.log('滚动中...')
}, 200)
```

## UUID 生成

```js
import { generateUUID } from '@/utils/common'

const id = generateUUID()  // 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'
```

## JSON 解析判断

```js
import { canParseToJson } from '@/utils/common'

canParseToJson('{"name":"test"}')  // true
canParseToJson('invalid json')     // false
```

## 睡眠函数

```js
import { sleep } from '@/utils/common'

async function loadData() {
  loading.value = true
  await sleep(1000)  // 等待 1 秒
  loading.value = false
}
```

## 尺寸监听

```js
import { useResize } from '@/utils/common'

const observer = useResize(element, (rect) => {
  console.log('尺寸变化:', rect.width, rect.height)
})

// 销毁监听
observer.disconnect()
```

## API 参考

| 函数 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| formatDateTime | time?, format? | string | 格式化日期时间 |
| formatDate | date?, format? | string | 格式化日期 |
| debounce | fn, wait, immediate? | Function | 防抖函数 |
| throttle | fn, wait | Function | 节流函数 |
| generateUUID | - | string | 生成 UUID |
| canParseToJson | str | boolean | 判断是否可解析为 JSON |
| sleep | time | Promise | 睡眠函数 |
| useResize | el, cb | ResizeObserver | 尺寸监听 |