package com.lsnju.sdk.alipay.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.alipay.api.AlipayConstants;

/**
 *
 * @author ls
 * @since 2020/12/27 11:14
 * @version V1.0
 */
public class AlipayDateUtils {

    public static final String DATE_TIME_FORMAT = AlipayConstants.DATE_TIME_FORMAT;

    public static String getDateTimeString(Date date) {
        return format(date, DATE_TIME_FORMAT);
    }

    public static String format(Date date, String format) {
        return format(date, new SimpleDateFormat(format));
    }

    public static String format(Date date, DateFormat format) {
        if (date == null || format == null) {
            return null;
        }
        format.setTimeZone(TimeZone.getTimeZone(AlipayConstants.DATE_TIMEZONE));
        return format.format(date);
    }
}
