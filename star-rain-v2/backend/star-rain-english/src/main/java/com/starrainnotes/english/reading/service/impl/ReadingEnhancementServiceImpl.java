package com.starrainnotes.english.reading.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.mapper.*;
import com.starrainnotes.english.reading.service.*;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class ReadingEnhancementServiceImpl implements ReadingEnhancementService {
    private final ReadingService reading;
    private final ReadingMapper articles;
    private final ReadingEnhancementMapper mapper;
    @Override @Transactional(readOnly=true) public List<Item> list(String identity,boolean admin,String kind) {
        kind(kind);Article article=reading.get(identity,admin);List<Item> rows=mapper.list(kind,article.getId());
        if(admin || "vocabulary".equals(kind)) return rows;
        List<Item> active=rows.stream().filter(i->"ACTIVE".equals(i.getStatus()) && EnglishBodyValidator.hash("ZH".equals(i.getLanguage())?article.getTranslationZhMarkdown():article.getBodyMarkdown()).equals(i.getSourceMarkdownHash())).toList();
        if("alignments".equals(kind)) {
            Set<String> valid=new HashSet<>();Map<String,Set<String>> languages=new HashMap<>();
            active.forEach(i->languages.computeIfAbsent(i.getGroupKey(),k->new HashSet<>()).add(i.getLanguage()));
            languages.forEach((k,v)->{if(v.containsAll(List.of("EN","ZH"))) valid.add(k);});
            return active.stream().filter(i->valid.contains(i.getGroupKey())).toList();
        }
        return active;
    }
    @Override @Transactional public Result replace(String id,String kind,Batch batch) {
        kind(kind);Article article=lock(id,batch.getRowVersion());List<Item> items=batch.getItems();
        if(items==null || items.size()>1000) invalid("条目数量超出限制");
        for(Item i:items) validate(article,kind,i);
        if("alignments".equals(kind)) groups(items);
        mapper.touch(article.getId(),article.getRowVersion());mapper.clear(kind,article.getId());
        for(Item i:items) mapper.insert(kind,article.getId(),i);
        return result(article,kind);
    }
    @Override @Transactional public Result save(String id,String kind,Long itemId,Item item) {
        if("alignments".equals(kind)) invalid("对齐请按组整体保存");kind(kind);Article article=lock(id,item.getRowVersion());
        if(itemId!=null && mapper.item(kind,article.getId(),itemId)==null) missing();
        validate(article,kind,item);item.setId(itemId);mapper.touch(article.getId(),article.getRowVersion());
        if(itemId==null) mapper.insert(kind,article.getId(),item);else mapper.update(kind,article.getId(),item);
        return result(article,kind);
    }
    @Override @Transactional public Result delete(String id,String kind,long itemId,long version) {
        if("alignments".equals(kind)) invalid("对齐请按组整体保存");kind(kind);Article article=lock(id,version);
        if(mapper.item(kind,article.getId(),itemId)==null) missing();
        mapper.touch(article.getId(),article.getRowVersion());mapper.delete(kind,article.getId(),itemId);return result(article,kind);
    }
    @Override public void restore(long id,Snapshot snapshot) {
        for(String kind:List.of("alignments","annotations","vocabulary")) {
            mapper.clear(kind,id); List<Item> items=switch(kind) {case "alignments"->snapshot.alignments();case "annotations"->snapshot.annotations();default->snapshot.vocabulary();};
            for(Item item:items) { item.setId(null); mapper.insert(kind,id,item); }
        }
    }
    private Article lock(String id,Long version) {
        Article a=articles.lockById(ReadingServiceImpl.id(id));if(a==null) missing();
        if(version==null || !version.equals(a.getRowVersion())) throw new ApiException("READING_VERSION_CONFLICT","内容已变化，请刷新后再编辑",409);return a;
    }
    private Result result(Article a,String kind) { return new Result(a.getRowVersion()+1,mapper.list(kind,a.getId())); }
    private static void kind(String kind) { if(!List.of("alignments","annotations","vocabulary").contains(kind)) invalid("不支持的增强类型"); }
    private static void validate(Article a,String kind,Item i) {
        if(i==null) invalid("条目不能为空");i.setSortOrder(i.getSortOrder()==null?0:i.getSortOrder());
        if("vocabulary".equals(kind)) {
            if(i.getWord()==null || i.getWord().isBlank() || i.getMeaningZh()==null || i.getMeaningZh().isBlank()) invalid("请填写单词与本文释义");
            if(!List.of("NOUN","VERB","ADJECTIVE","ADVERB","PREPOSITION","PRONOUN","CONJUNCTION","PHRASE","OTHER").contains(i.getPartOfSpeech()==null?"":i.getPartOfSpeech())) invalid("词性无效");
            EnglishBodyValidator.length(i.getWord(),200);EnglishBodyValidator.length(i.getMeaningZh(),500);
            i.setWord(i.getWord().trim());i.setMeaningZh(i.getMeaningZh().trim());return;
        }
        if("annotations".equals(kind)) { i.setLanguage("EN"); if(i.getAnalysisMarkdown()==null || i.getAnalysisMarkdown().isBlank()) invalid("请填写人工解析");EnglishBodyValidator.length(i.getAnalysisMarkdown(),200000); }
        else { if(!List.of("EN","ZH").contains(i.getLanguage()==null?"":i.getLanguage()) || i.getGroupKey()==null || !i.getGroupKey().matches("[A-Za-z0-9_-]{1,64}")) invalid("对齐组或语言无效"); }
        EnglishBodyValidator.length(i.getExpectedText(),10000);
        if(i.getParagraphIndex()==null || i.getParagraphIndex()<0 || i.getRangeStart()==null || i.getRangeStart()<0 || i.getRangeEnd()==null || i.getRangeEnd()>200000 || i.getRangeEnd()<=i.getRangeStart() || i.getExpectedText()==null || i.getExpectedText().isBlank() || i.getExpectedText().length()!=i.getRangeEnd()-i.getRangeStart()) invalid("选区无效，请重新选择");
        if(i.getParagraphHash()==null || !i.getParagraphHash().matches("[a-f0-9]{64}") || !EnglishBodyValidator.hash("ZH".equals(i.getLanguage())?a.getTranslationZhMarkdown():a.getBodyMarkdown()).equals(i.getSourceMarkdownHash())) invalid("正文已变化，请重新选择");
        i.setStatus("ACTIVE");
    }
    private static void groups(List<Item> items) {
        Map<String,Set<String>> groups=new HashMap<>();items.forEach(i->groups.computeIfAbsent(i.getGroupKey(),k->new HashSet<>()).add(i.getLanguage()));
        if(groups.values().stream().anyMatch(v->!v.containsAll(List.of("EN","ZH")))) invalid("每个对齐组至少包含一段英文和一段中文");
    }
    private static void invalid(String text) { throw new ApiException("READING_ANCHOR_INVALID",text,400); }
    private static void missing() { throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND","文章或条目不存在",404); }
}
