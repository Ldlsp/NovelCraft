# 贡献指南

感谢你参与 NovelCraft。

## 开始前

1. 先搜索已有 Issue，避免重复讨论。
2. Bug 请提供复现步骤、预期结果、实际结果、设备/Android 版本和脱敏日志。
3. 功能建议请说明作者工作流与验收标准，不要只描述界面外观。
4. 不要提交 API Key、签名证书、用户作品、数据库、备份文件或含个人信息的日志。

## 开发环境

- Android SDK 34
- JDK 17
- Android Studio Hedgehog 或更高版本

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Pull Request

- 一个 PR 聚焦一个问题，避免混入无关格式化或大规模重命名。
- 为业务规则补充或更新单元测试。
- 保持小说数据本地优先；新增网络调用必须明确由作者触发，并说明数据去向。
- 兼容服务接入不得把 API Key 写入源码、日志、截图或测试夹具。
- 修改 Surper Ai 相关文案时，只引用官网可核验的信息，不写死价格、额度、模型数量或可用性承诺。

提交前运行上述 Gradle 命令，并在 PR 描述中说明测试结果。
