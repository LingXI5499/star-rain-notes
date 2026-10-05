package com.starrainnotes.media.storage;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次存储写入请求 —— 存储抽象的入参。
 *
 * 放在 storage 包而不是 dto：它是 MediaStorage 契约的一部分（就像 api 包里
 * 「只有接口 + 契约 DTO」那样），跟着抽象走才能一眼看出「换存储实现时哪些类型需要一起实现」。
 * 放在 dto 会让人误以为它是某个 HTTP 请求体。
 *
 * 只携带内容流与规范化扩展名：storageKey 由存储实现按自己的目录布局生成，
 * 业务层不参与路径拼接，也就无法制造越界路径。
 *
 * 用 InputStream 而不是 byte[]，是为了让视频这类大文件流式落盘。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageWriteCommand {

    private InputStream content;
    private String fileExtension;
}
