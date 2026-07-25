# 墨舟 NovelCraft Android

面向网文作者的本地优先 Android 应用。作品、章节和资料保存到本机 SQLite；模型调用由手机直接访问用户填写的 OpenAI 兼容 Base URL，API Key 不会进入任何业务服务端。

## 已可用功能

- 新建作品，保存书名、题材和一句话设定
- 导入 TXT / Markdown 并自动识别“第 N 章”标题
- 本地 SQLite 持久化项目、章节和资料卡
- 手机章节编辑器，500ms 防抖自动保存
- 人物、伏笔、地点等资料卡
- 章节大纲与基础发布前检查
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

- 导入首版支持 TXT / Markdown；DOCX 解析将在下一阶段接入。
- 当前支持 OpenAI 兼容 Chat Completions 接口。Anthropic 原生 Messages API 会单独适配。
- 没有账号、云同步或服务端存储，符合“SQLite 本地优先”的首版方向。
- AI 返回为一次性结果，不包含流式输出、取消与断点续写。

