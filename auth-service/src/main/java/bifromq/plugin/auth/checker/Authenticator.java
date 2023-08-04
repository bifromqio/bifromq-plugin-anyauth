package bifromq.plugin.auth.checker;

import bifromq.plugin.auth.model.Credential;
import bifromq.plugin.auth.storage.IAuthStorage;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class Authenticator {
    private IAuthStorage storage;

    public Authenticator(IAuthStorage storage) {
        this.storage = storage;
    }

    public CompletableFuture<Boolean> auth(String username, String originalPassword) {
        return storage.getUserCredential(username)
                .thenApply(optionalCredential -> {
                    if (!optionalCredential.isPresent()) {
                        return false;
                    }else {
                        Credential credential = optionalCredential.get();
                        return verify(originalPassword, credential);
                    }
                });
    }

    private boolean verify(String originalPassword, Credential credential) {
        String salt = credential.getSalt();
        String hashedInputPassword = BCrypt.hashpw(originalPassword, salt);
        return hashedInputPassword.equals(credential.getHashedPassword());
    }

}
