# 作品 ZIP 闭环 Spec

## Parent Spec

- Parent Spec: [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md)
- Current Approval Scope: 作品页上传 ZIP 写入媒体库并绑定、校验解压、预览/发布、删除与删作品级联、迁移清空旧数据、安全解压。
- Sibling Specs:
  - [2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md)
  - [2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md)

## Global Constraints

- Shared constraints: 继承父 spec 的共享约束，见 [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md) 的 `Global Constraints`。
- 替换策略：删除旧 ZIP 后再上传新 ZIP；不保留多 revision 产品语义（实现内部可用 revision 目录名，但不对用户暴露版本管理）。
- 校验失败不得留下半绑定脏数据：无孤立 `media_asset`、无媒体存储中的残留 ZIP 文件、无半解压/staging 目录、无无效原型行。
- 安全解压最低要求：扩展名白名单、根目录 `index.html`、压缩包 ≤10MB、解压累计 ≤40MB、文件数 ≤200、**拒绝软链接**、**拒绝路径越界**、解压过程中按写入进度累计限制磁盘占用（不得仅在单个文件完整写完后才检查超限）。
- 原始 ZIP 必须写入媒体库存储布局（`yyyy/MM/<uuid>.zip`）并创建 `media_asset` ARCHIVE 行；不得只解压到原型目录却无媒体资产。
- `media_asset_id` 在原型表上须 **UNIQUE**（保证一 ZIP 一作品）。

## Feature Slice 1: 上传绑定与校验解压

- [ ] Implementation status: Not done

### Behavior

- 管理员在作品编辑上下文上传 ZIP 后：创建 ARCHIVE 媒体资产（媒体存储路径）→ 绑定当前作品 → 校验并解压到私有原型目录 → 返回原型视图。
- 校验失败：返回可读原因；回滚媒体行、媒体 ZIP 文件与解压 staging。

### Public Interface

- `POST /api/v1/admin/portfolio/projects/{projectId}/prototype`（multipart ZIP）：成功 **201**；响应体 `ProjectPrototypeView` 至少含：
  - `mediaAssetId`（number）
  - `sourceName`（string，原始文件名）
  - `sizeBytes`（number，ZIP 大小）
  - `validationStatus`（string，成功时为 `VALID`）
  - `fileCount`（number）
  - `totalBytes`（number，解压后合计）
  - `previewUrl` 或等价预览入口字段（string，供管理端 iframe）
- 作品管理详情 `AdminProjectDetailView.prototype`：无原型时为 `null`；有原型时含上述字段子集。
- `portfolio_project_prototype` 经新迁移增加非空 `media_asset_id`（UNIQUE，FK → `media_asset`）。

### Error and Boundary Cases

- 非 ZIP / magic 不符 / 超压缩包大小 / 无 `index.html` / 含白名单外扩展名 / 含软链 / 含路径越界 / 解压超限：4xx + 可读消息；无脏数据（含无残留媒体 ZIP 文件）。
- 作品已有绑定 ZIP 时再次上传：**409**，提示先删除；原绑定与原 ZIP 不变。
- 未登录：401/403。

### Non-goals

- 「从媒体库选择」绑定。
- 版本历史列表。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：管理端登录 → 创建/取得作品 → `POST .../prototype` 上传夹具 ZIP → 读作品详情 / media list → 预览或发布相关入口。
- 默认断言层级：HTTP + DB（`media_asset`、`portfolio_project_prototype`）+ 磁盘（媒体存储 ZIP 与解压树）；调试产物 `.tmp/media-archive-portfolio-zip/`。
- 防 mock 逃逸禁令：不得跳过解压校验硬编码成功；不得只插 DB 不写盘；失败用例必须断言无残留 DB 行、无残留媒体 ZIP、无残留 staging/解压目录。
- 测试必须能在「只存 ZIP 不解压」「失败仍留 media 行或 ZIP 文件」「禁止扩展名未校验」「仅文件写完后才检查体积」等假实现上失败。

#### AC-1: 合法 ZIP 上传入库并绑定

- 触发：对草稿作品上传含根目录 `index.html` 的合法静态 ZIP。
- 必须可观察：**201**；响应含 `mediaAssetId`、`validationStatus=VALID`、`sourceName`、`sizeBytes`；`media_asset` 有 ARCHIVE 行且 `storage_path` 指向媒体存储下的 zip 文件且文件存在；原型表有该 `project_id` 与 UNIQUE `media_asset_id`；私有解压目录存在 `index.html`；作品详情 `prototype != null`。
- 验证手段：Portfolio 管理端集成测试 + 查 DB/磁盘。

