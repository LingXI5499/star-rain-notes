package com.starrainnotes.english.vocabulary.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.domain.VocabularyLayer;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.english.vocabulary.entity.VocabularyTheme;
import com.starrainnotes.english.vocabulary.entity.VocabularyWord;
import com.starrainnotes.english.vocabulary.mapper.VocabularyThemeMapper;
import com.starrainnotes.english.vocabulary.mapper.VocabularyWordMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class VocabularyThemeRepository {
    private final VocabularyThemeMapper themes;
    private final VocabularyWordMapper words;

    public VocabularyThemeRepository(VocabularyThemeMapper themes,VocabularyWordMapper words) {
        this.themes=themes; this.words=words;
    }

    public void requireTheme(long id) {
        if (themes.selectById(id)==null) throw new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_THEME_NOT_FOUND",
                "Vocabulary theme not found","No vocabulary theme exists with id "+id+".");
    }

    public List<Long> idsForLayer(int layerOrder) {
        return themes.selectList(new QueryWrapper<VocabularyTheme>().eq("layer_order",layerOrder))
                .stream().map(VocabularyTheme::getId).toList();
    }

    public VocabularyThemeView create(VocabularyThemeRequest request) {
        VocabularyTheme theme=new VocabularyTheme();
        theme.setLayer(VocabularyLayer.fromOrder(request.layerOrder()).label());
        theme.setLayerOrder(request.layerOrder()); theme.setName(request.name().trim());
        theme.setSortOrder(request.sortOrder()==null?nextOrder():request.sortOrder()); themes.insert(theme);
        return new VocabularyThemeView(theme.getId(),theme.getName(),0);
    }

    public VocabularyThemeView update(long id,VocabularyThemeRequest request) {
        VocabularyTheme theme=themes.selectById(id); requireTheme(id);
        theme.setLayer(VocabularyLayer.fromOrder(request.layerOrder()).label());
        theme.setLayerOrder(request.layerOrder()); theme.setName(request.name().trim());
        if(request.sortOrder()!=null) theme.setSortOrder(request.sortOrder());
        themes.updateById(theme);
        long count=words.selectCount(new QueryWrapper<VocabularyWord>().eq("theme_id",id));
        return new VocabularyThemeView(theme.getId(),theme.getName(),count);
    }

    public long wordCount(long id) {
        Long count = words.selectCount(new QueryWrapper<VocabularyWord>().eq("theme_id", id));
        return count == null ? 0L : count;
    }

    public void delete(long id) {
        requireTheme(id);
        themes.deleteById(id);
    }

    private int nextOrder() {
        Integer value=themes.selectList(new QueryWrapper<VocabularyTheme>().orderByDesc("sort_order").last("LIMIT 1"))
                .stream().findFirst().map(VocabularyTheme::getSortOrder).orElse(0);
        return value+1;
    }
}
