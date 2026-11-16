package in.feekeeper.auth;

import in.feekeeper.auth.dto.*;
import in.feekeeper.common.exception.ConflictException;
import in.feekeeper.tenant.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
public class AuthService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock) {
        this.tenantRepository = tenantRepository; this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
        }
        Tenant tenant = tenantRepository.save(new Tenant(request.businessName().trim(), request.ownerName().trim(),
                normalizePhone(request.phone()), trimToNull(request.upiId()), clock.instant()));
        AppUser user = userRepository.save(new AppUser(tenant.getId(), email,
                passwordEncoder.encode(request.password()), UserRole.OWNER, clock.instant()));
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new InvalidCredentialsException();
        return responseFor(user);
    }

    private AuthResponse responseFor(AppUser user) {
        String token = jwtService.generate(user);
        return new AuthResponse(token, "Bearer", jwtService.expiresAt(token), user.getId(), user.getTenantId(), user.getRole());
    }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String normalizePhone(String phone) { return phone.startsWith("+91") ? phone : "+91" + phone; }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
