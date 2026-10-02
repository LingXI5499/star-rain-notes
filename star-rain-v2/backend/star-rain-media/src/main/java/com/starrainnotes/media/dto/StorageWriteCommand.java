package com.starrainnotes.media.dto;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次存储写入请求。
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
