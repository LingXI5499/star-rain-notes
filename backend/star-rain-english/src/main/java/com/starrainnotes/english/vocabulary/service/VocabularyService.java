package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.ThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.WordRequest;
import java.util.List;

// 词库主题与单词的读写入口。
public interface VocabularyService {

    List<Theme> themes();

    Page<Word> words(String themeId, String search, int page, int size);

    Word word(String wordId);

    List<Word> wordsByIds(List<Long> wordIds, int maxWords);

    Theme createTheme(ThemeRequest request);

    Theme updateTheme(String themeId, ThemeRequest request);

    void deleteTheme(String themeId);

    Word createWord(WordRequest request);

    Word updateWord(String wordId, WordRequest request);

    void deleteWord(String wordId);
}
