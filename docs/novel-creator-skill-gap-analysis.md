# 墨舟与 novel-creator-skill 功能差距分析

> 对比时间：2026-07-26。基线为本地已检出的上游源码，而不是 GitHub 页面摘要；当前实现以 Android 工作区源码为准。上游有少量“规划中”能力，本文不会将其当作已交付能力。

## 结论

墨舟已经具备可离线管理作品、导入、编辑、续写、批量生成、资料/关系维护、封面和正文导出的移动端基础能力。但在长篇网文最关键的“自动化质量闭环”上，仍明显弱于上游：生成后的记忆、检索、风格、校稿、门禁没有串成强制流水线，且缺少长程节奏控制与自动恢复调度。

最高优先级应是：先把**章节门禁闭环**和**自动记忆/RAG 更新**做成每次 AI 写作的可靠后置步骤，再扩展自动写书、节奏与审稿。

## 已达到或领先

| 能力 | 结论 | 当前源码依据 | 上游依据 |
| --- | --- | --- | --- |
| 本地作品与章节管理 | 已达到。SQLite/Room 持久化，支持建书、编辑、添加/删除章节、删除作品。 | `app/src/main/java/com/mozhou/novelcraft/NovelDatabase.kt`、`NovelRepository.kt:17-153`、`NovelViewModel.kt:126-237` | `README.md:464-474` |
| 导入已有正文并续写 | 已达到。支持纯文本和 DOCX 导入、章节识别、在当前章末尾续写。 | `DocumentTextExtractor.kt`、`ChapterImporter.kt`、`NovelViewModel.kt:138-153,629-657` | `README.md:464-470` |
| 正文导出 | 墨舟领先。可把作品及章节导出为 Markdown 文档；上游可验证的导出仅是知识图谱 Mermaid。 | `NovelViewModel.kt:155-183` | `scripts/story_graph_builder.py:470-533` |
| 章节大纲、分镜和批量写作 | 基础能力已达到。可生成/保存章节计划、Beat Sheet，并批量写 1-5 章。 | `NovelViewModel.kt:237-254,538-584,659-702` | `README.md:464-474`、`references/beat-pipeline-spec.md` |
| 资料与关系图谱的本地模型 | 基础能力已达到。人物、地点、伏笔等资料卡与关系边存入 SQLite，支持 AI 从当前章提取。 | `NovelDatabase.kt:76-124`、`NovelViewModel.kt:388-508`、`MainActivity.kt:1165-1297` | `scripts/story_graph_builder.py:470-533`、`scripts/story_graph_updater.py:201-444` |
| 改纲影响标记 | 部分领先于普通写作 App：可从指定章节标记相关锚点、资料、关系，并在未确认时阻止 AI 继续写作。 | `OutlineCascade.kt`、`NovelViewModel.kt:704-723` | `README.md:473-474`、`SKILL.md:221-234` |
| 模型接入与封面 | 墨舟独有的移动端能力：用户自配 OpenAI 兼容文本/图像模型，独立测试图像模型，生成或上传封面。 | `ModelConfig.kt:57-201`、`NovelViewModel.kt:280-348` | 上游侧重点是命令行写作流；模型配置见 `README.md:563-608` |

## 部分达到，需补强

| 能力 | 差距 | 当前源码依据 | 上游依据 |
| --- | --- | --- | --- |
| 新书建模与开书确认 | 当前只要求书名，资料页可补题材/设定/读者等；没有开书向导来确认目标读者、文风、禁区、自动化等级、目标规模，也没有百万字路线图。 | `NovelViewModel.kt:126-136,264-279` | `README.md:408-423`、`SKILL.md:50-58` |
| 剧情检索/RAG | 当前是可解释的关键词检索：资料卡 + 最多两章片段，能改善上下文，但没有索引、实体-章节映射、查询缓存、增量重建或检索报告。 | `ContextEngine.kt:14-91` | `references/rag-consistency-design.md`、`scripts/plot_rag_retriever.py` |
| 长期记忆/知识图谱 | 有图谱数据模型和手动“从本章提取”，但生成后不会自动抽取、更新角色状态/事件/伏笔/时间线，也没有图谱校验/差异或可视化导出。 | `NovelViewModel.kt:444-508`、`NovelDatabase.kt:76-124` | `scripts/story_graph_updater.py:201-444`、`scripts/story_graph_builder.py:470-533` |
| 改纲续写 | 有影响范围标记和人工确认；缺少锚点重算备份、RAG 重建、图谱级联更新报告与回滚。 | `OutlineCascade.kt`、`NovelRepository.kt:86-98` | `README.md:473-474`、`scripts/novel_flow_executor.py:2450-2606` |
| 文风 | 可从当前章节生成项目文风档案，并在提示词中使用；缺少多样章指纹、风格库跨作品复用、风格锚点与周期性校准。 | `NovelViewModel.kt:586-611`、`ContextEngine.kt:48-50` | `README.md:498-505`、`scripts/style_fingerprint.py:294-390` |
| 质量检查与修复建议 | 有本地篇幅、占位符、重复段落、禁区、章末钩子提示，且可请求 AI 生成修复计划；但它只是提示，不是发布/下一章的硬门禁，也不自动修复或重检。 | `QualityGate.kt`、`NovelViewModel.kt:510-536,659-702`、`MainActivity.kt:1300-1362` | `README.md:476-484`、`scripts/chapter_gate_check.py:42-229`、`scripts/novel_flow_executor.py:1545-1815` |
| 批量/自动写作 | 当前单次最多 5 章、无持久进度/断点恢复/计划预览；并且批量写作直接写全文，未强制使用已生成分镜。 | `NovelViewModel.kt:659-702` | `scripts/auto_novel_writer.py:595-655`、`README.md:464-474` |

