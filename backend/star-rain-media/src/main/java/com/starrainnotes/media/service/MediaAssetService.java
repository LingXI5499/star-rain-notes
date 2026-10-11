/*
 * MediaAssetService：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、MediaAccessLevel、MediaQueryDTO、MediaAssetDetailVO、MediaAssetVO。
 * 阅读入口：page（分页读取媒体资产）；getDetail（读取详情）；archive（archive处理入口）；restore（恢复媒体资产）。
 */

package com.starrainnotes.media.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.dto.MediaQueryDTO;
import com.starrainnotes.media.vo.MediaAssetDetailVO;
import com.starrainnotes.media.vo.MediaAssetVO;

// 媒体资产的模块内服务契约
/**
 * 媒体资产业务服务，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaAssetService {

    /**
     * 分页读取媒体资产。
     *
     * @param query 查询参数；类型为 <code>MediaQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;MediaAssetVO&gt;</code> 的处理结果
     */
    PageResult<MediaAssetVO> page(MediaQueryDTO query);

    /**
     * 读取详情。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaAssetDetailVO</code> 的处理结果
     */
    MediaAssetDetailVO getDetail(Long mediaAssetId);

    // MED-006 归档：必须无业务引用，且只有 ACTIVE 才能归档
    /**
     * archive处理入口。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
    void archive(Long mediaAssetId);

    // 归档生命周期里的管理动作，不新增 Formal Use Case
    /**
     * 恢复媒体资产。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
    void restore(Long mediaAssetId);

    /**
     * change访问Level处理入口。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param accessLevel 媒体访问级别；类型为 <code>MediaAccessLevel</code>
     */
    void changeAccessLevel(Long mediaAssetId, MediaAccessLevel accessLevel);
}
