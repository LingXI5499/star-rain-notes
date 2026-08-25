package com.starrainnotes.english.shared.bundle.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.bundle.dto.BundleItemView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class LearningBundleItemService {
    private final JdbcTemplate jdbc;
    public LearningBundleItemService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<BundleItemView> list(Long bundleId,boolean publishedOnly){
        requireBundle(bundleId,publishedOnly);
        String status=publishedOnly?" AND a.publish_status='PUBLISHED'":"";
        List<BundleItemView> items=new ArrayList<>();
        items.addAll(jdbc.query("""
          SELECT 'READING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
          m.public_url cover_url,a.publish_status,x.sort_order
          FROM english_learning_bundle_reading_item x JOIN english_reading_article a ON a.id=x.article_id
          LEFT JOIN media_asset m ON m.id=a.cover_media_id WHERE x.bundle_id=?
          """+status,this::map,bundleId));
        status=publishedOnly?" AND a.publish_status='PUBLISHED'":"";
        items.addAll(jdbc.query("""
          SELECT 'LISTENING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
          m.public_url cover_url,a.publish_status,x.sort_order
          FROM english_learning_bundle_listening_item x JOIN english_listening_item a ON a.id=x.listening_item_id
          LEFT JOIN media_asset m ON m.id=a.cover_media_id WHERE x.bundle_id=?
          """+status,this::map,bundleId));
        status=publishedOnly?" AND a.publish_status='PUBLISHED'":"";
        items.addAll(jdbc.query("""
          SELECT 'WRITING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
          m.public_url cover_url,a.publish_status,x.sort_order
          FROM english_learning_bundle_writing_item x JOIN english_writing_prompt a ON a.id=x.prompt_id
          LEFT JOIN media_asset m ON m.id=a.cover_media_id WHERE x.bundle_id=?
          """+status,this::map,bundleId));
        items.sort(java.util.Comparator.comparingInt(BundleItemView::sortOrder)
                .thenComparing(BundleItemView::contentType).thenComparing(BundleItemView::contentId));
        return items;
    }

    @Transactional public BundleItemView add(Long bundleId,String rawType,Long contentId){
        requireBundle(bundleId,false);String type=type(rawType);requireContent(type,contentId);
        int order=nextOrder(bundleId);
        try{jdbc.update("INSERT INTO "+table(type)+"("+bundleColumn(type)+","+idColumn(type)+",sort_order) VALUES (?,?,?)",
                bundleId,contentId,order);}
        catch(DuplicateKeyException e){throw new ApiException(HttpStatus.CONFLICT,"ENGLISH_BUNDLE_ITEM_DUPLICATE","Item already exists","The content is already in this bundle.");}
        return list(bundleId,false).stream().filter(x->x.contentType().equals(type)&&x.contentId().equals(contentId)).findFirst().orElseThrow();
    }
    @Transactional public void remove(Long bundleId,String rawType,Long contentId){String type=type(rawType);requireBundle(bundleId,false);int changed=jdbc.update("DELETE FROM "+table(type)+" WHERE "+bundleColumn(type)+"=? AND "+idColumn(type)+"=?",bundleId,contentId);if(changed==0)notFound();}
    @Transactional public void move(Long bundleId,String rawType,Long contentId,int target){
        String type=type(rawType);requireBundle(bundleId,false);
        List<Key> keys=list(bundleId,false).stream().map(x->new Key(x.contentType(),x.contentId())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Key selected=new Key(type,contentId);if(!keys.remove(selected))notFound();keys.add(Math.min(Math.max(target,0),keys.size()),selected);
        for(int i=0;i<keys.size();i++){Key k=keys.get(i);jdbc.update("UPDATE "+table(k.type())+" SET sort_order=? WHERE "+bundleColumn(k.type())+"=? AND "+idColumn(k.type())+"=?",(i+1)*10,bundleId,k.id());}
    }
    public List<BundleItemView> publicList(String slug){
        Long id;
        try{id=jdbc.queryForObject("SELECT id FROM english_learning_bundle WHERE slug=? AND publish_status='PUBLISHED'",Long.class,slug);}
        catch(Exception e){throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED","Bundle unavailable","The learning bundle is not published.");}
        return list(id,true);
    }
    private void requireBundle(Long id,boolean published){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM english_learning_bundle WHERE id=?"+(published?" AND publish_status='PUBLISHED'":""),Integer.class,id);if(n==null||n==0)throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_BUNDLE_NOT_FOUND","Bundle not found","The learning bundle does not exist.");}
    private void requireContent(String type,Long id){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM "+contentTable(type)+" WHERE id=?",Integer.class,id);if(n==null||n==0)throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_BUNDLE_ITEM_INVALID","Invalid bundle item","The selected content does not exist.");}
    private int nextOrder(Long bundle){return list(bundle,false).stream().mapToInt(BundleItemView::sortOrder).max().orElse(0)+10;}
    private String type(String raw){String t=raw==null?"":raw.toUpperCase(Locale.ROOT);if(!List.of("READING","LISTENING","WRITING").contains(t))throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_BUNDLE_ITEM_INVALID","Invalid bundle item","Only reading, listening and writing content can be added.");return t;}
    private String table(String t){return switch(t){case"READING"->"english_learning_bundle_reading_item";case"LISTENING"->"english_learning_bundle_listening_item";default->"english_learning_bundle_writing_item";};}
    private String contentTable(String t){return switch(t){case"READING"->"english_reading_article";case"LISTENING"->"english_listening_item";default->"english_writing_prompt";};}
    private String bundleColumn(String t){return "bundle_id";}private String idColumn(String t){return switch(t){case"READING"->"article_id";case"LISTENING"->"listening_item_id";default->"prompt_id";};}
    private BundleItemView map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new BundleItemView(r.getString("content_type"),r.getLong("content_id"),r.getString("title"),r.getString("slug"),r.getString("summary"),r.getString("cefr_level"),r.getString("cover_url"),r.getString("publish_status"),r.getInt("sort_order"));}
    private void notFound(){throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_BUNDLE_ITEM_NOT_FOUND","Bundle item not found","The selected bundle item does not exist.");}
    private record Key(String type,Long id){}
}
