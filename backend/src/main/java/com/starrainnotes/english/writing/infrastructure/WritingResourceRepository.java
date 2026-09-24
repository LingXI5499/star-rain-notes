package com.starrainnotes.english.writing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.writing.dto.*;
import com.starrainnotes.english.writing.domain.WritingResourcePolicy;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.english.writing.entity.WritingResource;
import com.starrainnotes.english.writing.mapper.WritingResourceMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 独立写作资源域；所有可发布性校验在服务端完成。 */
@Repository
@Transactional(readOnly = true)
public class WritingResourceRepository {
    private static final Set<String> KINDS=Set.of("EXPRESSION_LESSON","GENRE_LESSON","MODEL_ESSAY","TEMPLATE");
    private static final Set<String> LEVELS=Set.of("SENTENCE","PARAGRAPH","COHESION","STYLE");
    private static final DateTimeFormatter ISO=DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String BASE="SELECT r.* FROM english_writing_resource r ";
    private final WritingResourceMapper mapper; private final JdbcTemplate jdbc; private final SiteSettingsTimezone timezone; private final MediaAssetPort mediaAssets; private final WritingResourcePolicy policy;
    public WritingResourceRepository(WritingResourceMapper mapper,JdbcTemplate jdbc,SiteSettingsTimezone timezone,MediaAssetPort mediaAssets,WritingResourcePolicy policy){this.mapper=mapper;this.jdbc=jdbc;this.timezone=timezone;this.mediaAssets=mediaAssets;this.policy=policy;}

