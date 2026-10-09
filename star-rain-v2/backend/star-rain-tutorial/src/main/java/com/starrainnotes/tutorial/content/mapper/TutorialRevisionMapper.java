package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程不可变版本快照的全部 SQL 都在 mapper/tutorial/TutorialRevisionMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialRevisionMapper {

    int insert(TutorialRevisionEntity revision);

    TutorialRevisionEntity selectById(@Param("id") Long id);

    /* 审核按 revision_ref 精确定位版本。 */
    TutorialRevisionEntity selectByTutorialIdAndRef(@Param("tutorialId") Long tutorialId,
                                                    @Param("revisionRef") String revisionRef);

    /* 审核通过前必须确认这是最新版本，避免旧版本覆盖新版本。 */
    TutorialRevisionEntity selectLatestByTutorialId(@Param("tutorialId") Long tutorialId);

    long countByTutorialId(@Param("tutorialId") Long tutorialId);
}
