# Test spec

- 参与者写入：同一用户同一关系重复写入不报错。
- `DynamicDataScopeService`：个人数据 + 经手 ID 生成 OR；写路径/开关关闭/全部数据不 OR。
- 适配保存带上 `flowRelatedVisible`。
- 适配弹窗默认开启开关，取消后随 save 提交 false。
