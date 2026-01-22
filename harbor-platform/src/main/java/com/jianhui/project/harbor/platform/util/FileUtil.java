package com.jianhui.project.harbor.platform.util;

public final class FileUtil {

    /**
     * 获取文件后缀
     *
     * @param fileName 文件名
     * @return boolean
     */
    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 判断文件是否图片类型
     *
     * @param fileName 文件名
     * @return boolean
     */
    public static boolean isImage(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        String[] imageExtension = new String[]{
                "jpeg", "jpg", "bmp", "png", "webp", "gif",
                "svg", "ico", "tiff", "tif", "heic", "heif", "avif"
        };
        for (String e : imageExtension) {
            if (extension.equals(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件是否视频类型
     *
     * @param fileName 文件名
     * @return boolean
     */
    public static boolean isVideo(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        String[] videoExtension = new String[]{
                "mp4", "avi", "mov", "mkv", "flv", "wmv", "webm",
                "m4v", "mpeg", "mpg", "3gp", "3g2", "ogv", "ts", "mts"
        };
        for (String e : videoExtension) {
            if (extension.equals(e)) {
                return true;
            }
        }
        return false;
    }
}
