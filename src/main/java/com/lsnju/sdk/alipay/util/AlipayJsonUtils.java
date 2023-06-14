package com.lsnju.sdk.alipay.util;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.alipay.api.AlipayConstants;
import com.alipay.api.AlipayObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 *
 * @author ls
 * @since 2021/3/12 20:30
 * @version V1.0
 */
public class AlipayJsonUtils {

    public static final int MAX_SIZE = 1 << 13;

    private static final Gson GSON = new GsonBuilder()
        .setExclusionStrategies(new GsonExclusionStrategy())
        .setFieldNamingStrategy(new GsonFieldNamingStrategy())
        .setDateFormat(AlipayConstants.DATE_TIME_FORMAT)
        .create();

    private static final Gson GP = new GsonBuilder()
        .setDateFormat(AlipayConstants.DATE_TIME_FORMAT)
        .setPrettyPrinting()
        .create();

    public static String toJson(AlipayObject alipayObject) {
        return GSON.toJson(alipayObject);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonSyntaxException {
        return GSON.fromJson(json, clazz);
    }

    public static boolean isValidJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            return JsonParser.parseString(json) != null;
        } catch (JsonSyntaxException ignore) {
            return false;
        }
    }

    public static String toPrettyFormat(String jsonString) {
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        return toJsonPretty(json);
    }

    public static String toJsonPretty(Object obj) {
        Objects.requireNonNull(obj);
        return GP.toJson(obj);
    }

    public static String getRawValue(final String json, final String tag) {
        String aTag = String.format("\"%s\":", tag);
        int index = StringUtils.indexOf(json, aTag);
        if (index < 0) {
            return StringUtils.EMPTY;
        }
        for (int i = index + aTag.length(); i < MAX_SIZE; i++) {
            switch (json.charAt(i)) {
                case ' ':
                    continue;
                case '{':
                    return getObjectValue(i, json);
                case '"':
                    return getStringValue(i, json);
                case '[':
                    return getArrayValue(i, json);
                default:
                    return getValue(i, json);
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getValue(final int fromIndex, final String json) {
        final StringBuilder sb = new StringBuilder();
        for (int i = fromIndex, max = json.length(); i < max; i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') {
                String retValue = sb.toString();
                if (StringUtils.equals(retValue, "null")) {
                    return null;
                }
                return retValue;
            }
            sb.append(c);
        }
        return StringUtils.EMPTY;
    }

    private static String getObjectValue(final int fromIndex, final String json) {
        final StringBuilder sb = new StringBuilder();
        boolean escapes = false;
        boolean strData = false;
        for (int count = 0, i = fromIndex, max = json.length(); i < max; i++) {
            final char c = json.charAt(i);
            if (escapes) {
                sb.append('\\').append(c);
                escapes = false;
            } else {
                switch (c) {
                    case '"':
                        sb.append(c);
                        strData = !strData;
                        break;
                    case '\\':
                        escapes = true;
                        break;
                    case '{':
                        sb.append(c);
                        if (!strData) {
                            count++;
                        }
                        break;
                    case '}':
                        sb.append(c);
                        if (!strData) {
                            if (--count == 0) {
                                return sb.toString();
                            }
                        }
                        break;
                    default:
                        sb.append(c);
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getArrayValue(final int fromIndex, final String json) {
        final StringBuilder sb = new StringBuilder();
        boolean escapes = false;
        boolean strData = false;
        for (int count = 0, i = fromIndex, max = json.length(); i < max; i++) {
            final char c = json.charAt(i);
            if (escapes) {
                sb.append('\\').append(c);
                escapes = false;
            } else {
                switch (c) {
                    case '"':
                        sb.append(c);
                        strData = !strData;
                        break;
                    case '\\':
                        escapes = true;
                        break;
                    case '[':
                        sb.append(c);
                        if (!strData) {
                            count++;
                        }
                        break;
                    case ']':
                        sb.append(c);
                        if (!strData) {
                            if (--count == 0) {
                                return sb.toString();
                            }
                        }
                        break;
                    default:
                        sb.append(c);
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getStringValue(final int fromIndex, final String json) {
        final StringBuilder sb = new StringBuilder();
        boolean escapes = false;
        for (int i = fromIndex + 1, max = json.length(); i < max; i++) {
            char c = json.charAt(i);
            if (escapes) {
                sb.append('\\').append(c);
                escapes = false;
            } else {
                switch (c) {
                    case '"':
                        return sb.toString();
                    case '\\':
                        escapes = true;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
        }
        return StringUtils.EMPTY;
    }

}
