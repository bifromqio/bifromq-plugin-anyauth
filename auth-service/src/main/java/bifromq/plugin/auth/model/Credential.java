package bifromq.plugin.auth.model;

import lombok.Data;

@Data
public class Credential {
    private String salt;
    private String hashedPassword;
    public static Credential Instance = new Credential("", "");
    public Credential(String hashedPassword, String salt) {
        this.hashedPassword = hashedPassword;
        this.salt = salt;
    }
}
