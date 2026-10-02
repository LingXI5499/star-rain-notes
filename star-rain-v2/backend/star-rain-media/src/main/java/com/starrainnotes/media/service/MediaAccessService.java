package com.starrainnotes.media.service;

import com.starrainnotes.media.vo.MediaContent;

/*
 * MED-005 获取公开 / 受保护访问资源。
 *
 * 刻意不接受调用方传入的“访问上下文”参数：
 * 当前主体只能取自认证上下文，否则浏览器就能通过伪造参数提升权限。
 */
public interface MediaAccessService {

    /*
     * 授权并打开资源。
     *
     * rangeHeader 是原始 Range 请求头，null 或空表示完整内容。
     * 区间由实现按真实文件长度解析与裁剪，不可满足时抛 416。
     * 调用方负责关闭返回的流。
     */
    MediaContent open(Long mediaAssetId, String rangeHeader);
}
