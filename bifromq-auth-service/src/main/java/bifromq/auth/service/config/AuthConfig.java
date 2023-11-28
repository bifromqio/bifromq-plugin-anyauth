package bifromq.auth.service.config;

import bifromq.auth.service.storage.IAuthStorage;
import lombok.Data;

@Data
public class AuthConfig {
    private int bossThreads = 1;
    private int workerThreads = Runtime.getRuntime().availableProcessors() / 2;
    private String host = "0.0.0.0";
    private int port = 8080;
    private IAuthStorage.Type type = IAuthStorage.Type.Dummy;
    private MySQlStorageConfig mySqlStorageConfig;

    @Data
    public class MySQlStorageConfig {
        private String jdbcUrl;
        private String username;
        private String password;
    }
}
