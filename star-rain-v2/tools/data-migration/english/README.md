# V1 英语内容导入工具

`Import-V1BasicContent.ps1` 是手动执行的数据导入工具，不是 Flyway 版本或英语模块运行时代码。脚本从本机 V1 数据库读取内容，将基础词汇、语法、阅读、听力和写作记录导入已完成 V2 表结构迁移（至少 V2_022）的目标库。

先备份并核对目标库，显式传入 `-TargetDatabase`。建议先用隔离库验收，再决定是否对正式 V2 库执行。首次导入的语法、阅读、听力和写作内容会直接设为公开；`INSERT IGNORE` 不会覆盖此后在 V2 手工编辑的记录。导入后逐项核对数量、发布状态、正文和音频引用。

## 词汇记忆体系相关列（V2_022 之后新增）

| V1 来源 | V2 目标 | 处理方式 |
|---|---|---|
| `vocabulary_word.scene_meaning` | `sr_english_vocabulary_word.scene_meaning` | `COALESCE(目标, 来源)`：只补空值，重跑不覆盖 V2 手工编辑 |
| `vocabulary_word.inflections` | `sr_english_vocabulary_word.inflections` | 同上。V1 全表为空，导入后仍为空，属于**源数据本身缺失**，不是导入失败 |
| `vocabulary_word.memory_count` | `sr_english_vocabulary_word.memory_count` | `GREATEST(目标, 来源)`：只增不减 |
| `vocabulary_word.last_memory_at` | `sr_english_vocabulary_word.last_memory_at` | `COALESCE(目标, 来源)` |
| `vocabulary_word_audio` | `sr_english_vocabulary_word_audio` | 见下 |

**V1 媒体 ID 不能直接当作 V2 媒体 ID。** 媒体资产在两边按各自的库独立生成身份，V1 的 `media_asset_id` 在 V2 里可能指向完全不相干的资源。因此音频导入时 `media_asset_id` **一律写 NULL**，只保留 `provider / source_url / license_note / is_primary`，并在 `license_note` 里写明原始 V1 媒体编号，需要真人发音时在 V2 媒体库重新上传后回填。这一条与听力条目音频引用留空是同一个口径。

音频导入的幂等性靠「同词 + 同口音 + 同 `source_url`」的 `LEFT JOIN` 判定，而不是只靠 `INSERT IGNORE`：唯一键 `uk_sr_english_vocab_audio_media (word_id, media_asset_id)` 在 `media_asset_id` 为 NULL 时按 MySQL 语义**不生效**（唯一索引不约束 NULL），只靠 `INSERT IGNORE` 会在重跑时插入重复行。

本机 V1 库实测：`vocabulary_word` 7053 行全部有 `scene_meaning`，`inflections`、`memory_count`、`last_memory_at` 全为空，`vocabulary_word_audio` 为 0 行。因此这四项导入后仍为空是预期结果。

## 学习进度不随内容导入

`account_vocabulary_*`（记忆、复习日志、显示偏好、学习设置）属于**个人数据**，按账户隔离，不在这份内容导入的范围内。游客的本机进度由前端「学习进度」页的导出/导入与「合并本机进度到账号」入口处理，接口是 `POST /api/account/english/vocabulary/import-local`。

示例：

```powershell
./Import-V1BasicContent.ps1 -SourceDatabase star_rain_notes -TargetDatabase star_rain_v2_english_acceptance
```
