-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-review 数据库迁移
-- 覆盖：REV-001 ~ REV-007
-- 数据库：MySQL 8.x / InnoDB / utf8mb4
-- 关系策略：逻辑外键，不建立任何物理 FOREIGN KEY
--
-- 关键设计：
-- 1. Review 只保存审核请求与审核决定，不拥有目标业务对象的状态。
-- 2. 每次重新提交创建新的 sr_review_request，终态请求不再复用。
-- 3. target_revision_ref 绑定提交时不可变审核版本。
-- 4. 同一 target_module + target_type + target_id + review_type 仅允许一个 PENDING。
-- 5. 并发 Approve/Reject/Cancel 只能有一个成功，靠条件 UPDATE 的 affected_rows 判定。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 审核请求
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_review_request (
    id                          BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核请求ID',

    review_type                 VARCHAR(100) NOT NULL
                                COMMENT '审核类型，如 tutorial.publish',

    target_module               VARCHAR(50) NOT NULL
                                COMMENT '目标模块，如 TUTORIAL',
    target_type                 VARCHAR(50) NOT NULL
                                COMMENT '目标对象类型，如 CHAPTER',
    target_id                   BIGINT NOT NULL
                                COMMENT '目标业务对象ID',

    target_revision_ref         VARCHAR(128) NOT NULL
                                COMMENT '业务模块提供的不可变版本/快照引用',

    target_display_name         VARCHAR(255) NOT NULL
                                COMMENT '提交时目标显示名称快照，用于审核列表',

    applicant_account_id        BIGINT NOT NULL
                                COMMENT '申请人账户ID，逻辑关联 sr_account.id',
    applicant_display_name      VARCHAR(100) NULL
                                COMMENT '提交时申请人显示名快照，可为空',

    submission_note             VARCHAR(1000) NULL
                                COMMENT '申请说明',

    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                COMMENT 'PENDING/APPROVED/REJECTED/CANCELED',

    -- 只保存决策人账户ID，不保存决策人显示名快照：
    -- Account 模块目前没有向其他模块暴露账户摘要的对外 API，
    -- 为了一个展示名去读 Account 的私表会破坏模块边界，因此决策人展示统一由
    -- 前端按 reviewer_account_id 呈现（例如「审核员 #12」），
    -- 待 Account 提供账户摘要 API 后再补展示名。
    reviewer_account_id         BIGINT NULL
                                COMMENT '最终决策人账户ID，逻辑关联 sr_account.id',

    decision_reason             VARCHAR(2000) NULL
                                COMMENT '拒绝原因或审核备注',

    submitted_at                DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    decided_at                  DATETIME(3) NULL,
    canceled_at                 DATETIME(3) NULL,

    created_at                  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    KEY idx_sr_review_status_submitted (
        status,
        submitted_at
    ),

    KEY idx_sr_review_target (
        target_module,
        target_type,
        target_id
    ),

    KEY idx_sr_review_target_type (
        target_module,
        target_type,
        target_id,
        review_type
    ),

    KEY idx_sr_review_applicant (
        applicant_account_id,
        submitted_at
    ),

    KEY idx_sr_review_reviewer (
        reviewer_account_id,
        decided_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='审核请求';

-- ---------------------------------------------------------------------
-- 2. 审核动作/历史
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_review_action (
    id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核历史ID',

    review_request_id       BIGINT NOT NULL COMMENT '逻辑关联 sr_review_request.id',

    action_type             VARCHAR(30) NOT NULL
                            COMMENT 'SUBMITTED/APPROVED/REJECTED/CANCELED',

    actor_account_id        BIGINT NULL
                            COMMENT '动作执行者；系统动作可为空',

    actor_type              VARCHAR(20) NOT NULL DEFAULT 'ACCOUNT'
                            COMMENT 'ACCOUNT/SYSTEM',

    note                    VARCHAR(2000) NULL
                            COMMENT '动作备注/拒绝原因/取消原因',

    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    KEY idx_sr_review_action_request (
        review_request_id,
        created_at
    ),

    KEY idx_sr_review_action_actor (
        actor_account_id,
        created_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='审核动作历史';

-- =====================================================================
-- Review 模块自身的业务权限
-- Review 只登记自己的权限语义，角色分配由 RBAC 关系完成。
-- =====================================================================
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('review:read',         'review', 'read',         '查看审核请求',   '查看待审核列表与审核详情',               'ENABLED'),
('review:history-read', 'review', 'history-read', '查询审核历史',   '按目标、状态、时间范围查询审核历史',     'ENABLED'),
('review:approve',      'review', 'approve',      '审核通过',       '对待审核请求作出通过决定',               'ENABLED'),
('review:reject',       'review', 'reject',       '审核拒绝',       '对待审核请求作出拒绝决定并要求填写原因', 'ENABLED')
ON DUPLICATE KEY UPDATE
    resource = VALUES(resource),
    action = VALUES(action),
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

-- 查看类权限按设计规范授予 ADMIN 与 SUPER_ADMIN。
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sr_role r
JOIN sr_permission p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN (
      'review:read',
      'review:history-read'
  );

-- 决策类权限当前只属于 SUPER_ADMIN：REV-004 / REV-005 的 Actor 就是 Super Admin。
-- USER 与普通 ADMIN 不取得 review:approve / review:reject。
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sr_role r
JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN (
      'review:approve',
      'review:reject'
  );

-- =====================================================================
-- 说明
-- 1. 不建立到 sr_account 的物理外键，申请人/决策人都是逻辑关联。
-- 2. MySQL 无条件唯一索引无法直接表达「同一 target + review_type 仅一个 PENDING」，
--    V2 由 Service 事务 + 存在性检查 + 按 target 维度的命名锁共同保证；
--    状态变更一律使用带 status='PENDING' 条件的 UPDATE，affected_rows 必须为 1。
-- 3. Review 不推进目标业务对象状态，审核结论通过 ReviewTargetHandler 在业务模块
--    提交事务之后回调，由业务模块自己推进。
-- =====================================================================
