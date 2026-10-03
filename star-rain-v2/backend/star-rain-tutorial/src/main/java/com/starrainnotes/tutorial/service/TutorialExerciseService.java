package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.dto.KnowledgeCardDTO;
import java.util.List;
import java.util.Map;

public interface TutorialExerciseService {
    List<Map<String, Object>> cards(Long chapterId);
    Map<String, Object> createCard(Long chapterId, KnowledgeCardDTO request);
    Map<String, Object> updateCard(Long cardId, KnowledgeCardDTO request);
    void deleteCard(Long cardId);
    void reorderCards(Long chapterId, List<Long> ids);
    List<Map<String, Object>> questions(Long chapterId);
    Map<String, Object> createQuestion(Long chapterId, ChapterQuestionDTO request);
    Map<String, Object> updateQuestion(Long questionId, ChapterQuestionDTO request);
    void deleteQuestion(Long questionId);
    void reorderQuestions(Long chapterId, List<Long> ids);
}
