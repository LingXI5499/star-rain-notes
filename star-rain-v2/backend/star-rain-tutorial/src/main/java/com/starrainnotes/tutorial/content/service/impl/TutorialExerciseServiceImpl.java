package com.starrainnotes.tutorial.content.service.impl;

import com.starrainnotes.tutorial.content.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.content.dto.KnowledgeCardDTO;
import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity;
import com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity;
import com.starrainnotes.tutorial.content.exception.TutorialChapterNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialInvalidRequestException;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.service.TutorialExerciseService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialExerciseServiceImpl implements TutorialExerciseService {
    private final TutorialChapterMapper chapterMapper;
    private final TutorialKnowledgeCardMapper cardMapper;
    private final TutorialQuestionMapper questionMapper;
    private final com.starrainnotes.tutorial.content.mapper.TutorialQuestionCardMapper questionCardMapper;

    private void requireChapter(Long id) {
        if (chapterMapper.selectById(id) == null) throw new TutorialChapterNotFoundException();
    }

    private String text(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.length() > limit) {
            throw new TutorialInvalidRequestException(label + "不能为空且长度不能超过 " + limit + " 字符");
        }
        return value;
    }

    private String status(String value) {
        if (value == null) return "ENABLED";
        if (!"ENABLED".equals(value) && !"DISABLED".equals(value)) {
            throw new TutorialInvalidRequestException("状态只能是 ENABLED 或 DISABLED");
        }
        return value;
    }

    private List<TutorialKnowledgeCardEntity> cardRows(Long chapterId) {
        return cardMapper.listByChapterId(chapterId);
    }

    private List<TutorialQuestionEntity> questionRows(Long chapterId) {
        return questionMapper.listByChapterId(chapterId);
    }

    private Map<String, Object> cardView(TutorialKnowledgeCardEntity row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", String.valueOf(row.getId()));
        view.put("chapterId", String.valueOf(row.getChapterId()));
        view.put("contentVersion", row.getContentVersion());
        view.put("frontText", row.getFrontText());
        view.put("backMarkdown", row.getBackMarkdown());
        view.put("sortOrder", row.getSortOrder());
        view.put("status", row.getStatus());
        return view;
    }

    private Map<String, Object> questionView(TutorialQuestionEntity row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", String.valueOf(row.getId()));
        view.put("chapterId", String.valueOf(row.getChapterId()));
        view.put("knowledgeCardIds", questionCardMapper.cardIds(row.getId()).stream().map(String::valueOf).toList());
        view.put("questionText", row.getQuestionText());
        view.put("referenceAnswer", row.getReferenceAnswer());
        view.put("sortOrder", row.getSortOrder());
        view.put("status", row.getStatus());
        return view;
    }

    private void saveRelations(TutorialQuestionEntity question, List<Long> ids) {
        List<Long> selected = ids == null ? List.of() : ids;
        if (selected.size() > 1000 || selected.contains(null) || new HashSet<>(selected).size() != selected.size()) {
            throw new TutorialInvalidRequestException("关联卡片不能重复或为空");
        }
        java.util.Set<Long> available = new HashSet<>(cardRows(question.getChapterId()).stream().map(TutorialKnowledgeCardEntity::getId).toList());
        if (!available.containsAll(selected)) throw new TutorialInvalidRequestException("只能关联本章节的知识卡片");
        questionCardMapper.deleteQuestion(question.getId());
        for (int index=0;index<selected.size();index++) questionCardMapper.insert(question.getId(),selected.get(index),index);
    }

    private void requireOrder(List<Long> ids, List<Long> expected) {
        if (ids == null || ids.size() != expected.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(expected))) {
            throw new TutorialInvalidRequestException("排序必须包含本章节的全部内容且不能重复");
        }
    }

    @Override
    public List<Map<String, Object>> cards(Long chapterId) {
        requireChapter(chapterId);
        return cardRows(chapterId).stream().map(this::cardView).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createCard(Long chapterId, KnowledgeCardDTO request) {
        requireChapter(chapterId);
        TutorialKnowledgeCardEntity row = new TutorialKnowledgeCardEntity();
        row.setChapterId(chapterId);
        row.setFrontText(text(request.getFrontText(), "卡片正面", 5000));
        row.setBackMarkdown(text(request.getBackMarkdown(), "卡片背面", 100000));
        row.setStatus(status(request.getStatus()));
        row.setSortOrder(cardRows(chapterId).stream().mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()).max().orElse(0) + 10);
        cardMapper.insert(row);
        return cardView(row);
    }

    @Override
    @Transactional
    public Map<String, Object> updateCard(Long cardId, KnowledgeCardDTO request) {
        TutorialKnowledgeCardEntity row = cardMapper.selectById(cardId);
        if (row == null) throw new TutorialInvalidRequestException("知识卡片不存在");
        row.setFrontText(text(request.getFrontText(), "卡片正面", 5000));
        row.setBackMarkdown(text(request.getBackMarkdown(), "卡片背面", 100000));
        row.setStatus(status(request.getStatus()));
        row.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        cardMapper.updateContent(row.getId(), row.getFrontText(), row.getBackMarkdown(),
                row.getStatus(), row.getUpdatedAt());
        return cardView(cardMapper.selectById(cardId));
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        if (cardMapper.selectById(cardId) == null) throw new TutorialInvalidRequestException("知识卡片不存在");
        questionCardMapper.deleteCard(cardId);
        cardMapper.deleteById(cardId);
    }

    @Override
    @Transactional
    public void reorderCards(Long chapterId, List<Long> ids) {
        requireChapter(chapterId);
        List<TutorialKnowledgeCardEntity> rows = cardRows(chapterId);
        requireOrder(ids, rows.stream().map(TutorialKnowledgeCardEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            Long id = ids.get(index);
            TutorialKnowledgeCardEntity row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            cardMapper.updateSortOrder(row.getId(), row.getSortOrder());
        }
    }

    @Override
    public List<Map<String, Object>> questions(Long chapterId) {
        requireChapter(chapterId);
        return questionRows(chapterId).stream().map(this::questionView).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createQuestion(Long chapterId, ChapterQuestionDTO request) {
        requireChapter(chapterId);
        TutorialQuestionEntity row = new TutorialQuestionEntity();
        row.setChapterId(chapterId);
        row.setQuestionText(text(request.getQuestionText(), "题目", 5000));
        row.setReferenceAnswer(text(request.getReferenceAnswer(), "参考答案", 100000));
        row.setStatus(status(request.getStatus()));
        row.setSortOrder(questionRows(chapterId).stream().mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()).max().orElse(0) + 10);
        questionMapper.insert(row);
        saveRelations(row, request.getKnowledgeCardIds());
        return questionView(row);
    }

    @Override
    @Transactional
    public Map<String, Object> updateQuestion(Long questionId, ChapterQuestionDTO request) {
        TutorialQuestionEntity row = questionMapper.selectById(questionId);
        if (row == null) throw new TutorialInvalidRequestException("章节问题不存在");
        row.setQuestionText(text(request.getQuestionText(), "题目", 5000));
        row.setReferenceAnswer(text(request.getReferenceAnswer(), "参考答案", 100000));
        row.setStatus(status(request.getStatus()));
        row.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        questionMapper.updateContent(row.getId(), row.getQuestionText(), row.getReferenceAnswer(),
                row.getStatus(), row.getUpdatedAt());
        saveRelations(row, request.getKnowledgeCardIds());
        return questionView(row);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        if (questionMapper.selectById(questionId) == null) throw new TutorialInvalidRequestException("章节问题不存在");
        questionCardMapper.deleteQuestion(questionId);
        questionMapper.deleteById(questionId);
    }

    @Override
    @Transactional
    public void reorderQuestions(Long chapterId, List<Long> ids) {
        requireChapter(chapterId);
        List<TutorialQuestionEntity> rows = questionRows(chapterId);
        requireOrder(ids, rows.stream().map(TutorialQuestionEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            Long id = ids.get(index);
            TutorialQuestionEntity row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            questionMapper.updateSortOrder(row.getId(), row.getSortOrder());
        }
    }
}
