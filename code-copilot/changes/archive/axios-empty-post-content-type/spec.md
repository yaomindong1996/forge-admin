# 空 POST 不带 form-urlencoded
> status: apply
> created: 2026-09-02
> complexity: 🟢简单

## 1. 目标

`request.post(url, null, config)` 不再让 axios 把空 body 当成表单。拦截器把 `data: null` 收成无请求体，并清掉 Content-Type，避免以后 axios/Undertow 版本组合再踩 charset 表单解析坑。

## 2. 范围

只改三个前端的 axios 请求拦截，不改业务调用点。
