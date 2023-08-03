package bifromq.plugin.auth;

import bifromq.plugin.util.ConfigUtils;
import com.baidu.bifromq.plugin.authprovider.type.MQTTAction;
import com.baidu.bifromq.plugin.authprovider.type.PubAction;
import com.baidu.bifromq.plugin.authprovider.type.SubAction;
import com.baidu.bifromq.plugin.authprovider.type.UnsubAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Authorizer {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Authorizer(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<Boolean> check(String username, MQTTAction action) {
        return CompletableFuture.supplyAsync(() -> {
            HttpPost post = new HttpPost(ConfigUtils.getAuthProviderConfig().getDevice().getCheckUrl());
            boolean checkResult = false;
            String checkAction = null;
            String topic = null;
            try {
                if (action.hasPub()) {
                    PubAction pubAction = action.getPub();
                    checkAction = "Pub";
                    topic = pubAction.getTopic();
                }else if (action.hasSub()) {
                    SubAction subAction = action.getSub();
                    checkAction = "Sub";
                    topic = subAction.getTopicFilter();
                }else if (action.hasUnsub()) {
                    UnsubAction unsubAction = action.getUnsub();
                    checkAction = "Sub";
                    topic = unsubAction.getTopicFilter();
                }
                StringEntity entity = new StringEntity(String.format("{\"username\":\"%s\",\"topic\":\"%s\", " +
                                "\"action\": \"%s\"}", username, topic, checkAction));
                post.setEntity(entity);
                post.setHeader("Content-Type", "application/json");
                HttpResponse response = httpClient.execute(post);

                if (parseAuthResult(response.getEntity())) {
                    checkResult = true;
                }
            }catch (Exception exception) {
                log.warn("failed to check: ", exception);
            }
            return checkResult;
        });
    }

    private boolean parseAuthResult(HttpEntity responseEntity) throws IOException {
        String jsonString = EntityUtils.toString(responseEntity);
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        return jsonNode.get("check").asBoolean();
    }
}
