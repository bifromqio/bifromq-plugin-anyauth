package bifromq.auth.service.checker;

import bifromq.auth.service.model.UserAction;
import bifromq.auth.service.storage.IAuthStorage;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;

public class AuthorizerTest {
    @Mock
    private IAuthStorage storage;
    private Authorizer authorizer;
    private AutoCloseable closeable;
    private String username = "dev";

    @BeforeMethod
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        authorizer = new Authorizer(storage);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testPubWithNonWildcardRule() {
        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/b/c"))));
        boolean checkResult = authorizer.check(username, "a/b/c", UserAction.Action.Pub).join();
        Assert.assertTrue(checkResult);

        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/b/c"))));
        checkResult = authorizer.check(username, "a/b/c/d", UserAction.Action.Pub).join();
        Assert.assertTrue(!checkResult);
    }

    @Test
    public void testPubWithEmptyRule() {
        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.empty()));
        boolean checkResult = authorizer.check(username, "a/b/c", UserAction.Action.Pub).join();
        Assert.assertTrue(!checkResult);
    }

    @Test
    public void testPubWithWildcardRule() {
        List<String> rules = new ArrayList<>() {{
            add("+/b/c");
            add("a/+/c");
            add("a/b/+");
            add("a/+/+");
            add("+/+/c");
            add("+/b/+");
            add("+/+/+");
            add("a/+/#");
            add("+/+/#");
            add("a/#");
            add("+/#");
            add("#");
        }};
        rules.forEach(rule -> {
            when(storage.getUserRoles(username, UserAction.Action.Pub))
                    .thenReturn(CompletableFuture
                            .completedFuture(Optional.of(Arrays.asList(rule))));
            boolean checkResult = authorizer.check(username, "a/b/c", UserAction.Action.Pub).join();
            Assert.assertTrue(checkResult);
        });
    }

    @Test
    public void testSubTopicFilterWithWildcardRule() {
        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("+/#"))));
        boolean checkResult = authorizer.check(username, "a/#", UserAction.Action.Pub).join();
        Assert.assertTrue(checkResult);

        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/#"))));
        checkResult = authorizer.check(username, "+/#", UserAction.Action.Pub).join();
        Assert.assertTrue(!checkResult);

        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/#"))));
        checkResult = authorizer.check(username, "a/+/#", UserAction.Action.Pub).join();
        Assert.assertTrue(checkResult);

        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/+/#"))));
        checkResult = authorizer.check(username, "a/+/+", UserAction.Action.Pub).join();
        Assert.assertTrue(checkResult);

        when(storage.getUserRoles(username, UserAction.Action.Pub))
                .thenReturn(CompletableFuture
                        .completedFuture(Optional.of(Arrays.asList("a/#"))));
        checkResult = authorizer.check(username, "a", UserAction.Action.Pub).join();
        Assert.assertTrue(checkResult);
    }
}
