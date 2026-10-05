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

@Service
@RequiredArgsConstructor
public class LearningAnswerServiceImpl implements LearningAnswerService {
    private final CurrentActorApi currentActorApi;
    private final LearningContentAccess content;
    private final UserQuestionAnswerMapper answerMapper;
    private final LearningEventWriter events;

    private UserQuestionAnswerEntity row(Long accountId, Long questionId) {
        return answerMapper.selectByAccountIdAndQuestionId(accountId, questionId);
    }

    private LearningAnswerVO view(Long questionId, UserQuestionAnswerEntity row) {
        return LearningAnswerVO.builder().questionId(String.valueOf(questionId))
                .answerText(row == null ? "" : row.getAnswerText())
                .firstSubmittedAt(row == null ? null : row.getFirstSubmittedAt())
                .referenceUnlockedAt(row == null ? null : row.getReferenceUnlockedAt()).build();
    }

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

    @Override
    public LearningAnswerVO ownAnswer(Long questionId) {
        content.question(questionId);
        return view(questionId, row(currentActorApi.current().getAccountId(), questionId));
    }

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
