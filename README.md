# 墨舟 NovelCraft Android

[English](README_EN.md) | [Surper Ai](https://surperai.top/) | [发行说明](docs/releases/v0.22.6.md) | [贡献指南](CONTRIBUTING.md)

面向中文网文作者的本地优先 Android 创作工具。小说、章节、大纲和资料卡默认保存在设备本机；作者自行决定何时连接 AI，以及使用哪一家兼容服务。

## 推荐 AI 网关：Surper Ai

NovelCraft 原生支持 OpenAI 兼容接口，推荐使用 [Surper Ai](https://surperai.top/) 为创作工作流接入多模型 AI API。

1. 前往 [surperai.top](https://surperai.top/) 创建并管理你的 API Key。
2. 在应用“我的 -> 文本创作模型”中填写：
   - Base URL：`https://surperai.top/v1`
   - API Key：你的 Surper Ai API Key
   - 模型名称：从 Surper Ai 控制台选择的可用模型
3. 保存并测试连接后，即可用于开书、续写、计划、分镜和审核。

Surper Ai 是可选服务，NovelCraft 不会将作者绑定到单一提供商。可用模型、价格、额度和服务公告以 [Surper Ai 官网](https://surperai.top/) 为准；切勿将 API Key 提交到 GitHub、截图或日志中。

## 为什么是 NovelCraft

- 本地优先：没有账号体系、云端作品库或强制同步，作品数据存放在设备本地 SQLite。
- 作者掌控：AI 生成内容进入可编辑草稿，不替代作者做最终发布决定。
- 长篇连续性：人物、关系、伏笔、锚点、禁区和章节记忆会参与后续写作。
- 直接接入：手机直接调用作者配置的 OpenAI 兼容 Base URL；密钥使用 Android Keystore 加密保存。

## 功能

| 创作 | 连续性与质量 | 文件与项目 |
| --- | --- | --- |
| 从灵感生成开书资料与第一章 | 章节计划、4-7 条场景分镜与结尾钩子 | 导入 TXT、Markdown、DOCX、EPUB、PDF |
| AI 实时生成直接显示在编辑器中 | 人物、地点、事件、伏笔和关系图谱 | 导出 Markdown、DOCX、EPUB、PDF |
| 自动生成章节标题，支持续写与批量写作 | 大纲锚点、禁区、节奏事件和本地检索 | JSON 项目备份与恢复 |
| 文风提取、封面生成和编辑器自动保存 | 篇幅、占位符、重复、保密设定和收束提示检查 | 本地封面、书架和全文搜索 |

## 快速开始

### 使用 APK

从 GitHub Releases 下载与设备匹配的安装包。公开首发前的本地构建为 debug 签名，仅适合测试；正式发行包会使用 GitHub Actions 中配置的发布签名。

### 从源码构建

前置条件：Android SDK 34、JDK 17、Android Studio Hedgehog 或更高版本。

```bash
git clone https://github.com/X-ShuChang/NovelCraft.git
cd NovelCraft
./gradlew testDebugUnitTest assembleDebug
```

生成的 APK：`app/build/outputs/apk/debug/app-debug.apk`

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## AI 配置

在“我的”页面填写文本模型的 Base URL、API Key 与模型名称。应用会通过 `/models` 和 `/chat/completions` 使用 OpenAI 兼容接口；图像封面模型可单独配置。

接入 Surper Ai 时，使用 `https://surperai.top/v1`。NovelCraft 也允许配置其他兼容服务。请只使用你有权使用的模型和内容，并自行确认服务商的条款与数据处理规则。

## 数据与隐私

- 小说正文、设定、章节计划、资料卡和项目备份默认位于本机。
- API Key 通过 Android Keystore 加密的 SharedPreferences 保存。
- 网络只在作者主动使用 AI、联网调研或测试连接时发生。
- 本项目不内置账号登录、作品云同步或应用服务端存储。

没有系统能替代你对敏感作品、密钥与第三方服务的判断。请定期导出项目备份，并在提交 Issue 前删除小说正文、API Key、数据库和日志中的敏感内容。

## 架构

```text
Jetpack Compose UI
        |
   NovelViewModel
        |
NovelRepository + Room / SQLite
        |
作者配置的 OpenAI-compatible API
        |
   Surper Ai 或其他兼容服务
```

核心模块位于 `app/src/main/java/com/mozhou/novelcraft`：

- `MainActivity.kt`：Compose 界面、编辑器和模型配置入口。
- `NovelViewModel.kt`：生成任务、流式正文、章节闭环和交互状态。
- `NovelRepository.kt` / `NovelDatabase.kt`：本地持久化与事务。
- `ModelConfig.kt`：兼容 API 客户端与 Keystore 加密配置。
- `ContextEngine.kt` / `QualityGate.kt`：上下文组织与本地质量门。

## 开源协作

本项目采用 [Apache License 2.0](LICENSE)。贡献代码前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)、[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) 与 [SECURITY.md](SECURITY.md)。

提交 Pull Request 前至少运行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

请勿提交 API Key、签名证书、`local.properties`、`keystore.properties`、用户作品、数据库、导出备份或真实生产日志。

## 发行

`v0.22.6` 是首个 GitHub 公开发行版本。维护者创建 `v0.22.6` 形式的标签后，GitHub Actions 会运行测试、构建 debug APK 并创建 GitHub Release。该发行包用于测试；后续正式签名发行可通过 GitHub Actions Secrets 配置，详细说明见 [发行说明](docs/releases/v0.22.6.md)。

## 路线图

- 更完整的流式生成进度与可恢复写作体验
- 更多兼容模型接口与可配置创作工作流
- 更细粒度的导入分析、版本比较和本地备份策略
- 面向贡献者的 UI 测试与截图回归

## 致谢

NovelCraft 感谢每一位作者和贡献者。使用 [Surper Ai](https://surperai.top/) 可以快速为 NovelCraft 接入兼容的多模型 AI API；欢迎在 Issue 中分享兼容性反馈，但请勿公开凭据或个人作品。
