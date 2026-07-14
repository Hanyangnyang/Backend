package life.hanyang.core.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    public String createToken(String subject, String role, String secretKeyString, long expirationMs) {
        SecretKey key = getSigningKey(secretKeyString);
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    // JWT 토큰의 payload에서 sub값(사용자 ID)를 꺼내는 메서드
    public String getSubject(String token, String secretKeyString) {
        return getClaims(token, secretKeyString).getSubject();
    }

    // JWT 토큰에서 Role(권한)을 꺼내는 메서드
    public String getRole(String token, String secretKeyString){
        return getClaims(token, secretKeyString).get("role",String.class);
    }

    // JWT 토큰이 유효하고 만료되지 않았는지 검증하는 메서드
    public boolean validateToken(String token, String secretKeyString) {
        try {
            Claims claims = getClaims(token, secretKeyString);
            java.util.Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new java.util.Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token, String secretKeyString) {
        SecretKey key = getSigningKey(secretKeyString);
                return Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
    }

    private SecretKey getSigningKey(String secretKeyString) {
        byte[] keyBytes =
                secretKeyString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