    @Transactional public WritingResourceView create(WritingResourceRequest r){
        validateRequest(r); String slug=NumericSlugGenerator.forCreate(r.slug(),this::slugExists); WritingResource e=new WritingResource(); apply(e,r,slug); e.setPublishStatus("DRAFT"); e.setSortOrder(r.sortOrder()==null?nextOrder():r.sortOrder());
        try{mapper.insert(e);}catch(DuplicateKeyException x){throw conflict();} replaceTags(e.getId(),r.tagIds()); return get(e.getId());
    }
    @Transactional public WritingResourceView update(Long id,WritingResourceRequest r){WritingResource e=require(id);validateRequest(r);String slug=NumericSlugGenerator.forUpdate(r.slug(),e.getSlug());assertSlug(slug,id);apply(e,r,slug);try{mapper.updateById(e);}catch(DuplicateKeyException x){throw conflict();}replaceTags(id,r.tagIds());return get(id);}
    public WritingResourceView get(Long id){WritingResource e=require(id);return jdbc.queryForObject(BASE+" WHERE r.id=?",this::mapView,id);}
    public WritingResourceView publicGet(String slug){try{return jdbc.queryForObject(BASE+" WHERE r.slug=? AND r.publish_status='PUBLISHED'",this::mapView,slug);}catch(Exception x){throw unavailable();}}
    public WritingPageView<WritingResourceSummaryView> list(int page,int pageSize,String q,String kind,String level,String cefr,String status,Long topic,Long genre){
        List<Object> p=new ArrayList<>();String where=where(q,kind,level,cefr,status,topic,genre,p);return page(page,pageSize,where,p);
    }
    public WritingPageView<WritingResourceSummaryView> publicList(int page,int pageSize,String q,String kind,String level,String cefr,Long topic,Long genre){List<Object> p=new ArrayList<>();String where=where(q,kind,level,cefr,"PUBLISHED",topic,genre,p);return page(page,pageSize,where,p);}
    @Transactional public WritingResourceView publish(Long id){WritingResource e=require(id);policy.validatePublication(e.getTitle(),e.getBodyMarkdown(),hasTags(e.getId(),"TOPIC")||hasTags(e.getId(),"GENRE"),e.getResourceKind(),e.getTemplateSchemaJson());jdbc.update("UPDATE english_writing_resource SET publish_status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP(6)) WHERE id=?",id);return get(id);}
    @Transactional public WritingResourceView withdraw(Long id){WritingResource e=require(id);if("DRAFT".equals(e.getPublishStatus()))throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_INVALID_PUBLISH_TRANSITION","Invalid publish transition","A draft cannot be withdrawn.");jdbc.update("UPDATE english_writing_resource SET publish_status='WITHDRAWN' WHERE id=?",id);return get(id);}
    @Transactional public void delete(Long id){WritingResource e=require(id);if("PUBLISHED".equals(e.getPublishStatus()))throw new ApiException(HttpStatus.CONFLICT,"ENGLISH_WRITING_PUBLISHED_DELETE_FORBIDDEN","Published resource cannot be deleted","Withdraw it first."); Integer refs=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_prompt WHERE template_resource_id=? OR model_resource_id=?",Integer.class,id,id);if(refs!=null&&refs>0)throw new ApiException(HttpStatus.CONFLICT,"ENGLISH_WRITING_RESOURCE_IN_USE","Resource is in use","Remove prompt references first.");mapper.deleteById(id);}
    @Transactional public void move(Long id,int index){require(id);List<Long> ids=jdbc.queryForList("SELECT id FROM english_writing_resource ORDER BY sort_order,id",Long.class);ids.remove(id);ids.add(Math.min(Math.max(index,0),ids.size()),id);for(int i=0;i<ids.size();i++)jdbc.update("UPDATE english_writing_resource SET sort_order=? WHERE id=?",(i+1)*10,ids.get(i));}

    private WritingPageView<WritingResourceSummaryView> page(int page,int size,String where,List<Object> p){int safePage=Math.max(1,page), safeSize=Math.min(50,Math.max(1,size));Long total=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_resource r "+where,Long.class,p.toArray());List<WritingResourceSummaryView> rows=jdbc.query(BASE+where+" ORDER BY r.sort_order,r.id LIMIT "+safeSize+" OFFSET "+((safePage-1)*safeSize),p.toArray(),(rs,n)->summary(rs));Map<Long,List<WritingTagRef>> tags=tags(rows.stream().map(WritingResourceSummaryView::id).toList());return new WritingPageView<>(rows.stream().map(x->x.withTags(tags.getOrDefault(x.id(),List.of()))).toList(),safePage,safeSize,total==null?0:total,total==null||total==0?0:(int)((total+safeSize-1)/safeSize));}
    private String where(String q,String kind,String level,String cefr,String status,Long topic,Long genre,List<Object> p){StringBuilder w=new StringBuilder(" WHERE 1=1");if(q!=null&&!q.isBlank()){w.append(" AND (r.title LIKE ? OR r.summary LIKE ?)");p.add("%"+q.trim()+"%");p.add("%"+q.trim()+"%");}if(kind!=null&&!kind.isBlank()){w.append(" AND r.resource_kind=?");p.add(kind);}if(level!=null&&!level.isBlank()){w.append(" AND r.expression_level=?");p.add(level);}if(cefr!=null&&!cefr.isBlank()){w.append(" AND r.cefr_level=?");p.add(cefr);}if(status!=null&&!status.isBlank()){w.append(" AND r.publish_status=?");p.add(status);}tagWhere(w,p,topic,"TOPIC");tagWhere(w,p,genre,"GENRE");return w.toString();}
    private void tagWhere(StringBuilder w,List<Object> p,Long id,String dimension){if(id!=null){w.append(" AND EXISTS (SELECT 1 FROM english_writing_resource_tag wt JOIN english_taxonomy_term t ON t.id=wt.term_id WHERE wt.resource_id=r.id AND wt.term_id=? AND t.dimension=?)");p.add(id);p.add(dimension);}}
    private void validateRequest(WritingResourceRequest r) { policy.validateRequest(r); cefr(r.cefrLevel()); cover(r.coverMediaId()); }
    private void apply(WritingResource e,WritingResourceRequest r,String slug){e.setResourceKind(r.resourceKind());e.setExpressionLevel(clean(r.expressionLevel()));e.setTitle(r.title().trim());e.setSlug(slug);e.setSummary(r.summary().trim());e.setBodyMarkdown(r.bodyMarkdown());e.setCoverMediaId(r.coverMediaId());e.setCefrLevel(r.cefrLevel());e.setWordMin(r.wordMin());e.setWordMax(r.wordMax());e.setEstimatedMinutes(r.estimatedMinutes()==null?0:r.estimatedMinutes());e.setTemplateSchemaJson(clean(r.templateSchemaJson()));if(r.sortOrder()!=null)e.setSortOrder(r.sortOrder());}
    private boolean hasTags(Long id,String d){Integer c=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_resource_tag x JOIN english_taxonomy_term t ON t.id=x.term_id WHERE x.resource_id=? AND t.dimension=? AND t.enabled=1",Integer.class,id,d);return c!=null&&c>0;}
    private void replaceTags(Long id,List<Long> ids){jdbc.update("DELETE FROM english_writing_resource_tag WHERE resource_id=?",id);for(Long term:new LinkedHashSet<>(ids==null?List.of():ids)){String d=jdbc.query("SELECT dimension FROM english_taxonomy_term WHERE id=? AND enabled=1",rs->rs.next()?rs.getString(1):null,term);if(d==null||!(d.equals("TOPIC")||d.equals("GENRE")||d.equals("FUNCTION")||d.equals("ABILITY")))invalid("Invalid writing tag");jdbc.update("INSERT INTO english_writing_resource_tag(resource_id,term_id) VALUES (?,?)",id,term);}}
    private Map<Long,List<WritingTagRef>> tags(List<Long> ids){if(ids.isEmpty())return Map.of();String in=String.join(",",ids.stream().map(String::valueOf).toList());Map<Long,List<WritingTagRef>> out=new LinkedHashMap<>();jdbc.query("SELECT x.resource_id,t.id,t.name,t.slug,t.dimension FROM english_writing_resource_tag x JOIN english_taxonomy_term t ON t.id=x.term_id WHERE x.resource_id IN ("+in+") ORDER BY t.dimension,t.sort_order,t.id",rs->{while(rs.next())out.computeIfAbsent(rs.getLong(1),k->new ArrayList<>()).add(new WritingTagRef(rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5)));return null;});return out;}
    private WritingResourceView mapView(ResultSet rs,int n)throws SQLException{Long id=rs.getLong("id");Long coverId=nullable(rs,"cover_media_id");return new WritingResourceView(id,rs.getString("resource_kind"),rs.getString("expression_level"),rs.getString("title"),rs.getString("slug"),rs.getString("summary"),rs.getString("body_markdown"),coverId,mediaAssets.publicUrl(coverId),rs.getString("cefr_level"),nullableInt(rs,"word_min"),nullableInt(rs,"word_max"),rs.getInt("estimated_minutes"),rs.getString("template_schema_json"),rs.getString("publish_status"),rs.getInt("sort_order"),format(rs,"published_at"),format(rs,"updated_at"),tags(List.of(id)).getOrDefault(id,List.of()));}
    private WritingResourceSummaryView summary(ResultSet rs)throws SQLException{Long coverId=nullable(rs,"cover_media_id");return new WritingResourceSummaryView(rs.getLong("id"),rs.getString("resource_kind"),rs.getString("expression_level"),rs.getString("title"),rs.getString("slug"),rs.getString("summary"),mediaAssets.publicUrl(coverId),rs.getString("cefr_level"),rs.getInt("estimated_minutes"),rs.getString("publish_status"),rs.getInt("sort_order"),format(rs,"updated_at"),List.of());}
    private WritingResource require(Long id){WritingResource e=mapper.selectById(id);if(e==null)throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_WRITING_RESOURCE_NOT_FOUND","Resource not found","The writing resource does not exist.");return e;}
    private boolean slugExists(String slug){return mapper.selectCount(new LambdaQueryWrapper<WritingResource>().eq(WritingResource::getSlug,slug))>0;}
    private void assertSlug(String slug,Long excluded){LambdaQueryWrapper<WritingResource>w=new LambdaQueryWrapper<WritingResource>().eq(WritingResource::getSlug,slug.trim());if(excluded!=null)w.ne(WritingResource::getId,excluded);if(mapper.selectCount(w)>0)throw conflict();}
    private int nextOrder() { Integer value=jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),0) FROM english_writing_resource",Integer.class); return (value==null?0:value)+10; }
    private void cefr(String level) { Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM english_cefr_standard WHERE level=?",Integer.class,level); if(count==null||count==0)invalid("Invalid CEFR level"); }
    private void cover(Long id) { if(id!=null&&!mediaAssets.isImage(id))invalid("Cover must be an image"); }
    private ApiException conflict() { return new ApiException(HttpStatus.CONFLICT,"ENGLISH_CONTENT_SLUG_CONFLICT","Slug already in use","Choose another stable slug."); }
    private ApiException unavailable() { return new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED","Content not available","The requested writing resource is not published."); }
    private void invalid(String detail) { throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_WRITING_INVALID","Invalid writing data",detail); }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private String format(ResultSet rs,String column)throws SQLException { var timestamp=rs.getTimestamp(column); return timestamp==null?null:timezone.atSite(timestamp.toLocalDateTime()).format(ISO); }
    private Long nullable(ResultSet rs,String column)throws SQLException { long value=rs.getLong(column); return rs.wasNull()?null:value; }
    private Integer nullableInt(ResultSet rs,String column)throws SQLException { int value=rs.getInt(column); return rs.wasNull()?null:value; }
}
