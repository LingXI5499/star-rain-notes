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
@Data
@TableName("sr_review_action")
public class ReviewActionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewRequestId;

    private String actionType;

    private Long actorAccountId;
    private String actorType;

    private String note;

    private LocalDateTime createdAt;
}
