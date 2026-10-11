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
    void ensureSlot(@Param("accountId") long accountId);
    Plan lockSlot(@Param("accountId") long accountId);
    Plan plan(@Param("accountId") long accountId);
    Entry defaultEntry(@Param("accountId") long accountId);
    List<GroupProgress> groups(@Param("accountId") long accountId, @Param("after") int after, @Param("limit") int limit);
    List<PlanItem> items(@Param("accountId") long accountId, @Param("groupNo") Integer groupNo,
                         @Param("after") int after, @Param("limit") int limit);
    PlanItem item(@Param("accountId") long accountId, @Param("wordId") long wordId);
    void clearItems(@Param("accountId") long accountId);
    void replacePlan(@Param("accountId") long accountId, @Param("plan") Plan plan);
    void insertItems(@Param("accountId") long accountId, @Param("items") List<PlanItem> items);
    void markDone(@Param("accountId") long accountId, @Param("wordId") long wordId,
                  @Param("bit") int bit, @Param("now") LocalDateTime now);
    void updatePlanStatus(@Param("accountId") long accountId, @Param("now") LocalDateTime now);
    void cancelPlan(@Param("accountId") long accountId, @Param("now") LocalDateTime now);
    int lastStartedGroup(@Param("accountId") long accountId);
    Integer firstRemainingSort(@Param("accountId") long accountId, @Param("frozen") int frozen);
    void regroup(@Param("accountId") long accountId, @Param("frozen") int frozen,
                 @Param("firstSort") int firstSort, @Param("batchSize") int batchSize);
    void resize(@Param("accountId") long accountId, @Param("batchSize") int batchSize, @Param("now") LocalDateTime now);
    long countWords(@Param("accountId") long accountId, @Param("f") Filter filter);
    List<Long> wordPage(@Param("accountId") long accountId, @Param("f") Filter filter,
                        @Param("offset") long offset, @Param("limit") int limit);
    List<SelectionRow> selected(@Param("accountId") long accountId, @Param("s") Selection selection,
                                @Param("after") long after, @Param("limit") int limit);
    long learnedCount(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    List<State> states(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    List<ModeMemory> modes(@Param("accountId") long accountId, @Param("ids") List<Long> ids);
    State lockMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);
    void initializeMemory(@Param("accountId") long accountId, @Param("wordId") long wordId, @Param("now") LocalDateTime now);
    void saveMode(@Param("accountId") long accountId, @Param("mode") ModeMemory mode);
    void addDay(@Param("accountId") long accountId, @Param("wordId") long wordId,
                @Param("day") LocalDate day, @Param("direction") String direction);
    DayCounts days(@Param("accountId") long accountId, @Param("wordId") long wordId,
                    @Param("start") LocalDate start, @Param("end") LocalDate end);
    void saveMastery(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("score") int score, @Param("rank") String rank,
                     @Param("rating") String rating, @Param("now") LocalDateTime now);
    void exitConsolidation(@Param("accountId") long accountId, @Param("wordId") long wordId, @Param("cap") LocalDateTime cap);
    RatingLog log(@Param("accountId") long accountId, @Param("sessionId") String sessionId);
    void insertLog(@Param("accountId") long accountId, @Param("log") RatingLog log);
    void saveLogResult(@Param("accountId") long accountId, @Param("sessionId") String sessionId, @Param("json") String json);
    List<ModeMemory> due(@Param("accountId") long accountId, @Param("cursor") DueCursor cursor, @Param("limit") int limit);
    ReviewSummary reviewSummary(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                                @Param("today") LocalDateTime today);
}
