/*
 * EnglishOverviewServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：EnglishOverviewMapper、EnglishOverviewEntity、EnglishOverviewNotFoundException、EnglishOverviewVO、EnglishOverviewInvalidException、EnglishOverviewRequestDTO、EnglishOverviewService。
 * 阅读入口：get（读取英语Overview）；update（更新英语Overview）。
 */

package com.starrainnotes.english.overview.service.impl;

import com.starrainnotes.english.overview.mapper.EnglishOverviewMapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;

import com.starrainnotes.english.overview.exception.EnglishOverviewNotFoundException;

import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

import com.starrainnotes.english.overview.exception.EnglishOverviewInvalidException;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 英语Overview业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class EnglishOverviewServiceImpl implements EnglishOverviewService {
    /**
     * 数据映射。
     * 协作类型：<code>EnglishOverviewMapper</code>。
     */
    private final EnglishOverviewMapper mapper;

    /**
     * 读取英语Overview。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.selectSingleton</code>。
     * 实现中的显式失败类型包括 <code>EnglishOverviewNotFoundException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>EnglishOverviewVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public EnglishOverviewVO get() {
        EnglishOverviewEntity entity = mapper.selectSingleton();
        if (entity == null) throw new EnglishOverviewNotFoundException();
        return EnglishOverviewVO.from(entity);
    }

    /**
     * 更新英语Overview。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateContent</code>。
     * 实现中的显式失败类型包括 <code>EnglishOverviewInvalidException</code>、<code>EnglishOverviewNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EnglishOverviewRequestDTO</code>
     * @return 类型为 <code>EnglishOverviewVO</code> 的处理结果
     */
    @Override
    @Transactional
    public EnglishOverviewVO update(EnglishOverviewRequestDTO request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getTitle().length() > 200)
            throw new EnglishOverviewInvalidException("标题需在 1 到 200 字之间");
        checkLength(request.getSubtitle(), 500, "副标题");
        checkLength(request.getIntroduction(), 10000, "介绍");
        checkLength(request.getRoadmapMarkdown(), 200000, "路线图");
        String title = request.getTitle().trim();
        String subtitle = blankToNull(request.getSubtitle());
        String introduction = blankToNull(request.getIntroduction());
        String roadmap = blankToNull(request.getRoadmapMarkdown());
        if (mapper.updateContent(title, subtitle, introduction, roadmap) != 1)
            throw new EnglishOverviewNotFoundException();
        return get();
    }

    /**
     * 检查Length。
     * 关键处理步骤调用 <code>value.length</code>。
     * 实现中的显式失败类型包括 <code>EnglishOverviewInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @param label 标签；类型为 <code>String</code>
     */
    private void checkLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength)
            throw new EnglishOverviewInvalidException(label + "过长");
    }

    /**
     * blank目标空值处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
