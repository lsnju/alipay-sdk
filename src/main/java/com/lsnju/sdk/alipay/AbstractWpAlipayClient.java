package com.lsnju.sdk.alipay;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.joor.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.AlipayRequest;
import com.alipay.api.AlipayResponse;
import com.alipay.api.BatchAlipayRequest;
import com.alipay.api.BatchAlipayResponse;
import com.alipay.api.Decryptor;
import com.alipay.api.DefaultDecryptor;
import com.alipay.api.DefaultEncryptor;
import com.alipay.api.DefaultSignChecker;
import com.alipay.api.DefaultSigner;
import com.alipay.api.Encryptor;
import com.alipay.api.SignChecker;
import com.alipay.api.Signer;
import com.alipay.api.internal.util.WebUtils;
import com.google.common.base.CaseFormat;
import com.lsnju.sdk.alipay.config.ClientConfig;
import com.lsnju.sdk.alipay.enums.AlipayCodeEnum;
import com.lsnju.sdk.alipay.util.AlipayDateUtils;
import com.lsnju.sdk.alipay.util.AlipayJsonUtils;
import com.lsnju.sdk.alipay.vo.RespBodyVo;

import lombok.Getter;
import lombok.SneakyThrows;

/**
 *
 * @author ls
 * @since 2021/3/16 15:00
 * @version V1.0
 */
