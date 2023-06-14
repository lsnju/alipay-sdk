package com.lsnju.sdk.alipay;

import java.util.Map;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Request;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayRequest;
import com.lsnju.sdk.alipay.config.ClientConfig;

/**
 *
 * @author ls
 * @since 2020/12/25 14:41
 * @version V1.0
 */
public class OpenAlipayClient extends AbstractWpAlipayClient {

    public static OpenAlipayClient newInstance(String serverUrl, String appId, String privateKey, String format,
                                               String charset, String alipayPublicKey, String signType) {
        return newInstance(ClientConfig.builder()
            .appId(appId)
            .mchPrivateKey(privateKey)
            .alipayPublicKey(alipayPublicKey)
            .serverUrl(serverUrl)
            .signType(signType)
            .format(format)
            .charset(charset)
            .build());
    }

    public static OpenAlipayClient newInstance(ClientConfig clientConfig) {
        return new OpenAlipayClient(clientConfig);
    }

    public OpenAlipayClient(ClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    protected String post(AlipayRequest<?> request, String accessToken, String appAuthToken, String targetAppId) throws AlipayApiException {
        final Map<String, String> params = getBizParam(request, accessToken, appAuthToken, targetAppId);
        return post(request, params);
    }

    private String post(AlipayRequest<?> request, Map<String, String> params) throws AlipayApiException {
        try {
            Request req = Request.Post(this.serverUrl)
                .connectTimeout(getConnectTimeout())
                .socketTimeout(getSocketTimeout())
                .userAgent(getUserAgent())
                .addHeader(METHOD, request.getApiMethodName())
                .body(new UrlEncodedFormEntity(getNameValuePairs(params), this.charset));
            return httpClientPost(req);
        } catch (Exception e) {
            throw new AlipayApiException(e);
        }
    }

}
