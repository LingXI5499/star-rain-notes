package com.starrainnotes.site.service;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.vo.HomeSectionSettingVO;
import java.util.List;

/*
 * 首页区块的顺序与可见性管理。
 *
 * 返回类型的分工是刻意的：
 *   - 面向前端的三个方法（all / reorder / patch）返回 `HomeSectionSettingVO`，把持久化实体挡在 HTTP 契约之外；
 *   - `enabled()` 只服务模块内部（首页聚合与各 Provider 都要读 config_json 决定取多少条），
 *     返回实体，避免内部为了一次读取多绕一层映射。
 */
public interface HomeSectionService {
    /** 按展示顺序返回全部区块（后台视图）。 */
    List<HomeSectionSettingVO> all();

    /** 按展示顺序返回已启用区块（模块内部使用，保留实体）。 */
    List<HomeSectionEntity> enabled();

    /** 按提交顺序整体重排；缺项或重复码一律拒绝。 */
    List<HomeSectionSettingVO> reorder(HomeSectionOrderDTO request);

    /** 修改单个区块的名称、可见性或布局参数。 */
    HomeSectionSettingVO patch(String code, HomeSectionPatchDTO request);
}
