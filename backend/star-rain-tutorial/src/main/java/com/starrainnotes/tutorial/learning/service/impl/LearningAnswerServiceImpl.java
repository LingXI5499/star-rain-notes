/*
 * LearningAnswerServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、LearningAnswerDTO、UserQuestionAnswerEntity、LearningAnswerLockedException、LearningInvalidRequestException、UserQuestionAnswerMapper、LearningAnswerService。
 * 阅读入口：answer（答案处理入口）；ownAnswer（own答案处理入口）；referenceAnswer（引用答案处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import com.starrainnotes.tutorial.learning.exception.LearningAnswerLockedException;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.mapper.UserQuestionAnswerMapper;
import com.starrainnotes.tutorial.learning.service.LearningAnswerService;
import com.starrainnotes.tutorial.learning.vo.LearningAnswerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学习答案业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Deprecated
// Legacy implementation retained for historical persistence tests; not registered as a Spring service.
@RequiredArgsConstructor
public class LearningAnswerServiceImpl implements LearningAnswerService {
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 内容。
     */
    private final LearningContentAccess content;
    /**
     * 答案数据映射。
     * 协作类型：<code>UserQuestionAnswerMapper</code>。
     */
    private final UserQuestionAnswerMapper answerMapper;
    /**
     * 事件集合。
     */
    private final LearningEventWriter events;

    /**
     * 行处理入口。
     * 主要协作调用：<code>answerMapper.selectByAccountIdAndQuestionId</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>UserQuestionAnswerEntity</code> 的处理结果
     */
    private UserQuestionAnswerEntity row(Long accountId, Long questionId) {
        return answerMapper.selectByAccountIdAndQuestionId(accountId, questionId);
    }

    /**
     * view处理入口。
     * 关键处理步骤调用 <code>LearningAnswerVO.builder</code>、<code>String.valueOf</code>、<code>row.getAnswerText</code>、<code>row.getFirstSubmittedAt</code>。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param row 行；类型为 <code>UserQuestionAnswerEntity</code>
     * @return 类型为 <code>LearningAnswerVO</code> 的处理结果
     */
    private LearningAnswerVO view(Long questionId, UserQuestionAnswerEntity row) {
        return LearningAnswerVO.builder().questionId(String.valueOf(questionId))
                .answerText(row == null ? "" : row.getAnswerText())
                .firstSubmittedAt(row == null ? null : row.getFirstSubmittedAt())
                .referenceUnlockedAt(row == null ? null : row.getReferenceUnlockedAt()).build();
    }

    /**
     * 答案处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.question</code>、<code>currentActorApi.current</code>、<code>answerMapper.upsertAnswer</code>、<code>events.chapter</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LearningAnswerDTO</code>
     * @return 类型为 <code>LearningAnswerVO</code> 的处理结果
     */
    @Override
    @Transactional
    public LearningAnswerVO answer(Long questionId, LearningAnswerDTO request) {
        LearningContentAccess.QuestionRef question = content.question(questionId);
        String answer = request == null || request.getAnswerText() == null ? "" : request.getAnswerText().strip();
        if (answer.isEmpty() || answer.length() > 20000) {
            throw new LearningInvalidRequestException("答案不能为空，且不能超过 20000 字符");
        }
        Long accountId = currentActorApi.current().getAccountId();
        answerMapper.upsertAnswer(accountId, questionId, answer);
        events.chapter(accountId, "QUESTION_ANSWERED", question.getChapter());
        return view(questionId, row(accountId, questionId));
    }

    /**
     * own答案处理入口。
     * 主要协作调用：<code>content.question</code>、<code>currentActorApi.current</code>。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningAnswerVO</code> 的处理结果
     */
    @Override
    public LearningAnswerVO ownAnswer(Long questionId) {
        content.question(questionId);
        return view(questionId, row(currentActorApi.current().getAccountId(), questionId));
    }

    /**
     * 引用答案处理入口。
     * 主要协作调用：<code>content.question</code>、<code>currentActorApi.current</code>。
     * 实现中的显式失败类型包括 <code>LearningAnswerLockedException</code>；具体触发条件见对应分支。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String referenceAnswer(Long questionId) {
        LearningContentAccess.QuestionRef question = content.question(questionId);
        Long accountId = currentActorApi.current().getAccountId();
        if (row(accountId, questionId) == null) {
            throw new LearningAnswerLockedException();
        }
        return question.getReferenceAnswer();
    }
}
