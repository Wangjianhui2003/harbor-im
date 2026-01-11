package com.jianhui.project.harbor.platform.service;

import com.jianhui.project.harbor.platform.pojo.vo.UploadImageVO;
import com.jianhui.project.harbor.platform.pojo.vo.UploadVideoVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 处理文件上传
 */
public interface FileService {

    UploadImageVO uploadImage(MultipartFile file);

    String uploadFile(MultipartFile file);

    UploadVideoVO uploadVideo(MultipartFile file);

}
