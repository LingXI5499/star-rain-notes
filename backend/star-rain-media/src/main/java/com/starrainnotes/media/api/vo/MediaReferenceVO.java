/*
 * MediaReferenceVO：媒体模块的响应数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.media.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 模块间媒体引用视图。业务模块只拿到这个，拿不到 Media 的 Entity 与 Mapper。
 *
 * 放在 api/vo 而不是 api/dto：它是**视图对象**（对外返回的读模型），
 * 按项目约定视图对象以 VO 结尾；而 api 包是它在架构上的位置（跨模块契约只住在这里）。
 *
 * 与模块内的 media.vo.MediaReferenceVO 是两个不同的读模型，不是重复：
 * 那个带 id / createdAt 并做了 Long→String 序列化，供后台「正在被谁引用」面板；
 * 这个只暴露业务方真正需要的五个字段。两者同名，所以在 MediaReferenceApiAdapter 里
 * 引用模块内那个必须写全限定名 —— 这是本包唯一的使用约束。
 */
/**
 * 媒体引用响应对象，属于媒体模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceVO {

    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;
    /**
     * 引用或数据来源所属模块。
     */
    private String sourceModule;
    /**
     * 来源对象的业务类型。
     */
    private String sourceType;
    /**
     * 来源对象的业务标识。
     */
    private Long sourceId;
    /**
     * usage代码。
     */
    private String usageCode;
}
