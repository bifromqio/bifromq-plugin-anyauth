package bifromq.auth.service.model;

import org.mindrot.jbcrypt.BCrypt;

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

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Credential that = (Credential) o;
        return java.util.Objects.equals(salt, that.salt)
                && java.util.Objects.equals(hashedPassword, that.hashedPassword);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(salt, hashedPassword);
    }

    @Override
    public String toString() {
        return "Credential{"
                + "salt='" + salt + '\''
                + ", hashedPassword='" + hashedPassword + '\''
                + '}';
    }
}
