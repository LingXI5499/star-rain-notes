# 媒体库 ARCHIVE Spec

## Parent Spec

- Parent Spec: [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md)
- Current Approval Scope: 将 ZIP 升为媒体库一等资产类型，并规定删除与列表契约；不解压、不做作品页上传 UI。
- Sibling Specs:
  - [2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md)
  - [2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md)

## Global Constraints

- Shared constraints: 继承父 spec 的共享约束，见 [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md) 的 `Global Constraints`。
- 本 slice 内 `POST /api/v1/admin/media-assets` 必须拒绝 ZIP；ARCHIVE 资产由作品上传链路（slice 2）写入。
- 删除 ARCHIVE 时若存在作品绑定，必须同步清除绑定与解压产物；本 slice 以 `DELETE /api/v1/admin/media-assets/{id}` 为公共入口保证可观察结果。
- 测试夹具允许：在集成测试 setup 中经 **slice 2 的真实 `POST .../prototype` 公共入口** 预置 ARCHIVE；禁止把「直接 INSERT `media_asset` / 手写磁盘文件」当作本 slice 的产品验收成功证据。
- AC-3（未绑定删除）setup：先经 slice 2 `POST .../prototype` 写入绑定态，再使用 **仅用于测试 setup** 的受控 helper 删除 `portfolio_project_prototype` 行（及解压目录），**保留** `media_asset` 行与媒体存储中的 ZIP。不得调用作品侧 `DELETE .../prototype` 来「解除关联」——该入口会连带删除 ARCHIVE，与产品「无仅解除关联」一致。该 helper 不得计入 AC-1/AC-4 的成功证据。

## Feature Slice 1: ARCHIVE 类型与列表统计

- [ ] Implementation status: Not done

### Behavior

- `media_asset.asset_type` 支持 `ARCHIVE`（扩展名 `zip`，MIME 兼容 `application/zip` / `application/x-zip-compressed`）。
- 管理端可通过列表与 summary 看到压缩包数量与条目。
- 磁盘上保留原始 ZIP 文件，与现有媒体存储布局一致（`yyyy/MM/<uuid>.zip`）。

### Public Interface

- `GET /api/v1/admin/media-assets?assetType=ARCHIVE`：返回 ARCHIVE 分页列表。
- `GET /api/v1/admin/media-assets/summary`：增加 `archives` 计数字段（保留既有 `total` / `images` / `audio` / `documents`）。
- `DELETE /api/v1/admin/media-assets/{id}`：删除任意类型资产；对 ARCHIVE，若存在 `portfolio_project_prototype.media_asset_id = id`，须删除该原型行并清理对应私有/公开解压目录，再删媒体行与 ZIP 文件；成功响应 **204**。
- `MediaAssetView` 对 ARCHIVE 返回 `assetType=ARCHIVE`、`originalName`、`sizeBytes`、`publicUrl` 等既有字段；无宽高要求。
- Flyway 新迁移扩展 `media_asset` 类型 CHECK，包含 `ARCHIVE`。

### Error and Boundary Cases

- 未知 `assetType` 查询参数：与现有 IMAGE/AUDIO 列表行为一致，返回 **空列表**（非 400）。
- summary 在零 ARCHIVE 时 `archives=0`。

### Non-goals

- 不在本 slice 实现解压、原型校验、作品绑定写入。
- 不提供媒体库 ZIP 上传入口。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：管理端鉴权 → 经 slice 2 `POST /api/v1/admin/portfolio/projects/{id}/prototype` 写入一条 ARCHIVE（及绑定）→ 再调用本 slice 的 media list/summary/delete 公共 HTTP 入口断言。
- 默认断言层级：HTTP 状态与 JSON 体；查 `media_asset` / `portfolio_project_prototype` 行与磁盘 ZIP/解压目录是否存在或消失；调试产物落到 `.tmp/media-archive-portfolio-zip/`。
- 防 mock 逃逸禁令：不得伪造 summary 数字；不得跳过鉴权；不得只断言 DTO 构造而不走 Controller；AC-1/AC-4 不得用直接改库代替 slice 2 上传入口来「证明」写入成功。
- 测试必须能在「未扩 CHECK / 未写 archives 字段 / delete 不落盘 / 已绑定删除不清理原型」的假实现上失败。
- 实现顺序依赖：本 slice 的 AC-1/AC-3/AC-4 自动化可与 slice 2 上传能力同 PR 或在 slice 2 之后合并；不得在 slice 2 上传入口不可用时声称本 slice Done。

#### AC-1: 列表与统计可见 ARCHIVE

- 触发：记录调用前 `summary.archives` 与 `summary.total`；经 slice 2 上传恰好 **1** 条新 ARCHIVE 后，调用 `GET .../media-assets?assetType=ARCHIVE` 与 `GET .../summary`。
- 必须可观察：列表包含该 ZIP 的 `id`、`originalName`、`assetType=ARCHIVE`、`sizeBytes`；`archives` 比调用前 **精确 +1**；`total` 比调用前 **精确 +1**。
- 验证手段：后端管理端集成测试（真实 HTTP + DB）；断言响应 JSON 字段与增量。

#### AC-2: 通用上传拒绝 ZIP

- 触发：已登录管理端对 `POST /api/v1/admin/media-assets` 提交 `.zip` multipart。
- 必须可观察：请求失败（4xx）；响应含可读错误；`media_asset` 无新 ARCHIVE 行；uploads 目录无对应新文件。
- 验证手段：`MediaAdminIntegrationTest` 或同等集成测试；覆盖扩展名/MIME 拒绝。

#### AC-3: 删除未绑定 ARCHIVE

- 触发：经 slice 2 `POST .../prototype` 预置后，用 AC-3 专用 setup helper 去掉原型行/解压目录并保留 `media_asset`+ZIP，得到 **无** `portfolio_project_prototype` 引用的 ARCHIVE；记录删除前 `archives`；调用 `DELETE /api/v1/admin/media-assets/{id}`。
- 必须可观察：**204**；该 id 不再出现在列表；磁盘 ZIP 文件被删除；`archives` 精确减一。
- 验证手段：管理端集成测试，断言 DB + 文件系统。

#### AC-4: 删除已绑定 ARCHIVE 自动清作品关联

- 触发：经 slice 2 上传使作品绑定某 ARCHIVE 且完成解压后，调用 `DELETE /api/v1/admin/media-assets/{id}`。
- 必须可观察：**204**；该 `media_asset` 行不存在；磁盘 ZIP 不存在；`portfolio_project_prototype` 中该 `project_id` 行不存在；`GET` 作品管理详情中 `prototype == null`（或等价「无原型」JSON）；该次解压使用的私有目录与公开目录（若已 publish）均不存在。
- 验证手段：跨 media + portfolio 的管理端集成测试；slice 2 上传 → media delete → 读作品详情 + 查目录。

### Done When

- 所有 Acceptance Criteria 都通过对应的验证手段完成自动化验证。
- 没有核心需求是通过直接状态修改、硬编码数据、占位行为或 fake integration 满足的。
