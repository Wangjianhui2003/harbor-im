package com.jianhui.project.harbor.platform.util;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

/**
 * minio工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    /**
     * 判断bucket是否存在
     */
    public Boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.info("查询bucket失败, bucketName: {}, error: {}", bucketName, e.getMessage());
            return false;
        }
    }

    /**
     * 创建bucket
     */
    public void makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.info("创建bucket失败, bucketName: {}, error: {}", bucketName, e.getMessage());
        }
    }

    /**
     * 设置bucket为公有
     */
    public void setBucketPublic(String bucketName) {
        //TODO:调整设置公有的配置
        try {
            // 设置bucket为公开
            String sb = "{\"Version\":\"2012-10-17\"," +
                    "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":" +
                    "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"," +
                    "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucketName +
                    "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\"],\"Resource\":[\"arn:aws:s3:::" +
                    bucketName +
                    "/*\"]}]}";
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(sb)
                            .build());
        } catch (Exception e) {
            log.info("设置bucket为公有失败, bucketName: {}, error: {}", bucketName, e.getMessage());
        }
    }

    /**
     * 上传文件(multipartFile)
     * @param bucketName 存储桶名称
     * @param path       minio存储路径
     * @param file       文件
     */
    public String upload(String bucketName, String path, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException("文件名不能为空");
        }
        //为文件重命名
        String filename = System.currentTimeMillis() + "";
        if (originalFilename.lastIndexOf(".") >= 0) {
            filename += originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        //小文件路径(日期/文件名)
        String objectName = DateTimeUtils.getFormatDate(new Date(), DateTimeUtils.PARTDATEFORMAT) + "/" + filename;
        try {
            InputStream inputStream = file.getInputStream();
            PutObjectArgs arg = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path + "/" + objectName)
                    //流参数
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            //文件名称相同会覆盖
            minioClient.putObject(arg);
        } catch (Exception e) {
            log.error("上传文件失败(MinioUtil),", e);
            return null;
        }
        return objectName;
    }

    /**
     * byte[]文件上传(缩略图等)
     */
    public String upload(String bucketName, String path, String name, byte[] fileByte, String contentType) {
        String fileName = System.currentTimeMillis() + name.substring(name.lastIndexOf("."));
        String objectName = DateTimeUtils.getFormatDate(new Date(), DateTimeUtils.PARTDATEFORMAT) + "/" + fileName;

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileByte);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path + "/" + objectName)
                    .stream(inputStream, fileByte.length, -1)
                    .contentType(contentType)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            log.error("上传文件失败,", e);
            return null;
        }
        return objectName;
    }

    /**
     * 移除文件
     */
    public boolean remove(String bucketName, String path, String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path + fileName)
                    .build());
        } catch (Exception e) {
            log.error("删除文件失败,", e);
            return false;
        }
        return true;
    }
}