public abstract class AbstractWpAlipayClient implements AlipayClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWpAlipayClient.class);
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String ISV_INVALID_SIGNATURE = "isv.invalid-signature";
    public static final String METHOD = "api-method";
    public static final String SDK_VERSION = getSdkVersion();
    public static final String JAVA_VERSION = javaVersion();
    public static String SDK_BUILD_TIME;
    private static final String USER_AGENT = "lsnju-sdk/" + SDK_VERSION + " (Java/" + JAVA_VERSION + ")";

    private static String getSdkVersion() {
        String version = "unknown-version";
        try (InputStream inputStream = AbstractWpAlipayClient.class.getResourceAsStream("/com/lsnju/sdk/alipay/alipay-sdk.properties")) {
            if (inputStream != null) {
                final Properties properties = new Properties();
                properties.load(inputStream);
                version = properties.getProperty("alipay-sdk.version");
                SDK_BUILD_TIME = properties.getProperty("alipay-sdk.build-time");
                logger.info("build-time = {}", SDK_BUILD_TIME);
            }
        } catch (IOException ignore) {
            // ignore
        }
        return version;
    }

    private static String javaVersion() {
        return System.getProperty("java.version");
    }

    public static final int CONNECT_TIMEOUT = 10_000;
    public static final int SOCKET_TIMEOUT = 20_000;

    public static final String SERVER_URL = "https://openapi.alipay.com/gateway.do";

    protected AbstractWpAlipayClient(ClientConfig clientConfig) {
        this.appId = Objects.requireNonNull(clientConfig.getAppId());
        this.signer = new DefaultSigner(Objects.requireNonNull(clientConfig.getMchPrivateKey()));
        this.signChecker = new DefaultSignChecker(Objects.requireNonNull(clientConfig.getAlipayPublicKey()));
        this.serverUrl = StringUtils.defaultIfBlank(clientConfig.getServerUrl(), SERVER_URL);
        this.signType = StringUtils.defaultIfBlank(clientConfig.getSignType(), AlipayConstants.SIGN_TYPE_RSA2);
        this.format = StringUtils.defaultIfBlank(clientConfig.getFormat(), AlipayConstants.FORMAT_JSON);
        this.charset = StringUtils.defaultIfBlank(clientConfig.getCharset(), AlipayConstants.CHARSET_UTF8);
        this.encryptType = StringUtils.defaultIfBlank(clientConfig.getEncryptType(), AlipayConstants.ENCRYPT_TYPE_AES);
        this.encryptKey = clientConfig.getEncryptKey();
        this.encryptor = new DefaultEncryptor(clientConfig.getEncryptKey());
        this.decryptor = new DefaultDecryptor(clientConfig.getEncryptKey());
        this.socketTimeout = clientConfig.getSocketTimeout() > 0 ? clientConfig.getSocketTimeout() : SOCKET_TIMEOUT;
        this.connectTimeout = clientConfig.getConnectTimeout() > 0 ? clientConfig.getConnectTimeout() : CONNECT_TIMEOUT;
        this.userAgent = StringUtils.defaultIfBlank(clientConfig.getUserAgent(), USER_AGENT);
    }

    protected final String appId;
    protected final String serverUrl;
    protected final String signType;
    protected final String format;
    protected final String charset;
    protected final Signer signer;
    protected final SignChecker signChecker;

    private final String encryptType;
    private final String encryptKey;
    private final Encryptor encryptor;
    private final Decryptor decryptor;

    @Getter
    private final int socketTimeout;
    @Getter
    private final int connectTimeout;
    @Getter
    private final String userAgent;

    protected void debugJson(String jsonStr) {
        if (log.isDebugEnabled()) {
            if (AlipayJsonUtils.isValidJson(jsonStr)) {
                log.debug("{}", AlipayJsonUtils.toPrettyFormat(jsonStr));
            } else {
                log.debug("{}", jsonStr);
            }
        }
    }

    @Override
    public <T extends AlipayResponse> T execute(AlipayRequest<T> request) throws AlipayApiException {
        return execute(request, null, null, null);
    }

    @Override
    public <T extends AlipayResponse> T execute(AlipayRequest<T> request, String accessToken) throws AlipayApiException {
        return execute(request, accessToken, null, null);
    }

    @Override
    public <T extends AlipayResponse> T execute(AlipayRequest<T> request, String accessToken, String appAuthToken) throws AlipayApiException {
        return execute(request, accessToken, appAuthToken, null);
    }

    @Override
    public <T extends AlipayResponse> T execute(AlipayRequest<T> request, String accessToken, String appAuthToken, String targetAppId) throws AlipayApiException {
        if (log.isInfoEnabled()) {
            log.info("pid={}, method={}, accessToken={}, appAuthToken={}, targetAppId={}", this.appId,
                request.getApiMethodName(), accessToken, appAuthToken, targetAppId);
        }

        final String rawResp = post(request, accessToken, appAuthToken, targetAppId);
        final RespBodyVo respBody = getRespBody(request, rawResp);
        if (StringUtils.isBlank(respBody.getRealBody())) {
            throw new RuntimeException(String.format("respBody is blank. rawResp = %s", rawResp));
        }

        final String response = respBody.getRealBody();
        log.info("respBody = {}", response);
        debugJson(response);

        final T result = AlipayJsonUtils.fromJson(response, request.getResponseClass());
        result.setBody(response);

        if (StringUtils.equals(result.getCode(), AlipayCodeEnum.ILLEGAL_ARGUMENT.getCode())
            && StringUtils.equals(result.getSubCode(), ISV_INVALID_SIGNATURE)) {
            log.warn("签名错误 {}", result);
            return result;
        }

        final String signStr = respBody.getSign();
        log.info("signStr = {}", signStr);
        if (StringUtils.isNotBlank(signStr) && !getSignChecker().check(respBody.getBody(), signStr, this.signType, this.charset)) {
            throw new AlipayApiException(String.format("sign error. rawResp = %s", rawResp));
        }
        return result;
    }

    private RespBodyVo getRespBody(AlipayRequest<?> request, String responseString) {
        final String errResp = AlipayJsonUtils.getRawValue(responseString, AlipayConstants.ERROR_RESPONSE);
        final String signStr = AlipayJsonUtils.getRawValue(responseString, AlipayConstants.SIGN);

        final RespBodyVo ret = new RespBodyVo();
        ret.setSign(signStr);
        if (StringUtils.isNotBlank(errResp)) {
            ret.setBody(errResp);
            ret.setRealBody(errResp);
            return ret;
        } else {
            final String respName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, request.getResponseClass().getSimpleName());
            if (log.isInfoEnabled()) {
                log.info("respName={}, respClass={}", respName, request.getResponseClass());
            }
            final String body = AlipayJsonUtils.getRawValue(responseString, respName);
            if (StringUtils.startsWith(body, "{")) {
                ret.setBody(body);
                ret.setRealBody(body);
                return ret;
            }
            Objects.requireNonNull(this.decryptor);
            Objects.requireNonNull(this.encryptType);
            ret.setBody("\"" + body + "\"");
            ret.setRealBody(this.decryptor.decrypt(body, this.encryptType, this.charset));
            return ret;
        }
    }

    protected abstract String post(AlipayRequest<?> request, String accessToken, String appAuthToken, String targetAppId) throws AlipayApiException;

    protected Map<String, String> getBizParam(AlipayRequest<?> request, String accessToken, String appAuthToken, String targetAppId) throws AlipayApiException {
        final Map<String, String> params = new HashMap<>(32);
        // biz-info
        params.putAll(request.getTextParams());
        if (StringUtils.isBlank(params.get(AlipayConstants.BIZ_CONTENT_KEY)) && request.getBizModel() != null) {
            params.put(AlipayConstants.BIZ_CONTENT_KEY, AlipayJsonUtils.toJson(request.getBizModel()));
        }
        if (StringUtils.isNotBlank(appAuthToken)) {
            params.put(AlipayConstants.APP_AUTH_TOKEN, appAuthToken);
        }
        debugJson(params.get(AlipayConstants.BIZ_CONTENT_KEY));

        if (request.isNeedEncrypt()) {
            String bizContent = params.get(AlipayConstants.BIZ_CONTENT_KEY);
            if (StringUtils.isBlank(bizContent)) {
                throw new AlipayApiException("当前API不支持加密请求");
            }
            // 需要加密必须设置密钥和加密算法
            if (StringUtils.isAnyBlank(this.encryptType, this.encryptKey) || this.encryptor == null) {
                throw new AlipayApiException("API请求要求加密，则必须设置密钥类型和加密器");
            }
            String encryptContent = encryptor.encrypt(bizContent, this.encryptType, this.charset);
            params.put(AlipayConstants.BIZ_CONTENT_KEY, encryptContent);
            params.put(AlipayConstants.ENCRYPT_TYPE, this.encryptType);
        }

        // protocal-Must-Params
        params.put(AlipayConstants.METHOD, request.getApiMethodName());
        params.put(AlipayConstants.VERSION, request.getApiVersion());
        params.put(AlipayConstants.APP_ID, this.appId);
        params.put(AlipayConstants.SIGN_TYPE, this.signType);
        params.put(AlipayConstants.TERMINAL_TYPE, request.getTerminalType());
        params.put(AlipayConstants.TERMINAL_INFO, request.getTerminalInfo());
        params.put(AlipayConstants.NOTIFY_URL, request.getNotifyUrl());
        params.put(AlipayConstants.RETURN_URL, request.getReturnUrl());
        params.put(AlipayConstants.CHARSET, charset);
        params.put(AlipayConstants.TIMESTAMP, AlipayDateUtils.getDateTimeString(new Date()));
        if (StringUtils.isNotBlank(targetAppId)) {
            params.put(AlipayConstants.TARGET_APP_ID, targetAppId);
        }

        // protocal-Opt-Params
        params.put(AlipayConstants.FORMAT, this.format);
        if (StringUtils.isNotBlank(accessToken)) {
            params.put(AlipayConstants.ACCESS_TOKEN, accessToken);
        }
        params.put(AlipayConstants.PROD_CODE, request.getProdCode());
        // params.put(AlipayConstants.ALIPAY_SDK, AlipayConstants.SDK_VERSION);

        if (StringUtils.isNotBlank(this.signType)) {
            final String signContent = getSignContent(MapUtils.unmodifiableMap(params));
            log.info("content = {}", signContent);
            params.put(AlipayConstants.SIGN, getSigner().sign(signContent, this.signType, this.charset));
        } else {
            params.put(AlipayConstants.SIGN, StringUtils.EMPTY);
        }
        return params;
    }

    private static String getSignContent(Map<String, String> params) {
        TreeSet<String> keys = new TreeSet<>(params.keySet());
        StringBuilder sb = new StringBuilder();
        boolean hasParam = false;
        for (String key : keys) {
            if (StringUtils.equals(AlipayConstants.SIGN, key)) {
                continue;
            }
            String str = params.get(key);
            if (StringUtils.isBlank(str)) {
                continue;
            }
            if (hasParam) {
                sb.append("&");
            } else {
                hasParam = true;
            }
            sb.append(key).append("=").append(str);
        }
        return sb.toString();
    }

    protected static List<NameValuePair> getNameValuePairs(Map<String, String> params) {
        final List<NameValuePair> nameValuePairs = new ArrayList<>(params.size());
        params.forEach((k, v) -> {
            if (StringUtils.isNotBlank(v)) {
                nameValuePairs.add(new BasicNameValuePair(k, v));
            }
        });
        return nameValuePairs;
    }

    protected String httpClientPost(Request req) throws IOException {
        final HttpResponse returnResponse = req.execute().returnResponse();
        final String responseString = EntityUtils.toString(returnResponse.getEntity(), this.charset);
        if (log.isInfoEnabled()) {
            log.info("code = {}, rawResp = {}", returnResponse.getStatusLine().getStatusCode(), responseString);
        }
        if (log.isDebugEnabled()) {
            log.debug("rawResp = {}", AlipayJsonUtils.toPrettyFormat(responseString));
        }
        return responseString;
    }

    public Signer getSigner() {
        return this.signer;
    }

    public SignChecker getSignChecker() {
        return this.signChecker;
    }

    @Override
    public <T extends AlipayResponse> T sdkExecute(AlipayRequest<T> request) throws AlipayApiException {
        if (log.isInfoEnabled()) {
            log.info("pid={}, method={}", this.appId, request.getApiMethodName());
        }
        final Map<String, String> bizParam = getBizParam(request, null, null, null);
        T ret = Reflect.onClass(request.getResponseClass()).create().get();
        ret.setBody(buildQueryParam(bizParam));
        return ret;
    }

    @Override
    public <T extends AlipayResponse> T pageExecute(AlipayRequest<T> request) throws AlipayApiException {
        return pageExecute(request, "POST");
    }

    @Override
    public <T extends AlipayResponse> T pageExecute(AlipayRequest<T> request, String method) throws AlipayApiException {
        if (log.isInfoEnabled()) {
            log.info("pid={}, method={}, httpMethod={}", this.appId, request.getApiMethodName(), method);
        }
        final Map<String, String> bizParam = getBizParam(request, null, null, null);
        T ret = Reflect.onClass(request.getResponseClass()).create().get();

        if (StringUtils.equalsIgnoreCase(method, "GET")) {
            ret.setBody(serverUrl + "?" + buildQueryParam(bizParam));
            return ret;
        } else {
            ret.setBody(WebUtils.buildForm(serverUrl, bizParam));
            return ret;
        }
    }

    private String buildQueryParam(Map<String, String> bizParam) {
        final Set<String> keys = new TreeSet<>(bizParam.keySet());
        final StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = bizParam.get(key);
            if (StringUtils.isNotBlank(value)) {
                sb.append(key).append("=").append(encode(value)).append("&");
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    @SneakyThrows
    private String encode(String value) {
        return URLEncoder.encode(value, this.charset);
    }

    @Override
    public <TR extends AlipayResponse, T extends AlipayRequest<TR>> TR parseAppSyncResult(Map<String, String> result, Class<T> requsetClazz) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

    @Override
    public BatchAlipayResponse execute(BatchAlipayRequest request) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

    @Override
    public <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

    @Override
    public <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request, String authToken) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

    @Override
    public <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request, String accessToken, String appAuthToken) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

    @Override
    public <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request, String accessToken, String appAuthToken, String targetAppId) throws AlipayApiException {
        throw new NotImplementedException("not support yet");
    }

}
