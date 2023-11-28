package bifromq.auth.service.model;

import lombok.Data;
import org.mindrot.jbcrypt.BCrypt;

@Data
public class Credential {
    private String salt;
    private String hashedPassword;
    public static Credential Instance = new Credential();
    public Credential(String hashedPassword, String salt) {
        this.hashedPassword = hashedPassword;
        this.salt = salt;
    }

    private Credential() {
        this.salt = BCrypt.gensalt();
        this.hashedPassword = BCrypt.hashpw("dev", salt);
    }
}
