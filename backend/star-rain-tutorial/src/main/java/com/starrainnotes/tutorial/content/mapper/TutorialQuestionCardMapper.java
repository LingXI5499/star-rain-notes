package com.starrainnotes.tutorial.content.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper public interface TutorialQuestionCardMapper {
 List<Long> cardIds(@Param("questionId") Long questionId);
 int deleteQuestion(@Param("questionId") Long questionId);
 int deleteCard(@Param("cardId") Long cardId);
 int insert(@Param("questionId") Long questionId,@Param("cardId") Long cardId,@Param("sortOrder") int sortOrder);
}
