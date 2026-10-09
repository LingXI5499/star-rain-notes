-- V2_008: 博客编辑权限授予管理员
--
-- 背景：V2_007 建表时把 blog:* 只授给了 SUPER_ADMIN。
-- 产品口径明确「管理员也可以编辑教程和博客」，因此把博客的读取、编辑、发布、
-- 撤回与分类管理一并授予 ADMIN。
--
-- 站点治理类权限（account:*）不在本迁移范围内，仍只属于 SUPER_ADMIN。
-- 前端对应地把 /admin/accounts、/admin/invitations、/admin/audits 的入口归属
-- 限定在管理站（见 frontend/src/shared/entryRoutes.js），并在路由 meta 上叠 superAdminOnly。
--
-- 逻辑外键，不建物理 FOREIGN KEY；INSERT IGNORE 依赖
-- uk_sr_role_permission(role_id, permission_id) 保证重复执行幂等。

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
    SELECT r.id, p.id
    FROM sr_role r
    JOIN sr_permission p
    WHERE r.code = 'ADMIN'
      AND p.code IN (
          'blog:read-admin',
          'blog:edit',
          'blog:publish',
          'blog:withdraw',
          'blog:taxonomy-manage'
      );
