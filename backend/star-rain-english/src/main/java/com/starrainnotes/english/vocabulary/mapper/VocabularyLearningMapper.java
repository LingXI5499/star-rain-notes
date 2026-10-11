/*
 * VocabularyLearningMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 阅读入口：ensureSlot（确保Slot）；lockSlot（锁Slot处理入口）；plan（计划处理入口）；defaultEntry（默认入口处理入口）。
 */

package com.starrainnotes.english.vocabulary.mapper;

import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** All predicates are account scoped; the persistent plan slot serializes account commands. */
@Mapper
public interface VocabularyLearningMapper {
    /**
     * 确保Slot。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     */
    void ensureSlot(@Param("accountId") long accountId);
    /**
     * 锁Slot处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    Plan lockSlot(@Param("accountId") long accountId);
    /**
     * 计划处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>、<code>sr_english_vocabulary_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    Plan plan(@Param("accountId") long accountId);
    /**
     * 默认入口处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>Entry</code> 的处理结果
     */
    Entry defaultEntry(@Param("accountId") long accountId);
    /**
     * groups处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param after 之后；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("after")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<GroupProgress> groups(@Param("accountId") long accountId, @Param("after") int after, @Param("limit") int limit);
    /**
     * 条目集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>、<code>sr_english_vocabulary_word</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param groupNo 分组不含；类型为 <code>Integer</code>；对应 MyBatis 命名参数 <code>@Param("groupNo")</code>
     * @param after 之后；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("after")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<PlanItem> items(@Param("accountId") long accountId, @Param("groupNo") Integer groupNo,
                         @Param("after") int after, @Param("limit") int limit);
    /**
     * item处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 类型为 <code>PlanItem</code> 的处理结果
     */
    PlanItem item(@Param("accountId") long accountId, @Param("wordId") long wordId);
    /**
     * 清理条目集合。
     *
     * 持久化操作：删除，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     */
    void clearItems(@Param("accountId") long accountId);
    /**
     * 替换计划。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param plan 计划；类型为 <code>Plan</code>；对应 MyBatis 命名参数 <code>@Param("plan")</code>
     */
    void replacePlan(@Param("accountId") long accountId, @Param("plan") Plan plan);
    /**
     * 写入条目集合。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param items 条目集合；类型为 <code>List&lt;PlanItem&gt;</code>；对应 MyBatis 命名参数 <code>@Param("items")</code>
     */
    void insertItems(@Param("accountId") long accountId, @Param("items") List<PlanItem> items);
    /**
     * markDone处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param bit bit；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("bit")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void markDone(@Param("accountId") long accountId, @Param("wordId") long wordId,
                  @Param("bit") int bit, @Param("now") LocalDateTime now);
    /**
     * 更新计划状态。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan</code>、<code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void updatePlanStatus(@Param("accountId") long accountId, @Param("now") LocalDateTime now);
    /**
     * 取消计划。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void cancelPlan(@Param("accountId") long accountId, @Param("now") LocalDateTime now);
    /**
     * 最近Started分组处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    int lastStartedGroup(@Param("accountId") long accountId);
    /**
     * 首次剩余排序处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param frozen frozen；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("frozen")</code>
     * @return 类型为 <code>Integer</code> 的处理结果
     */
    Integer firstRemainingSort(@Param("accountId") long accountId, @Param("frozen") int frozen);
    /**
     * regroup处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param frozen frozen；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("frozen")</code>
     * @param firstSort 首次排序；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("firstSort")</code>
     * @param batchSize 批量大小；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("batchSize")</code>
     */
    void regroup(@Param("accountId") long accountId, @Param("frozen") int frozen,
                 @Param("firstSort") int firstSort, @Param("batchSize") int batchSize);
    /**
     * resize处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param batchSize 批量大小；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("batchSize")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void resize(@Param("accountId") long accountId, @Param("batchSize") int batchSize, @Param("now") LocalDateTime now);
    /**
     * 统计Words。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param filter 筛选；类型为 <code>Filter</code>；对应 MyBatis 命名参数 <code>@Param("f")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countWords(@Param("accountId") long accountId, @Param("f") Filter filter);
    /**
     * 单词分页处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param filter 筛选；类型为 <code>Filter</code>；对应 MyBatis 命名参数 <code>@Param("f")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> wordPage(@Param("accountId") long accountId, @Param("f") Filter filter,
                        @Param("offset") long offset, @Param("limit") int limit);
    /**
     * 选中处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param selection selection；类型为 <code>Selection</code>；对应 MyBatis 命名参数 <code>@Param("s")</code>
     * @param after 之后；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("after")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<SelectionRow> selected(@Param("accountId") long accountId, @Param("s") Selection selection,
                                @Param("after") long after, @Param("limit") int limit);
    /**
     * 已学习数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long learnedCount(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    /**
     * states处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_plan_item</code>、<code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     * @return 本次处理得到的条目集合
     */
    List<State> states(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    /**
     * modes处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_mode_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     * @return 本次处理得到的条目集合
     */
    List<ModeMemory> modes(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    /**
     * 锁记忆处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 类型为 <code>State</code> 的处理结果
     */
    State lockMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);
    /**
     * initialize记忆处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void initializeMemory(@Param("accountId") long accountId, @Param("wordId") long wordId, @Param("now") LocalDateTime now);
    /**
     * 保存Mode。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_mode_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param mode mode；类型为 <code>ModeMemory</code>；对应 MyBatis 命名参数 <code>@Param("mode")</code>
     */
    void saveMode(@Param("accountId") long accountId, @Param("mode") ModeMemory mode);
    /**
     * 添加日。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_rating_day</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param day 日；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("day")</code>
     * @param direction direction；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("direction")</code>
     */
    void addDay(@Param("accountId") long accountId, @Param("wordId") long wordId,
                @Param("day") LocalDate day, @Param("direction") String direction);
    /**
     * days处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_rating_day</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param start 开始；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @return 符合 SQL 条件的记录数量
     */
    DayCounts days(@Param("accountId") long accountId, @Param("wordId") long wordId,
                    @Param("start") LocalDate start, @Param("end") LocalDate end);
    /**
     * 保存掌握。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_memory</code>、<code>sr_english_vocabulary_mode_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param score score；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("score")</code>
     * @param rank rank；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("rank")</code>
     * @param rating 评分；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("rating")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     */
    void saveMastery(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("score") int score, @Param("rank") String rank,
                     @Param("rating") String rating, @Param("now") LocalDateTime now);
    /**
     * exitConsolidation处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_mode_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param cap cap；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("cap")</code>
     */
    void exitConsolidation(@Param("accountId") long accountId, @Param("wordId") long wordId, @Param("cap") LocalDateTime cap);
    /**
     * 当前类使用的日志记录器处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @return 类型为 <code>RatingLog</code> 的处理结果
     */
    RatingLog log(@Param("accountId") long accountId, @Param("sessionId") String sessionId);
    /**
     * 写入Log。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param log 当前类使用的日志记录器；类型为 <code>RatingLog</code>；对应 MyBatis 命名参数 <code>@Param("log")</code>
     */
    void insertLog(@Param("accountId") long accountId, @Param("log") RatingLog log);
    /**
     * 保存Log结果。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @param json JSON 读写或映射协作对象；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("json")</code>
     */
    void saveLogResult(@Param("accountId") long accountId, @Param("sessionId") String sessionId, @Param("json") String json);
    /**
     * due处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_mode_memory</code>、<code>sr_english_vocabulary_word</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cursor cursor；类型为 <code>DueCursor</code>；对应 MyBatis 命名参数 <code>@Param("cursor")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<ModeMemory> due(@Param("accountId") long accountId, @Param("cursor") DueCursor cursor, @Param("limit") int limit);
    /**
     * 审核摘要处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>、<code>sr_english_vocabulary_mode_memory</code>、<code>sr_english_vocabulary_word</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @param today today；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 符合 SQL 条件的记录数量
     */
    ReviewSummary reviewSummary(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                                @Param("today") LocalDateTime today);
}
