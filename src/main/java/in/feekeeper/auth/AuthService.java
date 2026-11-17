package in.feekeeper.auth;

import in.feekeeper.auth.dto.*;
import in.feekeeper.common.exception.ConflictException;
import in.feekeeper.tenant.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.*;

import java.time.Clock;
import java.util.Locale;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock) {
        this.tenantRepository = tenantRepository; this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("FeeKeeper timing equalization value");
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
        var found = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()));
        String passwordHash = found.map(AppUser::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (found.isEmpty() || !passwordMatches) {
            log.warn("Login attempt failed");
            throw new InvalidCredentialsException();
        }
        AppUser user = found.get();
        log.info("Login attempt succeeded");
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
