package com.starrainnotes.english.knowledge.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
import com.starrainnotes.english.knowledge.mapper.KnowledgeMapper;
import com.starrainnotes.english.knowledge.service.KnowledgeService;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeMapper mapper; private final TaxonomyService taxonomy;
    @Override @Transactional(readOnly=true) public Page list(String type,String search,Long topic,Long genre,Long purpose,int page,int size) {
        if(type!=null && !type.isBlank() && !List.of("READING","WRITING").contains(type)) throw new ApiException("ENGLISH_TYPE_INVALID","内容类型无效",400);
        String term=search==null?"":search.trim();EnglishBodyValidator.length(term,100);int p=Math.max(1,page),s=Math.max(1,Math.min(50,size));
        var topics=taxonomy.descendants(topic,"TOPIC");var genres=taxonomy.descendants(genre,"GENRE");var purposes=taxonomy.descendants(purpose,"PURPOSE");
        return new Page(mapper.list(type,term,topics,genres,purposes,(long)(p-1)*s,s),mapper.count(type,term,topics,genres,purposes),p,s);
    }
}
