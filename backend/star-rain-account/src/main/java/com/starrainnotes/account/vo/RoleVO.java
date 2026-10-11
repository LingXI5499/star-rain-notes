/*
 * RoleVO：账号模块的响应数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.account.vo;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色响应对象，属于账号模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 代码。
     */
    private String code;
    /**
     * 名称。
     */
    private String name;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 权限标识集合。
     */
    private Set<String> permissionIds;
}
