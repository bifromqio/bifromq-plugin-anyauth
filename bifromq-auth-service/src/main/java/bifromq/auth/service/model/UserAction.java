package bifromq.auth.service.model;

public class UserAction {

    public enum Action {
        Pub,
        Sub
    }
    private String username;
    private Action action;
    public UserAction(String username, Action action) {
        this.username = username;
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserAction userAction = (UserAction) o;
        return java.util.Objects.equals(username, userAction.username)
                && action == userAction.action;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(username, action);
    }

    @Override
    public String toString() {
        return "UserAction{"
                + "username='" + username + '\''
                + ", action=" + action
                + '}';
    }
}
