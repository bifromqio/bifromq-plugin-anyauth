package bifromq.plugin.auth.model;

import lombok.Data;

@Data
public class UserAction {

    public enum Action {
        Pub,
        Sub
    }
    String username;
    Action action;
    public UserAction(String username, Action action) {
        this.username = username;
        this.action = action;
    }
}