#### AC-2: 非法 ZIP 失败且无脏数据

- 触发：参数化分别上传至少覆盖：**非 zip**、**无 index.html**、**含软链接**、**含路径越界**、**含白名单外扩展名（如 `.exe`）**、**超文件数上限**、**解压过程中累计超 40MB**（构造多文件或单文件使写入进度触发限制，而非仅最终合计检查可逃）。
- 必须可观察：每次失败均 4xx + 可读原因；无新 ARCHIVE 行；媒体存储无对应新 ZIP 文件；无原型行；无残留 staging/解压目录。
- 验证手段：集成测试参数化；断言 DB 计数与文件/目录不存在。

#### AC-3: 已绑定再次上传被拒绝

- 触发：作品已有原型后再次 `POST .../prototype`。
- 必须可观察：**409**；原 `mediaAssetId`、原 ZIP 文件与解压目录不变。
- 验证手段：集成测试。

## Feature Slice 2: 删除、删作品与发布预览

- [ ] Implementation status: Not done

### Behavior

- 作品页删除原型：删除 ARCHIVE 媒体 + 原型行 + 私有/公开解压树。
- 删除作品：连带删除绑定 ZIP 与解压产物。
- 发布：将私有解压树发布到公开路径；公开入口遵循既有 URL 形态。
- 草稿：仅管理端预览资源可读；未发布不得通过公开入口访问。

### Public Interface

- `DELETE /api/v1/admin/portfolio/projects/{projectId}/prototype`：有原型时 **204**；无原型时 **204**（幂等成功）。
- 删除作品既有管理端删除入口（须扩展清理 ZIP/媒体）
- `GET /api/v1/admin/portfolio/projects/{projectId}/prototype-preview/{*path}`
- 公开：`prototypeEntryUrl` / `/uploads/prototypes/...`（保持既有约定）
- 发布作品既有 `publish` 流程调用原型 publish

### Error and Boundary Cases

- 无原型时删除：**204** 幂等成功（已统一，不再允许 404）。
- ONLINE 且无 `demoUrl` 时发布仍要求存在可用静态原型（保持既有校验语义）。

### Non-goals

- 媒体库 UI。
- 改绑到其他作品。

### Acceptance Criteria

#### Shared Verification Baseline

- 同 Feature Slice 1 的鉴权与真实 HTTP/DB/磁盘基线；发布用例需覆盖公开可读性断言。
- 防 mock 逃逸：不得只删 DB 留磁盘，或只删磁盘留媒体行。

#### AC-4: 作品页删除清媒体与解压

- 触发：已绑定作品调用 `DELETE .../prototype`。
- 必须可观察：**204**；作品详情 `prototype == null`；对应 `media_asset` 行不存在；ZIP 文件与解压目录消失。
- 验证手段：集成测试。

#### AC-5: 无原型时删除幂等

- 触发：对无原型作品调用 `DELETE .../prototype`。
- 必须可观察：**204**；不创建任何媒体/原型行。
- 验证手段：集成测试。

#### AC-6: 删除作品连带删 ZIP

- 触发：删除已绑定 ZIP 的作品。
- 必须可观察：作品不存在；对应 ARCHIVE 不存在；解压目录不存在。
- 验证手段：集成测试。

#### AC-7: 草稿预览与发布后公开

- 触发：草稿上传后访问管理预览；发布前匿名请求公开 `prototypeEntryUrl`；发布后再次请求入口 `index.html` 并跟随相对链接打开第二页。
- 必须可观察：草稿预览可读；发布前公开 URL 返回 **404**（或等价不可匿名读取）；发布后公开可获取入口页与相对链接目标页。
- 验证手段：集成测试（HTTP 取预览/公开资源）；多页面夹具 ZIP。

#### AC-8: 迁移清空旧独立原型

- 触发：执行新迁移（在已有 V30 数据或测试夹具模拟旧行之后）。
- 必须可观察：旧无 `media_asset_id` 语义的历史原型行被清空；迁移后不存在「无 media_asset 却仍声称有原型」的行；新写入必须带 `media_asset_id`。
- 验证手段：Flyway 迁移测试或集成启动断言；文档化迁移 SQL 行为。
- 降级理由：若全库迁移测试过重，可用迁移 SQL 单测 + 启动后空表断言；须仍执行真实迁移脚本而非 mock。

### Done When

- 所有 Acceptance Criteria 都通过对应的验证手段完成自动化验证。
- 没有核心需求是通过直接状态修改、硬编码数据、占位行为或 fake integration 满足的。
