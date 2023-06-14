package com.lsnju.sdk.alipay.util;

import com.alipay.api.internal.mapping.ApiField;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 *
 * @author lisong
 * @since 2020-03-05 22:30:26
 * @version V1.0
 */
public class GsonExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(ApiField.class) == null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

}
