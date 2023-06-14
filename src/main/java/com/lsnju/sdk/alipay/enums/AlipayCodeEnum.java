package com.lsnju.sdk.alipay.enums;

import java.util.stream.Stream;

import lombok.Getter;

/**
 *
 * @author lisong
 * @since 2021/10/18 15:18
 * @version V1.0
 */
@Getter
public enum AlipayCodeEnum {

    // 公共错误码
    // https://opendocs.alipay.com/open/common/105806

    SUCCESS("10000", "接口调用成功"),
    PROGRESS("10003", "业务处理中"),

    SERVICE_UNAVAILABLE("20000", "服务不可用"),
    UNAUTHORIZED("20001", "授权权限不足"),
    BAD_REQUEST("40001", "缺少必选参数"),
    ILLEGAL_ARGUMENT("40002", "非法的参数"),
    BIZ_ERROR("40004", "业务处理失败"),
    FREQUENCY_LIMITED("40005", "调用频次超限"),
    PERMISSION_DENIED("40006", "权限不足"),


    ;

    private final String code;
    private final String desc;

    AlipayCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AlipayCodeEnum getByCode(String code) {
        return Stream.of(values()).filter(i -> i.code.equals(code)).findAny().orElse(null);
    }

}
