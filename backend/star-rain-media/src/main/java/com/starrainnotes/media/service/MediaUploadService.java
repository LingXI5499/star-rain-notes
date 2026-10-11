/*
 * MediaUploadService：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：MediaAccessLevel、MediaAssetVO。
 * 阅读入口：upload（上传媒体上传）。
 */

package com.starrainnotes.media.service;

import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.vo.MediaAssetVO;
import org.springframework.web.multipart.MultipartFile;

// MED-001 上传媒体
/**
 * 媒体上传业务服务，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaUploadService {

    // accessLevel 为空时按 PROTECTED 处理：默认不公开
    /**
     * 上传媒体上传。
     *
     * @param file 待处理的上传文件；类型为 <code>MultipartFile</code>
     * @param accessLevel 媒体访问级别；类型为 <code>MediaAccessLevel</code>
     * @return 类型为 <code>MediaAssetVO</code> 的处理结果
     */
    MediaAssetVO upload(MultipartFile file, MediaAccessLevel accessLevel);
}
