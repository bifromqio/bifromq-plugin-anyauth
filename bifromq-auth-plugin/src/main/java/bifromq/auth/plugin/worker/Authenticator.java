package bifromq.auth.plugin.worker;

import bifromq.auth.plugin.util.ConfigUtils;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baidu.bifromq.plugin.authprovider.type.MQTT3AuthData;
import com.baidu.bifromq.plugin.authprovider.type.MQTT3AuthResult;
import com.baidu.bifromq.plugin.authprovider.type.Ok;
import com.baidu.bifromq.plugin.authprovider.type.Reject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Authenticator {
    private String domain = ConfigUtils.getAuthProviderConfig().getAuth0().getDomain();
    private String tenantId = ConfigUtils.getAuthProviderConfig().getTenantId();
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    enum AuthType {
        Device,
        Auth0,
        WeChat,
        Unknown
    }

    public Authenticator(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    class UserInfo {
        AuthType authType;
        String realUsername;
    }

    public CompletableFuture<MQTT3AuthResult> auth(MQTT3AuthData authData) {
        CompletableFuture<MQTT3AuthResult> future;
        UserInfo userInfo = getUserInfo(authData.getUsername());
        switch (userInfo.authType) {
            case Device:
                future = deviceVerify(userInfo.realUsername, authData.getPassword().toStringUtf8());
                break;
            case Auth0:
                future = auth0Verify(authData.getPassword().toStringUtf8());
                break;
            case WeChat:
                future = weChatVerify(authData.getPassword().toStringUtf8());
                break;
            default:
                log.warn("unknown auth type, username: {}", authData.getUsername());
                future = CompletableFuture.completedFuture(MQTT3AuthResult.newBuilder()
                        .setReject(Reject.newBuilder()
                                .setCode(Reject.Code.BadPass)
                                .build())
                        .build());
        }
        return future;
    }

    private UserInfo getUserInfo(String username) {
        UserInfo userInfo = new UserInfo();
        String[] usernameInfo = username.split(":");
        try {
            switch (usernameInfo.length) {
                case 1:
                    userInfo.authType = AuthType.Device;
                    userInfo.realUsername = usernameInfo[0];
                    break;
                case 2:
                    userInfo.authType = AuthType.valueOf(usernameInfo[0]);
                    userInfo.realUsername = usernameInfo[1];
                    break;
                default:
                    userInfo.authType = AuthType.Unknown;
            }
        }catch (Exception exception) {
            log.error("get userInfo error: ", exception);
            userInfo.authType = AuthType.Unknown;
        }
        return userInfo;
    }

    private CompletableFuture<MQTT3AuthResult> deviceVerify(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            MQTT3AuthResult.Builder builder = MQTT3AuthResult.newBuilder();
            try {
                HttpPost post = new HttpPost(ConfigUtils.getAuthProviderConfig().getDevice().getAuthUrl());
                StringEntity entity = new StringEntity(String.format("{\"username\":\"%s\", \"password\":\"%s\"}",
                        username, password));
                post.setEntity(entity);
                post.setHeader("Content-Type", "application/json");
                HttpResponse response = httpClient.execute(post);

                if (parseAuthResult(response.getEntity())) {
                    builder.setOk(Ok.newBuilder()
                            .setTenantId(tenantId)
                            .setUserId(username)
                            .build());
                }else {
                    builder.setReject(Reject.newBuilder()
                            .setCode(Reject.Code.NotAuthorized)
                            .build());
                }
            } catch (Exception e) {
                log.warn("request auth service failed: ", e);
                builder.setReject(Reject.newBuilder()
                                .setCode(Reject.Code.Error)
                                .build());
            }
            return builder.build();
        });
    }

    private boolean parseAuthResult(HttpEntity responseEntity) throws IOException {
        String jsonString = EntityUtils.toString(responseEntity);
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        return jsonNode.get("auth").asBoolean();
    }

    private CompletableFuture<MQTT3AuthResult> auth0Verify(String token) {
        return CompletableFuture.supplyAsync(() -> {
            MQTT3AuthResult.Builder resultBuilder = MQTT3AuthResult.newBuilder();
            JwkProvider provider = new UrlJwkProvider(domain);
            try {
                DecodedJWT jwt = JWT.decode(token);
                Jwk jwk = provider.get(jwt.getKeyId());
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                JWTVerifier verifier = JWT.require(algorithm)
                        .withIssuer(domain)
                        .build();
                Map<String, Claim> claims = verifier.verify(token).getClaims();
                if (claims.get("exp").asDate().compareTo(new Date()) < 0) {
                    log.debug("token has expired for domain: {}", domain);
                    resultBuilder.setReject(Reject.newBuilder()
                            .setCode(Reject.Code.BadPass)
                            .build());
                }else {
                    resultBuilder.setOk(Ok.newBuilder()
                            .setTenantId(ConfigUtils.getAuthProviderConfig().getTenantId())
                            .setUserId(claims.get("sub").asString())
                            .build());
                }
            } catch (Exception e){
                log.error("invalid signature/claims: ", e);
                resultBuilder.setReject(Reject.newBuilder()
                        .setCode(Reject.Code.NotAuthorized)
                        .build());
            }
            return resultBuilder.build();
        });
    }

    private CompletableFuture<MQTT3AuthResult> weChatVerify(String jsCode) {
        return CompletableFuture.supplyAsync(() -> {
            MQTT3AuthResult.Builder builder = MQTT3AuthResult.newBuilder();
            String grantType = "authorization_code";
            String accessToken = "";

            HttpGet httpGet = new HttpGet("https://api.weixin.qq.com/sns/jscode2session?appid=" +
                    ConfigUtils.getAuthProviderConfig().getWechat().getAppId() + "&secret=" +
                    ConfigUtils.getAuthProviderConfig().getWechat().getAppSecret() + "&js_code=" +
                    jsCode + "&grant_type=" + grantType + "&access_token=" + accessToken);

            httpGet.addHeader("Accept", "application/json, text/plain, */*");
            httpGet.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            httpGet.addHeader("Content-Type", "application/json");

            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(responseString);

                String openid = jsonNode.get("openid").asText();
                builder.setOk(Ok.newBuilder()
                        .setTenantId(ConfigUtils.getAuthProviderConfig().getTenantId())
                        .setUserId(openid)
                        .build());
            } catch (IOException e) {
                log.error("failed to verify code: ", e);
                builder.setReject(Reject.newBuilder()
                        .setCode(Reject.Code.Error)
                        .build());
            }
            return builder.build();
        });
    }
}
