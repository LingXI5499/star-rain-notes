package com.starrainnotes.media.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.dto.MediaQueryDTO;
import com.starrainnotes.media.vo.MediaAssetDetailVO;
import com.starrainnotes.media.vo.MediaAssetVO;

// 媒体资产的模块内服务契约
public interface MediaAssetService {

    PageResult<MediaAssetVO> page(MediaQueryDTO query);

    MediaAssetDetailVO getDetail(Long mediaAssetId);

    // MED-006 归档：必须无业务引用，且只有 ACTIVE 才能归档
    void archive(Long mediaAssetId);

    // 归档生命周期里的管理动作，不新增 Formal Use Case
    void restore(Long mediaAssetId);

    void changeAccessLevel(Long mediaAssetId, MediaAccessLevel accessLevel);
}
