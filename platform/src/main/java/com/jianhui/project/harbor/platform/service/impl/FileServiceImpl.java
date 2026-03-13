package com.jianhui.project.harbor.platform.service.impl;


import com.jianhui.project.harbor.platform.config.props.MinioProperties;
import com.jianhui.project.harbor.platform.constant.Constant;
import com.jianhui.project.harbor.platform.dto.response.UploadImageRespDTO;
import com.jianhui.project.harbor.platform.dto.response.UploadVideoRespDTO;
import com.jianhui.project.harbor.platform.enums.FileType;
import com.jianhui.project.harbor.platform.enums.ResultCode;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.FileService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.util.FileUtil;
import com.jianhui.project.harbor.platform.util.ImageUtil;
import com.jianhui.project.harbor.platform.util.MinioUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * 处理文件上传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioUtil minioUtil;

    private final MinioProperties minioProps;

    /**
     * 初始化，检查bucket
     */
    @PostConstruct
    public void init() {
        if (!minioUtil.bucketExists(minioProps.getBucketName())) {
            // 创建bucket
            minioUtil.makeBucket(minioProps.getBucketName());
            // 公开bucket
            minioUtil.setBucketPublic(minioProps.getBucketName());
        }
    }

    /**
     * 上传到文件file路径
     */
    @Override
    public String uploadFile(MultipartFile file) {
        Long userId = SessionContext.getSession().getUserId();
        // 大小校验
        if (file.getSize() > Constant.MAX_FILE_SIZE) {
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "文件不能超过" + Constant.MAX_FILE_SIZE / 1024 / 1024 + "MB");
        }
        //上传到file路径
        String filename = minioUtil.upload(
                minioProps.getBucketName(),
                minioProps.getFilePath(),
                file
        );
        if (StringUtils.isBlank(filename)) {
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "文件上传失败");
        }
        //访问路径
        String url = generUrl(FileType.FILE, filename);
        log.info("上传文件成功，用户id:{},url:{}", userId, url);
        return url;
    }

    /**
     * 上传图片
     */
    @Override
    public UploadImageRespDTO uploadImage(MultipartFile file) {
        try {
            Long userId = SessionContext.getSession().getUserId();
            // 大小校验
            if (file.getSize() > Constant.MAX_IMAGE_SIZE) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片不能超过" + Constant.MAX_IMAGE_SIZE / 1024 / 1024 + "MB");
            }
            // 图片格式校验
            if (!FileUtil.isImage(file.getOriginalFilename())) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片格式不正确");
            }
            // 上传原图
            UploadImageRespDTO vo = new UploadImageRespDTO();
            String fileName = minioUtil.upload(minioProps.getBucketName(), minioProps.getImagePath(), file);
            if (StringUtils.isEmpty(fileName)) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片上传失败");
            }
            vo.setOriginUrl(generUrl(FileType.IMAGE, fileName));
            // 大于30K的文件需上传缩略图
            if (file.getSize() > 30 * 1024) {
                byte[] imageByte = ImageUtil.compressForScale(file.getBytes(), 30);
                fileName = minioUtil.upload(minioProps.getBucketName(), minioProps.getImagePath(), Objects.requireNonNull(file.getOriginalFilename()), imageByte, file.getContentType());
                if (StringUtils.isEmpty(fileName)) {
                    throw new GlobalException(ResultCode.PROGRAM_ERROR, "缩略图上传失败");
                }
            }
            vo.setThumbUrl(generUrl(FileType.IMAGE, fileName));
            log.info("文件图片成功，用户id:{},origin url:{}", userId, vo.getOriginUrl());
            return vo;
        } catch (Exception e) {
            log.error("上传图片失败，{}", e.getMessage(), e);
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片上传失败");
        }
    }

    /**
     * 生成文件访问url
     */
    public String generUrl(FileType fileTypeEnum, String fileName) {
        String url = minioProps.getDomain() + "/" + minioProps.getBucketName();
        switch (fileTypeEnum) {
            case FILE:
                url += "/" + minioProps.getFilePath() + "/";
                break;
            case IMAGE:
                url += "/" + minioProps.getImagePath() + "/";
                break;
            case VIDEO:
                url += "/" + minioProps.getVideoPath() + "/";
                break;
            default:
                break;
        }
        url += fileName;
        return url;
    }

    /**
     * 上传视频
     */
    @Override
    public UploadVideoRespDTO uploadVideo(MultipartFile file) {
        try {
            Long userId = SessionContext.getSession().getUserId();
            // 大小校验
            if (file.getSize() > Constant.MAX_VIDEO_SIZE) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR,
                        "视频不能超过" + Constant.MAX_VIDEO_SIZE / 1024 / 1024 + "MB");
            }
            // 视频格式校验
            if (!FileUtil.isVideo(file.getOriginalFilename())) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频格式不正确");
            }
            // 上传视频
            String fileName = minioUtil.upload(
                    minioProps.getBucketName(),
                    minioProps.getVideoPath(),
                    file
            );
            if (StringUtils.isEmpty(fileName)) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频上传失败");
            }

            UploadVideoRespDTO vo = new UploadVideoRespDTO();
            vo.setUrl(generUrl(FileType.VIDEO, fileName));
            vo.setCoverUrl(""); // 封面需前端自行处理或后续扩展
            vo.setDuration(0L); // 时长需额外工具提取，暂返回0

            log.info("上传视频成功，用户id:{}, url:{}", userId, vo.getUrl());
            return vo;
        } catch (Exception e) {
            log.error("上传视频失败，{}", e.getMessage(), e);
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频上传失败");
        }
    }
}

