package com.starrainnotes.english.vocabulary.mapper;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.vo.VocabularyWordAudioVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VocabularyMapper {
    List<Theme> themes();
    Theme theme(@Param("id") long id);
    long countWords(@Param("themeId") Long themeId, @Param("search") String search);
    List<Word> words(@Param("themeId") Long themeId, @Param("search") String search,
                     @Param("offset") long offset, @Param("limit") int limit);
    Word word(@Param("id") long id);
    List<Word> wordsByIds(@Param("ids") List<Long> ids);
    List<VocabularyWordAudioVO> audios(@Param("wordIds") List<Long> wordIds);
    int insertTheme(Theme theme);
    int updateTheme(Theme theme);
    int deleteTheme(@Param("id") long id);
    int insertWord(Word word);
    int updateWord(Word word);
    int deleteWord(@Param("id") long id);
}
