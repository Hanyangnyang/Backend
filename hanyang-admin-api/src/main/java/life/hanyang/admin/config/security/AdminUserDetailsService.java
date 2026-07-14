package life.hanyang.admin.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminAuthProperties adminAuthProperties;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminAuthProperties.getAccounts().stream()
                .filter(account -> account.getUsername().equals(username))
                .findFirst()
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPassword())
                        .roles("ADMIN")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Admin account not found: " + username));
    }
}
