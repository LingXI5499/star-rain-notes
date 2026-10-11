/*
 * CurrentAccountVO：账号模块的响应数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前账号响应对象，属于账号模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentAccountVO {

    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 用户名。
     */
    private String username;
    /**
     * 邮箱。
     */
    private String email;
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 角色集合。
     */
    private Set<String> roles;
    /**
     * 权限集合。
     */
    private Set<String> permissions;
    /**
     * 邮箱Verified。
     */
    private boolean emailVerified;
}
