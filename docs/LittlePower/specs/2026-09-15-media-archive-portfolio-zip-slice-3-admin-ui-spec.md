# 媒体库与作品页 UI Spec

## Parent Spec

- Parent Spec: [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md)
- Current Approval Scope: 管理端媒体库与作品编辑页的 ZIP 相关界面；依赖 slice 1/2 的 API 契约。
- Sibling Specs:
  - [2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md)
  - [2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md)

## Global Constraints

- Shared constraints: 继承父 spec 的共享约束，见 [2026-09-15-media-archive-portfolio-zip-spec.md](./2026-09-15-media-archive-portfolio-zip-spec.md) 的 `Global Constraints`。
- 媒体库布局采用 **侧栏分类 + 右侧内容区**（用户已选方案 C）。
- 保持后台 Inkspace 风格与现有 `admin.css` token；修复分类按钮呈原生样式、内容挤叠、图片巨大展开等问题。
- 媒体库压缩包分类 **无上传入口**；作品编辑页 **无「从媒体库选择」**。
- 与 slice 2 对齐：已关联 ZIP 时 **不提供上传/替换**；须先删除再上传；文案禁止「替换静态原型」。

## Feature Slice 1: 媒体库侧栏与 ARCHIVE 展示删除

- [ ] Implementation status: Not done

### Behavior

- 左侧分类：全部 / 图片 / 音频 / 文档 / 压缩包，显示计数。
- 右侧内容区：图片固定尺寸缩略图（显示宽高均 ≤ **160px**）；音频 / 文档 / ZIP 为类型块 + 文件名 / 大小。
- 可删除 ZIP（需确认）；删除后列表与计数更新；若该 ZIP 曾绑定作品，作品侧关联消失（由 API 保证，UI 刷新后可见）。
- 去掉「原型 ZIP 请前往作品管理」类替代管理提示框。

### Public Interface

- 页面：`MediaLibraryView`（后台媒体库路由）。
- 调用：`fetchMediaSummary`、`fetchMediaAssets`、`deleteMedia`；summary 使用 `archives`。
- 无媒体库 ZIP `<input type=file accept>` 含 zip 的上传控件，无「上传压缩包」主按钮。

### Error and Boundary Cases

- 删除失败：可读错误提示，列表不误删。
- 空压缩包分类：空状态文案，不报错。
- 窄屏：侧栏与内容区不严重横向溢出；主要操作可点。

### Non-goals

- 媒体库上传 ZIP。
- 拖拽上传（非本轮必达）。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：真实登录管理端 → 打开媒体库 → 切分类 → 删除（夹具数据经 slice 2 API 预置）。
- 默认断言层级：浏览器 DOM 可见性/布局约束 + 网络请求；调试截图与 trace 落到 `.tmp/media-archive-portfolio-zip/`。
- 防 mock 逃逸禁令：不得用 Storybook 假数据代替真实 API；不得只测 CSS 类名存在而不测缩略图边界盒。
- 测试必须能在「仍拦截 ZIP 并跳转作品管理」「图片无 max 尺寸撑破布局」的实现上失败。

#### AC-1: 侧栏分类样式与缩略图尺寸

- 触发：登录后打开媒体库；先点「图片」观察缩略图；再点「压缩包」。
- 必须可观察：侧栏分类控件非浏览器原生裸按钮（例如具有非默认 `border-radius` 或主题色背景/边框）；计数来自 summary；压缩包视图右侧仅 ARCHIVE；图片类目下任一可见缩略图元素 `getBoundingClientRect().width <= 160` 且 `height <= 160`。
- 验证手段：Playwright（或项目既有前端 E2E）真实会话；断言 computed style / 边界盒。

#### AC-2: 媒体库删除 ZIP

- 触发：在压缩包分类删除一条 ARCHIVE（确认对话框确认）。
- 必须可观察：条目从列表消失；summary 压缩包计数减少；若曾绑定作品，打开该作品编辑页不再显示该 ZIP。
- 验证手段：E2E；可与 API 断言组合。

#### AC-3: 无媒体库 ZIP 上传入口

