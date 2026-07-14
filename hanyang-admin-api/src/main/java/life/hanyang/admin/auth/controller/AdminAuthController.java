package life.hanyang.admin.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.admin.auth.dto.LoginRequest;
import life.hanyang.admin.auth.dto.TokenResponse;
import life.hanyang.admin.config.security.AdminAuthProperties;
import life.hanyang.core.auth.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 인증 API")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final AdminAuthProperties adminAuthProperties;

    @Operation(summary = "로그인을 완료하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String secretKey = adminAuthProperties.getJwt().getSecret();
        long expirationMs = adminAuthProperties.getJwt().getExpirationMs();

        String token = jwtProvider.createToken(authentication.getName(), role, secretKey, expirationMs);

        return ResponseEntity.ok(new TokenResponse(token));
    }
}
