package com.example.myApp.demos.util;
import javax.crypto.SecretKey;

import com.example.myApp.demos.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    // 1. 核心配置：密钥（关键！0.11.5 要求密钥长度符合对应算法要求，HS256 要求至少 256 位（32 个字符））
    private static final String SECRET_KEY_STR = "myJwtSecretKey_2026_32chars_length_1234";
    // 转换为 jjwt 要求的 SecretKey 类型（推荐使用 Keys 工具类生成，避免密钥格式错误）
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STR.getBytes());
    // 2. Token 过期时间配置（示例：2 小时，单位：毫秒）
    private static final long EXPIRATION_TIME = 2 * 60 * 60 * 1000L;

    /**
     * 生成 JWT Token
     * @param user 自定义载荷数据（示例：用户名，可扩展为用户 ID、角色等）
     * @return 生成的有效 Token 字符串
     */
    public static String generateToken(User user) {
        // 可选：添加额外的自定义载荷（如用户角色、用户 ID 等）
        Map<String, Object> claims = new HashMap<>();
        String username = user.getName();
        claims.put("userName", username); // 示例：存入用户角色
        claims.put("userId", user.getUserId()); // 示例：存入用户 ID
        claims.put("role", user.getRole());

        // 3. 核心：使用 Jwts.builder() 构建 Token（0.11.5 核心构建流程）
        return Jwts.builder()
                // 步骤1：设置自定义载荷（Claims），可多个键值对
                .setClaims(claims)
                // 步骤2：设置主题（Subject），通常存储唯一标识（如用户名）
                .setSubject(username)
                // 步骤3：设置 Token 签发时间（Issued At）
                .setIssuedAt(new Date())
                // 步骤4：设置 Token 过期时间（Expiration）
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // 步骤5：设置签名算法和密钥（0.11.5 推荐使用 SecretKey 类型，而非直接传入字符串）
                .signWith(SECRET_KEY)
                // 步骤6：构建并压缩为字符串
                .compact();
    }

    // 可选：添加 Token 验证、解析方法（辅助验证生成结果的有效性）
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // 可根据具体异常类型（过期、签名错误等）做细分处理
            throw new RuntimeException("Token 解析失败：" + e.getMessage());
        }
    }
}
