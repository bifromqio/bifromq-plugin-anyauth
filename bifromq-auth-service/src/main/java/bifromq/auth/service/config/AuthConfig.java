package bifromq.auth.service.config;

import bifromq.auth.service.storage.IAuthStorage;

public class AuthConfig {
    private int bossThreads = 1;
    private int workerThreads = Runtime.getRuntime().availableProcessors() / 2;
    private String host = "0.0.0.0";
    private int port = 8080;
    private IAuthStorage.Type type = IAuthStorage.Type.Dummy;
    private MySQlStorageConfig mySqlStorageConfig;

    public class MySQlStorageConfig {
        private String jdbcUrl;
        private String username;
        private String password;

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public IAuthStorage.Type getType() {
        return type;
    }

    public void setType(IAuthStorage.Type type) {
        this.type = type;
    }

    public MySQlStorageConfig getMySqlStorageConfig() {
        return mySqlStorageConfig;
    }

    public void setMySqlStorageConfig(MySQlStorageConfig mySqlStorageConfig) {
        this.mySqlStorageConfig = mySqlStorageConfig;
    }
}
