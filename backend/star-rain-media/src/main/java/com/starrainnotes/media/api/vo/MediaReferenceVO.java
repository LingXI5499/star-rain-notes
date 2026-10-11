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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceVO {

    private Long mediaAssetId;
    private String sourceModule;
    private String sourceType;
    private Long sourceId;
    private String usageCode;
}
