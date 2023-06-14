package com.lsnju.sdk.alipay.util;

import java.lang.reflect.Field;

import com.alipay.api.internal.mapping.ApiField;
import com.google.gson.FieldNamingStrategy;

/**
 *
 * @author ls
 * @since 2020/12/28 15:00
 * @version V1.0
 */
public class GsonFieldNamingStrategy implements FieldNamingStrategy {
    @Override
    public String translateName(Field f) {
        final ApiField api = f.getAnnotation(ApiField.class);
        if (api != null) {
            return api.value();
        }
        return f.getName();
    }
}
