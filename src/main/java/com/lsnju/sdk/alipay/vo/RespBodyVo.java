package com.lsnju.sdk.alipay.vo;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author ls
 * @since 2021/6/4 13:23
 * @version V1.0
 */
@Getter
@Setter
public class RespBodyVo extends BaseMo {

    private String sign;
    private String body;
    private String realBody;

}
