# V1 英语内容导入工具

`Import-V1BasicContent.ps1` 是手动执行的数据导入工具，不是 Flyway 版本或英语模块运行时代码。脚本从本机 V1 数据库读取内容，将基础词汇、语法、阅读、听力和写作记录导入已完成 V2 表结构迁移的目标库。

先备份并核对目标库，显式传入 `-TargetDatabase`。建议先用隔离库验收，再决定是否对正式 V2 库执行。首次导入的语法、阅读、听力和写作内容会直接设为公开；`INSERT IGNORE` 不会覆盖此后在 V2 手工编辑的记录。导入后逐项核对数量、发布状态、正文和音频引用；V1 媒体 ID 不能直接当作 V2 媒体 ID。由于本机缺少 V1 音频文件，听力条目的音频引用留空，音频需另外补齐。

示例：

```powershell
./Import-V1BasicContent.ps1 -SourceDatabase star_rain_notes -TargetDatabase star_rain_v2_english_acceptance
```
