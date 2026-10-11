/*
 * TutorialExerciseServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：TutorialMediaReferences、TutorialQuestionCardMapper、TutorialMapper、TutorialStateException、TutorialConflictException、ChapterQuestionDTO、KnowledgeCardDTO。
 * 阅读入口：cards（卡片集合处理入口）；createCard（创建卡片）；updateCard（更新卡片）；deleteCard（删除卡片）。
 */

package com.starrainnotes.tutorial.content.service.impl;

import org.springframework.transaction.annotation.Isolation;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.exception.TutorialConflictException;
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

/**
 * 教程Exercise业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class TutorialExerciseServiceImpl implements TutorialExerciseService {
    /**
     * 章节数据映射。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    private final TutorialChapterMapper chapterMapper;
    /**
     * 媒体References。
     */
    private final TutorialMediaReferences mediaReferences;
    /**
     * 教程数据映射。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorialMapper;
    /**
     * 卡片数据映射。
     * 协作类型：<code>TutorialKnowledgeCardMapper</code>。
     */
    private final TutorialKnowledgeCardMapper cardMapper;
    /**
     * 问题数据映射。
     * 协作类型：<code>TutorialQuestionMapper</code>。
     */
    private final TutorialQuestionMapper questionMapper;
    /**
     * 问题卡片数据映射。
     * 协作类型：<code>TutorialQuestionCardMapper</code>。
     */
    private final TutorialQuestionCardMapper questionCardMapper;

    /**
     * 刷新媒体。
     * 主要协作调用：<code>mediaReferences.replaceChapter</code>、<code>chapterMapper.selectById</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    private void refreshMedia(Long chapterId) {
        mediaReferences.replaceChapter(chapterId, chapterMapper.selectById(chapterId).getBodyMarkdown());
    }


    /**
     * edit章节处理入口。
     * 主要协作调用：<code>chapterMapper.selectById</code>、<code>tutorialMapper.byIdForUpdate</code>、<code>chapterMapper.bumpVersion</code>。
     * 实现中的显式失败类型包括 <code>TutorialChapterNotFoundException</code>、<code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void editChapter(Long id) {
        TutorialChapterEntity chapter = chapterMapper.selectById(id);
        if (chapter == null) throw new TutorialChapterNotFoundException();
        var tutorial = tutorialMapper.byIdForUpdate(chapter.getTutorialId());
        if (tutorial == null || "IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中或不存在，不能修改工作区");
        }
        chapterMapper.bumpVersion(id);
    }

    /**
     * 检查版本。
     * 关键处理步骤调用 <code>expected.equals</code>。
     * 实现中的显式失败类型包括 <code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param expected 测试期望结果；类型为 <code>Long</code>
     * @param actual 实际结果；类型为 <code>Long</code>
     */
    private void checkVersion(Long expected, Long actual) {
        if (expected == null || !expected.equals(actual)) {
            throw new TutorialConflictException("内容已被其他操作修改，请保留当前输入并重新加载后合并");
        }
    }

    /**
     * 检查所需章节。
     * 主要协作调用：<code>chapterMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialChapterNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void requireChapter(Long id) {
        if (chapterMapper.selectById(id) == null) throw new TutorialChapterNotFoundException();
    }

    /**
     * 文本处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>value.length</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @param label 标签；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String text(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.length() > limit) {
            throw new TutorialInvalidRequestException(label + "不能为空且长度不能超过 " + limit + " 字符");
        }
        return value;
    }

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束处理入口。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String status(String value) {
        if (value == null) return "ENABLED";
        if (!"ENABLED".equals(value) && !"DISABLED".equals(value)) {
            throw new TutorialInvalidRequestException("状态只能是 ENABLED 或 DISABLED");
        }
        return value;
    }

    /**
     * 卡片行集合处理入口。
     * 主要协作调用：<code>cardMapper.listByChapterId</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<TutorialKnowledgeCardEntity> cardRows(Long chapterId) {
        return cardMapper.listByChapterId(chapterId);
    }

    /**
     * 问题行集合处理入口。
     * 主要协作调用：<code>questionMapper.listByChapterId</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<TutorialQuestionEntity> questionRows(Long chapterId) {
        return questionMapper.listByChapterId(chapterId);
    }

    /**
     * 卡片View处理入口。
     * 关键处理步骤调用 <code>view.put</code>、<code>String.valueOf</code>、<code>row.getId</code>、<code>row.getChapterId</code>。
     *
     * @param row 行；类型为 <code>TutorialKnowledgeCardEntity</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    private Map<String, Object> cardView(TutorialKnowledgeCardEntity row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", String.valueOf(row.getId()));
        view.put("chapterId", String.valueOf(row.getChapterId()));
        view.put("rowVersion", row.getRowVersion());
        view.put("contentVersion", row.getContentVersion());
        view.put("frontText", row.getFrontText());
        view.put("backMarkdown", row.getBackMarkdown());
        view.put("sortOrder", row.getSortOrder());
        view.put("status", row.getStatus());
        return view;
    }

    /**
     * 问题View处理入口。
     * 主要协作调用：<code>questionCardMapper.cardIds</code>。
     *
     * @param row 行；类型为 <code>TutorialQuestionEntity</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    private Map<String, Object> questionView(TutorialQuestionEntity row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", String.valueOf(row.getId()));
        view.put("chapterId", String.valueOf(row.getChapterId()));
        view.put("rowVersion", row.getRowVersion());
        view.put("knowledgeCardIds", questionCardMapper.cardIds(row.getId()).stream().map(String::valueOf).toList());
        view.put("questionText", row.getQuestionText());
        view.put("referenceAnswer", row.getReferenceAnswer());
        view.put("sortOrder", row.getSortOrder());
        view.put("status", row.getStatus());
        return view;
    }

    /**
     * 保存Relations。
     * 主要协作调用：<code>questionCardMapper.deleteQuestion</code>、<code>questionCardMapper.insert</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param question 问题；类型为 <code>TutorialQuestionEntity</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
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

    /**
     * 检查所需顺序。
     * 关键处理步骤调用 <code>ids.size</code>、<code>expected.size</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param expected 测试期望结果；类型为 <code>List&lt;Long&gt;</code>
     */
    private void requireOrder(List<Long> ids, List<Long> expected) {
        if (ids == null || ids.size() != expected.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(expected))) {
            throw new TutorialInvalidRequestException("排序必须包含本章节的全部内容且不能重复");
        }
    }

    /**
     * 卡片集合处理入口。
     * 关键处理步骤调用 <code>requireChapter</code>、<code>cardRows</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<Map<String, Object>> cards(Long chapterId) {
        requireChapter(chapterId);
        return cardRows(chapterId).stream().map(this::cardView).toList();
    }

    /**
     * 创建卡片。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>cardMapper.insert</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> createCard(Long chapterId, KnowledgeCardDTO request) {
        editChapter(chapterId);
        TutorialKnowledgeCardEntity row = new TutorialKnowledgeCardEntity();
        row.setChapterId(chapterId);
        row.setFrontText(text(request.getFrontText(), "卡片正面", 5000));
        row.setBackMarkdown(text(request.getBackMarkdown(), "卡片背面", 100000));
        row.setStatus(status(request.getStatus()));
        row.setSortOrder(cardRows(chapterId).stream().mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()).max().orElse(0) + 10);
        cardMapper.insert(row);
        refreshMedia(chapterId);
        return cardView(row);
    }

    /**
     * 更新卡片。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>cardMapper.selectById</code>、<code>cardMapper.saveWorkspace</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>、<code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> updateCard(Long cardId, KnowledgeCardDTO request) {
        TutorialKnowledgeCardEntity row = cardMapper.selectById(cardId);
        if (row == null) throw new TutorialInvalidRequestException("知识卡片不存在");
        editChapter(row.getChapterId());
        row = cardMapper.selectById(cardId);
        checkVersion(request.getRowVersion(), row.getRowVersion());
        row.setFrontText(text(request.getFrontText(), "卡片正面", 5000));
        row.setBackMarkdown(text(request.getBackMarkdown(), "卡片背面", 100000));
        row.setStatus(status(request.getStatus()));
        row.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (cardMapper.saveWorkspace(row) != 1) {
            throw new TutorialConflictException("内容版本冲突，请重新加载后合并");
        }
        refreshMedia(row.getChapterId());
        return cardView(cardMapper.selectById(cardId));
    }

    /**
     * 删除卡片。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>cardMapper.selectById</code>、<code>questionCardMapper.deleteCard</code>、<code>cardMapper.deleteById</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteCard(Long cardId) {
        TutorialKnowledgeCardEntity row = cardMapper.selectById(cardId);
        if (row == null) throw new TutorialInvalidRequestException("知识卡片不存在");
        editChapter(row.getChapterId());
        questionCardMapper.deleteCard(cardId);
        cardMapper.deleteById(cardId);
        refreshMedia(row.getChapterId());
    }

    /**
     * 调整顺序卡片集合。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>cardMapper.updateSortOrder</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderCards(Long chapterId, List<Long> ids) {
        editChapter(chapterId);
        List<TutorialKnowledgeCardEntity> rows = cardRows(chapterId);
        requireOrder(ids, rows.stream().map(TutorialKnowledgeCardEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            Long id = ids.get(index);
            TutorialKnowledgeCardEntity row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            cardMapper.updateSortOrder(row.getId(), row.getSortOrder());
        }
    }

    /**
     * 问题集合处理入口。
     * 关键处理步骤调用 <code>requireChapter</code>、<code>questionRows</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<Map<String, Object>> questions(Long chapterId) {
        requireChapter(chapterId);
        return questionRows(chapterId).stream().map(this::questionView).toList();
    }

    /**
     * 创建问题。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>questionMapper.insert</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> createQuestion(Long chapterId, ChapterQuestionDTO request) {
        editChapter(chapterId);
        TutorialQuestionEntity row = new TutorialQuestionEntity();
        row.setChapterId(chapterId);
        row.setQuestionText(text(request.getQuestionText(), "题目", 5000));
        row.setReferenceAnswer(text(request.getReferenceAnswer(), "参考答案", 100000));
        row.setStatus(status(request.getStatus()));
        row.setSortOrder(questionRows(chapterId).stream().mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()).max().orElse(0) + 10);
        questionMapper.insert(row);
        saveRelations(row, request.getKnowledgeCardIds());
        refreshMedia(row.getChapterId());
        return questionView(row);
    }

    /**
     * 更新问题。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>questionMapper.selectById</code>、<code>questionMapper.saveWorkspace</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>、<code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> updateQuestion(Long questionId, ChapterQuestionDTO request) {
        TutorialQuestionEntity row = questionMapper.selectById(questionId);
        if (row == null) throw new TutorialInvalidRequestException("章节问题不存在");
        editChapter(row.getChapterId());
        row = questionMapper.selectById(questionId);
        checkVersion(request.getRowVersion(), row.getRowVersion());
        row.setQuestionText(text(request.getQuestionText(), "题目", 5000));
        row.setReferenceAnswer(text(request.getReferenceAnswer(), "参考答案", 100000));
        row.setStatus(status(request.getStatus()));
        row.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (questionMapper.saveWorkspace(row) != 1) {
            throw new TutorialConflictException("内容版本冲突，请重新加载后合并");
        }
        saveRelations(row, request.getKnowledgeCardIds());
        refreshMedia(row.getChapterId());
        return questionView(questionMapper.selectById(questionId));
    }

    /**
     * 删除问题。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>questionMapper.selectById</code>、<code>questionCardMapper.deleteQuestion</code>、<code>questionMapper.deleteById</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteQuestion(Long questionId) {
        TutorialQuestionEntity row = questionMapper.selectById(questionId);
        if (row == null) throw new TutorialInvalidRequestException("章节问题不存在");
        editChapter(row.getChapterId());
        questionCardMapper.deleteQuestion(questionId);
        questionMapper.deleteById(questionId);
        refreshMedia(row.getChapterId());
    }

    /**
     * 调整顺序问题集合。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>questionMapper.updateSortOrder</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderQuestions(Long chapterId, List<Long> ids) {
        editChapter(chapterId);
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
