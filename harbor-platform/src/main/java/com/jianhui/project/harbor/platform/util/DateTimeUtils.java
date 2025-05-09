package com.jianhui.project.harbor.platform.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期处理工具类
 */
public final class DateTimeUtils extends DateUtils {

    /**
     * 默认日期格式
     */
    public static final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将文件以日期来划分的文件夹日期格式
     */
    public static final String PARTDATEFORMAT = "yyyyMMdd";

    /**
     * 将日期类型转换为字符串
     */
    public static String getFormatDate(Date date, String xFormat) {
        date = date == null ? new Date() : date;
        xFormat = StringUtils.isNotEmpty(xFormat) ? xFormat : FULL_DATE_FORMAT;
        SimpleDateFormat sdf = new SimpleDateFormat(xFormat);
        return sdf.format(date);
    }


}