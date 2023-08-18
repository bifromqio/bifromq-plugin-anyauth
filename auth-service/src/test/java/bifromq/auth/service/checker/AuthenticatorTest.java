package bifromq.auth.service.checker;

import bifromq.auth.service.model.Credential;
import bifromq.auth.service.storage.IAuthStorage;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthenticatorTest {
    @Mock
    private IAuthStorage storage;
    private Authenticator authenticator;
    private AutoCloseable closeable;
    private String username = "dev";

    @BeforeMethod
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        authenticator = new Authenticator(storage);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testPasswordMatch() {
        String plainPassword = "dev";
        String salt = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(plainPassword, salt);
        when(storage.getUserCredential(anyString()))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(new Credential(hashedPassword, salt))));
        boolean result = authenticator.auth(username, plainPassword).join();
        Assert.assertTrue(result);
    }

    @Test
    public void testPasswordNotMatch() {
        String plainPassword = "dev";
        String salt = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(plainPassword, salt);
        when(storage.getUserCredential(anyString()))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(new Credential(hashedPassword, salt))));
        boolean result = authenticator.auth(username, plainPassword + "-").join();
        Assert.assertTrue(!result);
    }

    @Test
    public void testEmptyCredential() {
        String plainPassword = "dev";
        when(storage.getUserCredential(anyString()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        boolean result = authenticator.auth(username, plainPassword).join();
        Assert.assertTrue(!result);
    }

    @Test
    public void testEmptyPassword() {
        String plainPassword = "";
        String salt = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(plainPassword, salt);
        when(storage.getUserCredential(anyString()))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(new Credential(hashedPassword, salt))));
        boolean result = authenticator.auth(username, plainPassword).join();
        Assert.assertTrue(result);
    }
}
