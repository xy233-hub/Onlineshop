import java.security.SecureRandom;

public class SimpleBCrypt {
    private static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789./";
    private static final int GENSALT_DEFAULT_LOG2_ROUNDS = 10;
    private static final int BCRYPT_SALT_LEN = 16;
    
    public static void main(String[] args) {
        String password = "password";
        String hashed = hashpw(password, gensalt());
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt哈希: " + hashed);
        System.out.println("验证: " + checkpw(password, hashed));
    }
    
    public static boolean checkpw(String plaintext, String hashed) {
        return equalsNoEarlyReturn(hashed, hashpw(plaintext, hashed));
    }
    
    public static String hashpw(String password, String salt) {
        // 简化的BCrypt实现，仅用于生成符合格式的哈希
        // 实际项目中应使用完整的BCrypt库
        // 这里生成一个符合BCrypt格式的哈希字符串
        StringBuilder sb = new StringBuilder();
        sb.append("$2a$10$");
        
        // 生成随机盐值
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[BCRYPT_SALT_LEN];
        random.nextBytes(saltBytes);
        
        // 转换为Base64格式
        String saltStr = encodeBase64(saltBytes, saltBytes.length);
        sb.append(saltStr.substring(0, 22));
        
        // 添加模拟的哈希部分
        sb.append("AAAAAAAAAAAAAAAAAAAAAA");
        
        return sb.toString();
    }
    
    public static String gensalt() {
        return gensalt(GENSALT_DEFAULT_LOG2_ROUNDS);
    }
    
    public static String gensalt(int log_rounds) {
        StringBuilder salt = new StringBuilder();
        SecureRandom random = new SecureRandom();
        byte[] rnd = new byte[BCRYPT_SALT_LEN];
        
        random.nextBytes(rnd);
        
        salt.append("$2a$");
        if (log_rounds < 10) {
            salt.append("0");
        }
        salt.append(log_rounds);
        salt.append("$");
        salt.append(encodeBase64(rnd, rnd.length));
        
        return salt.toString();
    }
    
    private static String encodeBase64(byte[] d, int len) {
        StringBuilder rs = new StringBuilder();
        int off = 0;
        int c1, c2;
        
        while (off < len) {
            c1 = d[off++] & 0xff;
            rs.append(SALT_CHARS.charAt(c1 >> 2));
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(SALT_CHARS.charAt(c1));
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= c2 >> 4;
            rs.append(SALT_CHARS.charAt(c1));
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(SALT_CHARS.charAt(c1));
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= c2 >> 6;
            rs.append(SALT_CHARS.charAt(c1));
            rs.append(SALT_CHARS.charAt(c2 & 0x3f));
        }
        
        return rs.toString();
    }
    
    private static boolean equalsNoEarlyReturn(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}