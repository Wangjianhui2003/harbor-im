package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.dto.response.UploadImageRespDTO;
import com.jianhui.project.harbor.platform.dto.response.UploadVideoRespDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 处理文件上传
 */
public interface FileService {

    UploadImageRespDTO uploadImage(MultipartFile file);

    String uploadFile(MultipartFile file);

    UploadVideoRespDTO uploadVideo(MultipartFile file);

}
