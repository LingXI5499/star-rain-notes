package com.starrainnotes.media.service;

import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.vo.MediaAssetVO;
import org.springframework.web.multipart.MultipartFile;

// MED-001 上传媒体
public interface MediaUploadService {

    // accessLevel 为空时按 PROTECTED 处理：默认不公开
    MediaAssetVO upload(MultipartFile file, MediaAccessLevel accessLevel);
}
