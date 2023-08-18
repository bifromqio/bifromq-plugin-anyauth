package bifromq.auth.service.storage;

import bifromq.auth.service.model.Credential;
import bifromq.auth.service.model.UserAction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public interface IAuthStorage {
    enum Type {
        Dummy,
        MySql
    }

    IAuthStorage Dummy_Storage = new IAuthStorage() {
        Optional<List<String>> rules = Optional.of(Arrays.asList("#"));
        @Override
        public CompletableFuture<Optional<Credential>> getUserCredential(String username) {
            return CompletableFuture.completedFuture(Optional.of(Credential.Instance));
        }

        @Override
        public CompletableFuture<Optional<List<String>>> getUserRoles(String username, UserAction.Action action) {
            return CompletableFuture.completedFuture(rules);
        }
    };

    CompletableFuture<Optional<Credential>> getUserCredential(String username);

    CompletableFuture<Optional<List<String>>> getUserRoles(String username, UserAction.Action action);
}
