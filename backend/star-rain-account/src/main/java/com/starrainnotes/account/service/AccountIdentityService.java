/*
 * AccountIdentityService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：AccountReferenceApi、CurrentActorApi、PermissionQueryApi、AccountPrincipal、CurrentAccountVO。
 * 阅读入口：principal（认证主体处理入口）；currentView（当前View处理入口）；view（view处理入口）。
 */

package com.starrainnotes.account.service;

import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.vo.CurrentAccountVO;

// 账户身份入口：当前登录主体解析与账户视图读取。
/**
 * 账号身份业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AccountIdentityService extends CurrentActorApi, AccountReferenceApi, PermissionQueryApi {

    /**
     * 认证主体处理入口。
     * @return 类型为 <code>AccountPrincipal</code> 的处理结果
     */
    AccountPrincipal principal();

    /**
     * 当前View处理入口。
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO currentView();

    /**
     * view处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO view(long accountId);
}
