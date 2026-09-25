# 媒体与原型：文件和数据库的一致性

当前上传是「先落盘，再写库」。文件系统和数据库不在同一个事务里。进程在两步之间崩溃时，磁盘上会留下没有数据库记录的文件。本轮不重写上传管线，只记录现有补偿和仍会残留的情况。

## 谁负责清理

| 路径 | 正常失败时 | 进程崩溃时 |
|---|---|---|
| `MediaService.upload`（图片、音频、PDF） | 写库失败会删除刚写入的原文件，并调用 `ImageVariantSupport.deleteVariants` | 原文件和变体可能留在 `app.media.storage-dir` |
| `MediaService.createArchiveAsset`（ZIP） | 插入或回填 `publicUrl` 失败会删除刚写入的 ZIP。这两步在同一个数据库事务里，更新失败不会留下没有最终地址的行 | ZIP 可能留在 `app.media.private-storage-dir` |
| `PortfolioPrototypeService.bind` | 解压、移动、复制或写库失败会删除本次的 staging、私有目录和公开目录 | 本次 `projectId/revision` 目录可能留在私有根或公开根下 |

写库失败时的删除是尽力而为。删除本身再失败只记日志，不改变接口返回的错误。

## 仍会残留的情况

- JVM 在落盘成功、数据库提交前退出。没有启动任务扫描孤儿文件。
- `upload` 在原文件已经写入、生成图片变体时抛出 `IOException`。这个失败发生在写库之前，现有 catch 不删除已经写入的原文件。
- `bind` 在新行已经提交之后，再删除上一版目录和旧媒体。这一步失败不会回滚新绑定，旧目录可能留下。

## 不要在这次补偿里做的事

不把文件移动放进数据库事务，不增加定时清理，不改上传接口的状态码和错误 code。孤儿文件需要人工按存储根目录核对：没有对应 `media_asset.storage_path` 或 `portfolio_project_prototype.storage_key` 的目录才可以删。
