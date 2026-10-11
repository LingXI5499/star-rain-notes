/*
 * 历史导入路径。
 *
 * HTTP 客户端已提取到 src/shared/http.js（会话与 CSRF 是跨模块横切关注点）。
 * 这里保留再导出，使账户模块既有的 '../api/http' 导入继续可用；
 * 新模块请直接从 '../../../shared/http' 导入。
 */
export * from '../../../shared/http'
