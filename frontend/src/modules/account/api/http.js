/**
 * http.js：账号模块的接口封装。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 复用 shared/http 的同源请求、会话与 CSRF 处理；对调用方返回业务数据，而不是整个 Axios 响应。
 */

/*
 * 历史导入路径。
 *
 * HTTP 客户端已提取到 src/shared/http.js（会话与 CSRF 是跨模块横切关注点）。
 * 这里保留再导出，使账户模块既有的 '../api/http' 导入继续可用；
 * 新模块请直接从 '../../../shared/http' 导入。
 */
export * from '../../../shared/http'
