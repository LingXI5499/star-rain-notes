# 英语内容草稿 SQL 导入说明

## 交付内容

`star-rain-english-drafts.sql` 只新增以下内容：

- 6 个 A1–C2 听力材料草稿，含完整听力稿、建议总时长和 71 个中英双语时间片段。
- 6 个语音规则草稿。
- 6 个写作资源草稿、6 个写作任务草稿。
- 6 个词族、27 个词族成员，并尽量关联词库中已有的 `use`、`travel`、`decide`、`create`、`analyze`、`govern` 主词。

脚本不删除、不清空业务数据，不修改 Flyway 记录，也不写入媒体文件。

## 重要边界

- 听力的音频留空，`audio_media_id` 为 `NULL`。其余听力学习内容与时间片段已经生成。
- 时间片段按建议语速预设。以后上传真实音频后，应在后台试听并按真实波形微调开始、结束时间。
- 听力封面和来源地址是可选项，脚本不伪造媒体 ID 或网址。
- 听力、语音规则、写作资源和写作任务全部是 `DRAFT`，不会出现在公开页面。
- 当前数据库的词族表没有发布状态字段，因此词族无法保存为“草稿”；导入后会按现有系统逻辑立即可见。这不是脚本绕过发布状态，而是当前表结构的客观限制。
- 脚本使用固定 12 位内容编号和存在性判断，可以重复执行，不会重复创建本包内容。

## 第一步：备份

在宝塔面板进入“数据库”，找到 `star_rain_notes`，点击“备份”。确认备份成功后再继续。

## 第二步：上传

将整个 `english-draft-content-seed` 文件夹上传到：

```text
/www/star-rain-upgrade/english-draft-content-seed/
```

## 第三步：校验文件

在宝塔终端执行：

```bash
cd /www/star-rain-upgrade/english-draft-content-seed
sha256sum -c SHA256SUMS.txt
```

应看到两个文件均显示 `OK`。

## 第四步：导入

在宝塔终端执行下面一整行命令：

```bash
/www/server/mysql/bin/mysql --default-character-set=utf8mb4 -u star_rain -p star_rain_notes < /www/star-rain-upgrade/english-draft-content-seed/star-rain-english-drafts.sql
```

按 Enter 后，在 `Enter password:` 提示处输入数据库用户 `star_rain` 的密码。密码输入时终端不会显示字符，这是正常现象。

如果服务器没有 `/www/server/mysql/bin/mysql`，使用：

```bash
mysql --default-character-set=utf8mb4 -u star_rain -p star_rain_notes < /www/star-rain-upgrade/english-draft-content-seed/star-rain-english-drafts.sql
```

不要先进入 `mysql>` 再粘贴上面的命令；这是 Linux 终端命令，应直接在 `[root@服务器 ~]#` 提示符后执行。

## 第五步：检查输出

导入结束时应看到：

```text
listening_drafts                         6
pronunciation_drafts                     6
writing_resource_drafts                  6
writing_prompt_drafts                    6
word_families_no_status_column           6
word_family_members                     27
word_family_main_word_links              0 到 6
listening_segments                      71
listening_items_without_audio_expected_6 6
invalid_or_overflow_segments_must_be_zero 0
overlapping_segments_must_be_zero        0
non_draft_publish_rows_must_be_zero      0
```

`word_family_main_word_links` 正常情况下应为 6。若小于 6，说明主词库中缺少对应原形词；词族和成员仍然已经成功创建，可之后在后台补主词关联。

## 第六步：后台验收

数据库内容实时生效，不需要重启 Java 服务。登录后台并强制刷新浏览器：

1. 听力管理应出现 6 条草稿；每条有听力稿、建议时长和时间片段，音频为空。
2. 语音规则应出现 6 条草稿。
3. 写作资源应出现 6 条草稿，写作任务应出现 6 条草稿。
4. 词族管理应出现 6 个词族和共 27 个成员。
5. 公开页面不应出现新增的听力、语音规则或写作内容，因为它们尚未发布。

听力后续操作顺序：上传与该稿件匹配的完整音频 → 试听 → 按实际音频微调 71 个片段中的对应时间 → 补充练习 → 检查标签 → 发布。

## 失败处理

如果终端出现 `ERROR`，不要反复修改 SQL。保存完整终端输出，并使用宝塔数据库备份恢复到导入前状态后再定位。脚本本身使用事务；通常发生错误时不会提交本轮新增内容。
