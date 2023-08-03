package bifromq.plugin.auth.storage;

import bifromq.plugin.auth.config.AuthConfig;
import bifromq.plugin.auth.model.Credential;
import bifromq.plugin.auth.model.UserAction;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MySqlStorage implements IAuthStorage {
    private final HikariDataSource ds;
    private final AsyncLoadingCache<String, Optional<Credential>> userCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .buildAsync(this::queryUserCredential);

    private final AsyncLoadingCache<UserAction, Optional<List<String>>> aclCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .buildAsync(this::queryAcl);

    public MySqlStorage(AuthConfig.MySQlStorageConfig mysqlConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mysqlConfig.getJdbcUrl());
        config.setUsername(mysqlConfig.getUsername());
        config.setPassword(mysqlConfig.getPassword());
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }
    @Override
    public CompletableFuture<Optional<Credential>> getUserCredential(String username) {
        return userCache.get(username);
    }

    @Override
    public CompletableFuture<Optional<List<String>>> getUserRoles(String username, UserAction.Action action) {
        return aclCache.get(new UserAction(username, action));
    }

    private Optional<Credential> queryUserCredential(String username) {
        try {
            Connection conn = ds.getConnection();
            String sql = "SELECT password, salt FROM user WHERE username = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String hashedPassword = resultSet.getString("password");
                String salt = resultSet.getString("salt");
                return Optional.of(new Credential(hashedPassword, salt));
            }else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("sql exception: ", e);
            return Optional.empty();
        }
    }

    private Optional<List<String>> queryAcl(UserAction userAction) {
        try {
            Connection conn = ds.getConnection();
            String sql;
            String column;
            if (userAction.getAction() == UserAction.Action.Pub) {
                sql = "SELECT pubAcl FROM rule WHERE username = ?";
                column = "pubAcl";
            }else {
                sql = "SELECT subAcl FROM rule WHERE username = ?";
                column = "subAcl";
            }
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userAction.getUsername());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String rules = resultSet.getString(column);
                return Optional.of(Arrays.asList(rules.split(",")));
            }else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("sql exception: ", e);
            return Optional.empty();
        }
    }
}