- 触发：打开媒体库各分类（含压缩包）。
- 必须可观察：不存在「上传压缩包」主按钮；不存在 `accept` 包含 `zip` 的文件输入；不存在强制跳转作品管理的 ZIP callout。
- 验证手段：E2E DOM 断言。

## Feature Slice 2: 作品编辑页 ZIP 上传删除与状态

- [ ] Implementation status: Not done

### Behavior

- 未关联时：提供 **上传 ZIP**。
- 已关联时：展示文件名、大小、`validationStatus`、关联结果；提供 **删除 ZIP** 与草稿 **后台预览**（iframe 或打开 `previewUrl`）；**隐藏或禁用上传控件**（不可「替换」上传）。
- 上传中 / 成功 / 校验失败状态可感知；失败后（仍未关联成功时）可对同一文件重试。
- 删除即清绑定并从媒体库移除（与 API 一致）；删除成功后恢复上传入口。

### Public Interface

- 页面：`PortfolioEditView`。
- 调用：作品原型上传/删除 API（slice 2）；超时不少于 60s。
- 不渲染「从媒体库选择」；不渲染「替换静态原型」文案。

### Error and Boundary Cases

- 校验失败：展示服务端可读原因；不显示为已关联成功；上传入口仍可用以便重试。
- 未保存新作品（无 `projectId`）：上传入口不可用，并显示需先保存的明确提示。
- 已关联时若仍触发上传请求：UI 应阻止；若请求发出则展示 409 可读错误且不改变原关联。
- 键盘焦点：上传/删除按钮可聚焦；焦点环可见。

### Non-goals

- 「从媒体库选择」。
- 「仅解除关联」。
- 文件导出。

### Acceptance Criteria

#### Shared Verification Baseline

- 主路径：登录 → 打开已有作品编辑页 → 上传夹具 ZIP → 观察状态/预览 → 媒体库可见 → 删除。
- 默认断言层级：DOM + 网络；调试产物 `.tmp/media-archive-portfolio-zip/`。
- 防 mock 逃逸：不得 stub 上传成功而不发真实 multipart；失败用例须用真实拒绝响应或真实非法文件。

#### AC-4: 作品页上传后两侧可见

- 触发：在已保存且未关联的作品编辑页上传合法 ZIP。
- 必须可观察：页内显示文件名/大小/`VALID` 或等价成功态；出现可访问的预览（iframe `src` 含预览路径或预览控件可见且可加载）；媒体库压缩包分类出现同名条目；上传控件隐藏或禁用。
- 验证手段：E2E 跨两页断言。

#### AC-5: 校验失败可感知并可重试

- 触发：上传非法 ZIP（如无 index.html）；查看错误后再次选择合法 ZIP 上传。
- 必须可观察：首次失败有可读错误且非成功态；重试成功后变为已关联。
- 验证手段：E2E。

#### AC-6: 作品页删除后两侧一致并可再传

- 触发：删除已关联 ZIP。
- 必须可观察：作品页无关联；上传入口恢复可用；媒体库该 ARCHIVE 消失；页面无「替换」文案。
- 验证手段：E2E。

#### AC-7: 无「从媒体库选择」且窄屏可用

- 触发：查看作品编辑页 ZIP 区域；视口宽度降至 375px。
- 必须可观察：无「从媒体库选择」控件；上传或删除主操作仍在视口可点击区域；`document.documentElement.scrollWidth` 不显著大于 `clientWidth`（允许 ≤8px 误差）。
- 验证手段：E2E 响应式断言。

#### AC-8: 未保存新作品不可上传

- 触发：进入「新建作品」且尚未保存成功的编辑态，查看 ZIP 区域。
- 必须可观察：上传控件不可用（disabled 或不渲染）；存在需先保存的明确提示文案。
- 验证手段：E2E DOM 断言。

### Done When

- 所有 Acceptance Criteria 都通过对应的验证手段完成自动化验证。
- 没有核心需求是通过直接状态修改、硬编码数据、占位行为或 fake integration 满足的。

### Advisory Smoke（不进入 Done When）

- 真实登录下人工点检一次上传与删除，作为发布前建议，不替代上述 AC。
