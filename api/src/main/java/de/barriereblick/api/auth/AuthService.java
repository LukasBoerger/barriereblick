package de.barriereblick.api.auth;

import de.barriereblick.api.auth.dto.LoginRequest;
import de.barriereblick.api.auth.dto.RegisterRequest;
import de.barriereblick.api.auth.dto.TokenResponse;
import de.barriereblick.api.common.EmailAlreadyUsedException;
import de.barriereblick.api.common.PasswordTooLongException;
import de.barriereblick.api.org.Organization;
import de.barriereblick.api.org.OrganizationRepository;
import de.barriereblick.api.user.AppUser;
import de.barriereblick.api.user.UserRepository;
import de.barriereblick.api.user.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    // BCrypt verarbeitet maximal 72 Bytes; Spring Security 6.5+ wirft darueber
    // eine Exception. Wir validieren deshalb VOR encode()/matches().
    static final int MAX_PASSWORD_BYTES = 72;

    // Strukturell gueltiger BCrypt-Hash fuer den Vergleich bei unbekannter E-Mail:
    // matches() laeuft dadurch immer mit voller BCrypt-Kostenfunktion – die
    // Antwortzeit verraet nicht, ob die E-Mail existiert (kein Timing-Leak).
    private static final String UNKNOWN_USER_DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(OrganizationRepository organizationRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Legt Organisation + ersten User (ADMIN) an und stellt direkt ein JWT aus.
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // @Size zaehlt Zeichen, BCrypt Bytes – Mehrbyte-Zeichen (Umlaute, Emoji)
        // koennen das 72-Byte-Limit trotz gueltiger @Size-Validierung reissen.
        if (utf8Length(request.password()) > MAX_PASSWORD_BYTES) {
            throw new PasswordTooLongException();
        }
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyUsedException();
        }

        Organization organization = organizationRepository.save(new Organization(request.orgName().trim()));
        AppUser user = userRepository.save(new AppUser(
                organization,
                email,
                passwordEncoder.encode(request.password()),
                UserRole.ADMIN));

        return tokenService.issueToken(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // Register erlaubt maximal 72 Bytes – ein laengeres Passwort kann nie
        // korrekt sein. Frueh ablehnen, bevor BCrypt (>72 Bytes) eine Exception wirft.
        if (utf8Length(request.password()) > MAX_PASSWORD_BYTES) {
            throw new BadCredentialsException("Login fehlgeschlagen");
        }

        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmail(email).orElse(null);

        // Identisches Verhalten bei unbekannter E-Mail und falschem Passwort
        // (keine User-Enumeration). Der Dummy-Hash-Vergleich haelt auch die
        // Antwortzeit identisch (kein Timing-Leak durch Short-Circuit).
        String passwordHash = user != null ? user.getPasswordHash() : UNKNOWN_USER_DUMMY_HASH;
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !passwordMatches) {
            throw new BadCredentialsException("Login fehlgeschlagen");
        }

        return tokenService.issueToken(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
