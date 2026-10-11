/*
 * ReviewActionEntity：审核模块的持久化实体文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 审核动作历史。
 *
 * 只追加不修改：即使审核请求后来被拒绝或取消，走过的路径也必须完整可追溯。
 * actor_account_id 允许为空，用于系统自动取消这类没有自然人的动作。
 */
/**
 * 审核动作实体，属于审核模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_review_action")
public class ReviewActionEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 审核请求标识。
     */
    private Long reviewRequestId;

    /**
     * 动作类型。
     */
    private String actionType;

    /**
     * 操作者账号标识。
     */
    private Long actorAccountId;
    /**
     * 操作者类型。
     */
    private String actorType;

    /**
     * note。
     */
    private String note;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
