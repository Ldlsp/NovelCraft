# 墨舟 NovelCraft Android

面向网文作者的本地优先 Android 应用。作品、章节和资料保存到本机 SQLite；模型调用由手机直接访问用户填写的 OpenAI 兼容 Base URL，API Key 不会进入任何业务服务端。

## 已可用功能

- 新建作品，保存书名、题材和一句话设定
- 导入 TXT / Markdown / DOCX，并自动识别“第 N 章”标题
- 识别“第十二章”和 Markdown 章节标题，减少旧稿导入后的手工整理
- 从创作页一键导出完整作品为 UTF-8 Markdown，便于备份或转到其他编辑器
- 本地 SQLite 持久化项目、章节和资料卡
- 手机章节编辑器，500ms 防抖自动保存
- 横向章节轨道、章节新建和标题自动保存
- 人物、伏笔、地点、时间线、禁区等资料卡，支持活跃、已回收、保密状态
- 每章的冲突/转折/钩子大纲与目标字数，均离线自动保存
- 大纲锚点可限定章节区间、核心冲突、允许推进、禁止揭露与章末张力，并自动注入续写和审核
- 资料卡之间可记录关系，形成可持续维护的本地知识图谱
- 已配置模型时，可基于本地设定和历史章节生成可编辑的本章计划
- 支持将章节计划拆成 4-7 条 Beat Sheet 分镜，续写时强制按场景顺序展开
- 可从当前章节自动抽取资料卡和关系图谱，模型输出经过结构校验后才写入本机数据库
- 项目可维护文风档案，也可从当前样章提取；续写、计划、分镜均自动遵守该档案
- AI 续写和计划生成可随时取消；取消会断开本次请求，正文保持不变
- AI 返回时只会追加到作者当前草稿之后，不会覆盖等待期间继续输入的文字
- 审核页可基于门禁问题生成最短修复计划；正文只由作者确认后手动修改
- 离线本地检索：续写时按当前章节从资料卡和历史章节取回相关上下文
- 可解释的本地质量门：篇幅、占位符、重复段落、保密设定、结尾收束提示
- 用户填写 Base URL、API Key、模型名称
- 直接调用 OpenAI 兼容的 `/models` 和 `/chat/completions`
- API Key 使用 Android Keystore 加密存储

## 本地运行

1. 使用 Android Studio 打开项目根目录。
2. 使用 Android SDK 34、JDK 17。
3. 连接 Android 设备或启动模拟器。
4. 运行 `app` 模块。

命令行构建：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

## 数据与安全

- 小说数据：内部数据库 `novelcraft.db`
- 模型配置：Android Keystore 加密的 SharedPreferences
- Base URL：首版仅允许 HTTPS，避免 API Key 在明文链路上传输
- 离线：可完整浏览和编辑本地作品；AI 续写与连接测试需要网络

## 当前边界

- 当前支持 OpenAI 兼容 Chat Completions 接口。Anthropic 原生 Messages API 会单独适配。
- 没有账号、云同步或服务端存储，符合“SQLite 本地优先”的首版方向。
- AI 返回为一次性结果，不包含流式输出、取消与断点续写；这些是下一阶段的交互重点。
