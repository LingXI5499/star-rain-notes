/*
 * MediaAccessService：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：MediaContentVO。
 * 阅读入口：open（打开媒体访问）。
 */

package com.starrainnotes.media.service;

import com.starrainnotes.media.vo.MediaContentVO;

/*
 * MED-005 获取公开 / 受保护访问资源。
 *
 * 刻意不接受调用方传入的“访问上下文”参数：
 * 当前主体只能取自认证上下文，否则浏览器就能通过伪造参数提升权限。
 */
/**
 * 媒体访问业务服务，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaAccessService {

    /*
     * 授权并打开资源。
     *
     * rangeHeader 是原始 Range 请求头，null 或空表示完整内容。
     * 区间由实现按真实文件长度解析与裁剪，不可满足时抛 416。
     * 调用方负责关闭返回的流。
     */
    /**
     * 打开媒体访问。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param rangeHeader 范围响应头；类型为 <code>String</code>
     * @return 类型为 <code>MediaContentVO</code> 的处理结果
     */
    MediaContentVO open(Long mediaAssetId, String rangeHeader);
}
