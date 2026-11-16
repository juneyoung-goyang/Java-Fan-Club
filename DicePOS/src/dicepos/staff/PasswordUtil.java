package dicepos.staff;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** 비밀번호를 솔트와 함께 SHA-256으로 바꾼다. 원래 비밀번호는 어디에도 저장하지 않는다. */
public class PasswordUtil {

    /** 직원마다 다른 16자리 무작위 솔트 */
    public static String newSalt() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    /** SHA-256(솔트 + 비밀번호) → 64자리 16진수 */
    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] result = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return toHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // SHA-256은 모든 Java에 있으므로 일어나지 않는다
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
