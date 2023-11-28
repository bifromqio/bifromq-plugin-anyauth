package bifromq.auth.plugin;

import bifromq.auth.plugin.worker.Authenticator;
import bifromq.auth.plugin.worker.Authorizer;
import com.baidu.bifromq.plugin.authprovider.IAuthProvider;
import com.baidu.bifromq.plugin.authprovider.type.MQTT3AuthData;
import com.baidu.bifromq.plugin.authprovider.type.MQTT3AuthResult;
import com.baidu.bifromq.plugin.authprovider.type.MQTTAction;
import com.baidu.bifromq.type.ClientInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pf4j.Extension;

import java.util.concurrent.CompletableFuture;

import static com.baidu.bifromq.type.MQTTClientInfoConstants.MQTT_USER_ID_KEY;

@Extension
@Slf4j
public final class AuthProvider implements IAuthProvider {
    private Authenticator authenticator;
    private Authorizer authorizer;
    private HttpClient httpClient = HttpClientBuilder.create().build();
    private ObjectMapper objectMapper = new ObjectMapper();

    public AuthProvider() {
        this.authenticator = new Authenticator(httpClient, objectMapper);
        this.authorizer = new Authorizer(httpClient, objectMapper);
    }
    @Override
    public CompletableFuture<MQTT3AuthResult> auth(MQTT3AuthData authData) {
        return authenticator.auth(authData);
    }

    @Override
    public CompletableFuture<Boolean> check(ClientInfo client, MQTTAction action) {
        String username = client.getMetadataMap().get(MQTT_USER_ID_KEY);
        return authorizer.check(username, action);
    }
}
