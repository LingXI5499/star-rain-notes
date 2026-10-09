package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品主表。全部 SQL 在 mapper/portfolio/WorkMapper.xml。
 *
 * 不再继承 MyBatis-Plus 的 BaseMapper：selectById / selectList / selectCount / updateById
 * 这些语句由框架运行期拼装，源码里看不到实际 SQL，既不符合「所有实际 SQL 写在 XML」的约定，
 * 也让人无法核对「一次 updateById 到底改了哪几列」。这里按访问形状逐个显式声明。
 *
 * updateById 在原先的写法里有两种不同的后果，迁移时分别用独立方法表达：
 *   - 服务层先改字段再保存（改标题、改正文、发布、撤回、恢复、只touch时间戳）；
 *   - 保存的是刚从库里读出来的整行，等于把读出来的值原样写回，实际不改动它。
 * 因此下面每个 UPDATE 都只列该路径真正会写的那几列。
 */
public interface WorkMapper {

    int insert(WorkEntity work);

    WorkEntity selectById(@Param("id") Long id);

    WorkEntity byIdForUpdate(@Param("id") Long id);

    WorkEntity selectBySlugAndStatus(@Param("slug") String slug, @Param("status") String status);

    WorkEntity selectByIdAndStatus(@Param("id") Long id, @Param("status") String status);

    long countByFilter(@Param("workType") String workType, @Param("status") String status,
                       @Param("keyword") String keyword);

    List<WorkEntity> pageByFilter(@Param("workType") String workType, @Param("status") String status,
                                  @Param("keyword") String keyword,
                                  @Param("limit") int limit, @Param("offset") long offset);

    long countByTaxonomy(@Param("workType") String workType, @Param("status") String status,
        @Param("keyword") String keyword, @Param("filter") com.starrainnotes.portfolio.dto.WorkFilterDTO filter);
    List<WorkEntity> pageByTaxonomy(@Param("workType") String workType, @Param("status") String status,
        @Param("keyword") String keyword, @Param("filter") com.starrainnotes.portfolio.dto.WorkFilterDTO filter,
        @Param("limit") int limit, @Param("offset") long offset);
    long countBySlug(@Param("slug") String slug);

    /* 编辑工作区基础信息：只写 slug / title / summary 与审计列 */
    int updateContent(WorkEntity work);
    int updatePrototype(WorkEntity work);
    com.starrainnotes.portfolio.vo.WorkNavigationVO previousPublished(@Param("id") Long id, @Param("sortOrder") int sortOrder);
    com.starrainnotes.portfolio.vo.WorkNavigationVO nextPublished(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    /* 正文单独保存：正文可能有 1MB，不能跟着基础信息一起整行写 */
    int updateBody(WorkEntity work);

    /* 媒体、外链、类型详情的增删改只更新作品的审计列 */
    int touch(WorkEntity work);

    int publish(WorkEntity work);

    int withdraw(WorkEntity work);

    int restore(WorkEntity work);

    int deleteById(@Param("id") Long id);
}
