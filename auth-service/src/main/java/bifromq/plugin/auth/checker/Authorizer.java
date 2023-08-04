package bifromq.plugin.auth.checker;

import bifromq.plugin.auth.model.UserAction;
import bifromq.plugin.auth.storage.IAuthStorage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Authorizer {
    private String single = "+";
    public String multiple = "#";
    private IAuthStorage storage;

    public Authorizer(IAuthStorage storage) {
        this.storage = storage;
    }

    public CompletableFuture<Boolean> check(String username, String topic, UserAction.Action action) {
        return storage.getUserRoles(username, action)
                .thenApply(optionalList -> {
                    if (!optionalList.isPresent()) {
                        return false;
                    }else {
                        return match(topic, optionalList.get());
                    }
                });
    }

    private boolean match(String topic, List<String> rules) {
        String[] topicLevels = topic.split("/");
        for (int index = 0; index < rules.size(); index++) {
            if (matchRule(topicLevels, rules.get(index).split("/"))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchRule(String[] topicLevels, String[] ruleLevels) {
        for (int level = 0; level < ruleLevels.length; level++) {
            String eachLevel = ruleLevels[level];
            if (!eachLevel.equals(single) && !eachLevel.equals(multiple)) {
                if (!topicLevels[level].equals(eachLevel)) {
                    return false;
                }
            } else if (eachLevel.equals(single) && topicLevels[level].equals(multiple)) {
                return false;
            } else if (eachLevel.equals(multiple)) {
                return true;
            }
        }
        if (topicLevels.length > ruleLevels.length) {
            return false;
        }
        return true;
    }
}
