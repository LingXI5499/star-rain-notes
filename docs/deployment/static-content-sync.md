# 静态站教程与博客同步

独立静态网站位于 `star-rain-notes-site/`，当前主应用前后端位于根目录的 `frontend/` 和 `backend/`。两套前端在同一个 `main` 分目录维护，分别构建部署。历史源码、旧工作区、数据库备份、旧报告和发布包归入本地 `history/`，不进入版本库。

## 内容范围

`backend/content/static-site-content.json` 保存已整理的公开内容：31 门教程、2,659 个章节、16 篇博客，包含课程分类、分组、标题、摘要、Markdown 正文及博客标签、专题。正文引用的 9 张图片位于 `frontend/public/images/content/`。

来源仍有 884 个章节正文为“待编写”，同步保留这一状态，不把它们计为完整教程。初次补充同步保留来源之外的旧课程；后续已按用户明确要求执行严格清理，当前教程目录只包含来源中的 6 个知识体系、31 门教程和 2,659 个章节。保留课程的原主键、学习事件、知识卡片、练习和历史版本仍保留。按课程 slug 和章节 slug 匹配，分组先按标题匹配，再用现有章节归属识别改名。

静态源中 MySQL、Redis 和 AI Agent 存在同名或仅大小写不同、slug 不同的标签。主应用标签名有忽略大小写的唯一约束，导入时沿用已有标签 ID、名称和地址，保留全部文章关联，并在结果的 `tagAliases` 中记录源 slug 到已有 slug 的对应关系。

原静态站保留公开接口导入时的 Markdown 缓存。对于渲染结果与当前静态正文一致的内容，保留原 Markdown；已编辑的 HTML 则转换为 Markdown，逐篇比对可见文字、代码块、链接、图片和表格。内容包不含原站账户、私有配置、数据库导出或静态站应用代码。

导入缓存不进入版本库。克隆仓库后，导出器会根据正文哈希复用已审核内容包中的 Markdown；缓存只作为本机维护时的可选来源。可以用 `node tools/export-static-content.mjs --no-import-cache` 验证不依赖缓存的导出。静态正文统一为 LF，固定原词汇与语法的校验基线。

## 更新内容包

已有静态站依赖时，在根目录执行：

```powershell
node tools/export-static-content.mjs
```

导出器只读取静态站内容、复制被引用的图片并生成主应用内容包，不修改静态站源码。图片缺失、图片同名冲突、无法识别的 HTML 或转换不一致会中止。正常应用启动不会自动导入内容包。

## 本地数据库同步

要求已有 Python 的 `PyMySQL`、`PyYAML`，以及现有 JDK 21、MySQL 客户端和当前后端构建产物。先用独立测试数据库演练；不能把生产数据库用作测试库。

```powershell
# 默认仅预演并回滚，不保留内容写入。MySQL 自增序号可能产生空位。
python tools/sync-static-content.py

# 使用已有安装，不下载或替换系统工具。
$env:STAR_RAIN_JAVA_HOME = '<已有 JDK 21 目录>'
$env:STAR_RAIN_MYSQLDUMP = '<已有 mysqldump.exe 路径>'
python tools/sync-static-content.py --apply
```

默认读取 `backend/application-local-secret.yml`，可用 `--config` 指定根目录内的私有配置。只接受回环 MySQL 地址。默认预演的写入全部回滚；正式执行前必须成功生成整库 SQL 备份。

同步按以下步骤执行：

1. 在事务中预检全部来源内容，遇到审核中教程、同 slug 未发布/已撤回内容即中止并回滚。
2. 在 `history/content-sync-日期时间/` 保存整库备份。
3. 在一个事务中更新来源覆盖的工作区，保留来源之外的数据，不删除原主键。
4. 保存待执行发布清单，启动独立本地维护进程。该进程读取当前构建中的现有业务服务，以数据库中已有所有者身份生成教程不可变版本、发布新增博客并重建搜索与 SEO。
5. 发布及派生数据重建在另一个事务中完成。维护进程只绑定回环地址及随机端口，关闭邮件、账号初始化、孤儿文件清理和搜索引擎通知，执行后退出；不会修改账户、密码、角色、Session 或 CSRF 配置。

维护类单独编译在 `.local/dev/content-maintenance/`，不打入后端 JAR。临时进程合并搜索与 SEO 的重复监听器处理，最后统一重建一次；主应用正常发布逻辑不变。

工作区事务与发布事务分别提交。若发布失败，工作区写入、原公开版本、整库备份与 `pending-publication.json` 保留；检查同目录的 `publication.log`。修正后可以用 `backend/tools/publish-static-content.ps1 -Plan <待发布清单> -Config <本地配置>` 恢复发布，不需要再次导入工作区。不要删除原备份或清空数据库。

## 验证

若本次任务明确要求删除来源之外的教程目录，在完成内容同步后执行：

```powershell
# 先查看移除范围；预演全部回滚。
python tools/prune-static-tutorials.py
# 正式清理前自动保存整库备份。
python tools/prune-static-tutorials.py --apply
```

此工具只支持本地数据库。保留来源课程与章节的现有 ID，删除多余课程、章节、分组、分类及被删除课程的版本记录；保留历史学习事件和冻结计划条目，受影响的活动计划取消，待完成任务跳过。若删除对象关联知识卡片/练习或正在审核则中止。重新发布发生目录变化的保留课程，并重建搜索与 SEO，旧公开地址返回 404。

严格清理同样有单独的工作区清理事务和发布事务；失败时保留备份、待发布清单及日志，恢复发布的方法与上述同步流程一致。其集成检查使用 `tools/test_prune_static_tutorials.py`，通过 `STAR_RAIN_CONTENT_TEST_CONFIG` 指定独立测试库，`STAR_RAIN_PRUNE_BASELINE` 指定该测试副本对应的清理前保护数据校验文件。

集成回归只允许名称以 `star_rain_content_sync_test_` 开头的独立测试库。先将开发库备份恢复为独立测试副本，再使用其私有配置运行同步和回归：

```powershell
python tools/sync-static-content.py --config <测试配置> --apply
$env:STAR_RAIN_CONTENT_TEST_CONFIG = '<测试配置>'
$env:STAR_RAIN_CONTENT_BASELINE = '<同步前保护数据校验清单>'
python tools/test_sync_static_content.py -v
```

覆盖全部章节的工作区/公开版本一致性、博客正文与标签、重复同步无变化、审核冲突的事务回滚、历史版本与其他模块数据保留。另需检查公开接口、图片和代表性页面，并执行前端生产构建。

本说明及本地验证不代表 GitHub 推送、服务器部署或生产验收。
