# 媒体库 ARCHIVE × 作品静态原型 ZIP Spec

## Goal

作品编辑页上传的静态原型 ZIP 成为媒体库可管理的 ARCHIVE 资源；作品与 ZIP 一对一绑定；媒体库负责记录、展示、存储与删除；在线访问规则保持不变。

## Global Constraints

- 工作目录固定为 `E:\star-rain-ui-upgrade`，分支 `codex/ui-performance-upgrade-v1`；不覆盖 `E:\star-rain` 中用户无关改动。
- 不做 Markdown / PDF / Word 导出。
- 不做版本保留、改绑、「从媒体库选择」、作品页「仅解除关联」。
- ZIP 上传入口仅在作品编辑页；媒体库对 ZIP 只做记录、展示、存储、删除。
- 一个作品最多绑定一个 ZIP；一个 ZIP 最多绑定一个作品。
- 在线访问优先级：真实 `demoUrl` → 已发布静态原型 → 暂未提供。
- 草稿仅后台预览；公开使用版本化 URL、sandbox iframe 与安全响应头。
- 已应用的 Flyway V30 / V31 不得改写内容；只追加新迁移。旧独立原型数据清空，要求重新上传。
- 不擅自提交、合并 main、推送或部署。
- 核心行为必须通过公共入口（HTTP API + 管理端 UI）验证；mock / stub / no-op / 直接改库不得满足验收。测试 setup 经公共入口预置数据除外，且不得把直接改库当作成功证据。
- 浏览器验收必须包含真实登录会话；编译、类型检查、构建通过不等于功能完成。
- 安全解压与一 ZIP 一作品由 slice 2 落地；父级要求拒绝软链接与路径越界，二者均须有自动化覆盖。

## Split Specs

### 媒体库 ARCHIVE

- [ ] Implementation status: Not done
- Spec: [2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-1-media-archive-spec.md)
- Scope: `media_asset` 增加 ARCHIVE；列表/统计/删除；通用上传拒绝 ZIP；删除已绑定 ZIP 时自动清作品关联。
- Acceptance summary: 压缩包可作为媒体资产被列表与删除；通用上传接口拒 ZIP；删除联动作品关联。

### 作品 ZIP 闭环

- [ ] Implementation status: Not done
- Spec: [2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-2-portfolio-zip-loop-spec.md)
- Scope: 作品页上传写入 ARCHIVE 并绑定、校验解压、预览/发布；作品页删除与删作品连带删 ZIP；迁移清空旧原型；安全解压。
- Acceptance summary: 上传/删除/发布/失败回滚全链路可用；旧数据清空后需重传。

### 媒体库与作品页 UI

- [ ] Implementation status: Not done
- Spec: [2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md](./2026-09-15-media-archive-portfolio-zip-slice-3-admin-ui-spec.md)
- Scope: 媒体库侧栏分类 + 内容区；作品页上传/删除 ZIP 与状态展示；Inkspace 样式修复。
- Acceptance summary: 两侧 UI 可完成上传、展示、删除，样式与响应式达标。

## Candidate Future Split Specs

- 拖拽上传：本轮非必达；若实现成本低可作为后续增强。
- 公开端在线访问体验打磨：现有 `/portfolio/:slug/live` 与优先级已定；仅当公开页出现独立缺陷时再拆。
