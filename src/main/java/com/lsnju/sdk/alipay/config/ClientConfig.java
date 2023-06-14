package com.lsnju.sdk.alipay.config;


import com.lsnju.sdk.alipay.vo.BaseMo;

import lombok.Builder;
import lombok.Getter;


/**
 *
 * @author ls
 * @since 2023-06-14 20:55:09
 * @version V1.0
 */
@Builder
@Getter
public class ClientConfig extends BaseMo {

    private final String serverUrl;
    private final String appId;
    private final String signType;
    private final String format;
    private final String charset;
    private final String mchPrivateKey;
    private final String alipayPublicKey;
    private final String encryptKey;
    private final String encryptType;

    private final String userAgent;
    private final int socketTimeout;
    private final int connectTimeout;

}