## 缺失

| 能力 | 影响 | 上游一手依据 |
| --- | --- | --- |
| 互动脑洞引导与选项式开书 | 新作者没有从模糊想法到可写设定的分步流程。注意：上游 `SKILL.md` 将“脑洞建图”标为规划中；独立引导脚本已有实现。 | `scripts/interactive_ideation_engine.py:184-427`、`SKILL.md:303-309` |
| 章节五步强制闭环 | 这是最大差距：上游要求记忆更新、一致性、风格校准、去 AI 味校稿、门禁均完成才能进入下一章；当前 AI 续写保存后直接结束。 | `SKILL.md:1-35`、`README.md:425-449`、当前 `NovelViewModel.kt:629-657` |
| 节奏配额、反过早收束与事件冷却 | 缺少慢/中/快节奏、A/B/C 剧情配额、终局防提前解决、事件矩阵冷却，长篇容易连续高潮或过早收线。 | `references/anti-resolution-cooldown-spec.md`、`scripts/anti_resolution_guard.py:158-282`、`scripts/event_matrix_scheduler.py:139-338` |
| 联网调研与知识缺口库 | 无调研任务、来源保存和按题材深度生成计划。 | `README.md:486-496`、`scripts/research_agent.py:331-373` |
| 去 AI 味润色 | 无 AI 痕迹检测报告、两遍润色提示词和重写工作流。 | `README.md:476-484`、`scripts/text_humanizer.py:185-615` |
| 章节 AI 改写 | 有手工编辑和“修复计划”，没有按问题执行 AI 改写、复检及回滚。 | `README.md:464-474`、`scripts/novel_flow_executor.py:1757-1815` |
| 图谱可视化/导出 | 当前只显示文字关系列表，不能生成 Mermaid 或可视化关系图。 | `MainActivity.kt:1220-1229` 对比 `scripts/story_graph_builder.py:470-533` |
| 跨模型/跨 Agent 审稿 | 当前审核明确为本地规则；没有逐章/每 10 章的独立审稿、P0/P1/P2 记录和未解决问题队列。上游协议本身仍标注 Phase 3 规划，故属于可借鉴设计而非已验证交付。 | `MainActivity.kt:1300-1362`；`references/cross-agent-review-protocol.md:1-122`、`scripts/cross_agent_reviewer.py:168-461` |
| 拆书/仿写工作流 | 当前没有结构拆解、爽点/钩子提炼、样书模板化等界面或服务。上游亦标记为部分实现，且任何实现应避免复现受版权保护文本。 | `README.md:507-513`、`SKILL.md:36-37` |

## 建议实施顺序

1. **章节闭环**：将 AI 生成后的“记忆提取 -> 检索索引更新 -> 质量门禁 -> 修复 -> 复检”持久化为可取消、可恢复的任务状态；门禁失败不得自动开始下一章。
2. **长期上下文**：升级 `ContextEngine` 为本地索引/实体映射/时间线与伏笔追踪，生成后自动更新并让作者可审阅。
3. **长篇节奏**：在大纲锚点上增加章节区间、节奏档位、禁止揭露与事件冷却；写前检查、写后校验。
4. **自动化工作流**：增加目标规模、进度面板、断点恢复与可预览的批量写作计划，保留作者审批点。
5. **编辑增强**：补调研、风格库、AI 改写/去 AI 味、图谱可视化；跨 Agent 审稿作为可选高级功能。
