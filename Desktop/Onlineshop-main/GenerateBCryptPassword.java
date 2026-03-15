import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptPassword {
    public static void main(String[] args) {
        // 创建BCrypt密码编码器
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 要加密的密码
        String rawPassword = "password";
        
        // 生成BCrypt密码
        String encodedPassword = encoder.encode(rawPassword);
        
        // 验证密码是否匹配
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        
        // 输出结果
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt密码: " + encodedPassword);
        System.out.println("密码匹配: " + matches);
        System.out.println("\n将此BCrypt密码更新到数据库中");
    }
}