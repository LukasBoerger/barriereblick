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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(organizationRepository, userRepository, passwordEncoder, tokenService);
    }

    @Test
    @DisplayName("register legt Organisation und ADMIN-User mit BCrypt-Hash und normalisierter E-Mail an")
    void register_createsOrgAndAdminUser() {
        var request = new RegisterRequest("Agentur Beispiel", "  Anna@Agentur.DE ", "superlanges-passwort");
        var tokenResponse = new TokenResponse("token", Instant.now());

        when(userRepository.findByEmail("anna@agentur.de")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("superlanges-passwort")).thenReturn("$2a$10$hash");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issueToken(any(AppUser.class))).thenReturn(tokenResponse);

        var result = authService.register(request);

        assertThat(result).isSameAs(tokenResponse);

        var userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("anna@agentur.de");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedUser.getOrganization().getName()).isEqualTo("Agentur Beispiel");
    }

    @Test
    @DisplayName("register wirft EmailAlreadyUsedException bei Duplikat-E-Mail")
    void register_duplicateEmail_throws() {
        var request = new RegisterRequest("Agentur", "anna@agentur.de", "superlanges-passwort");
        when(userRepository.findByEmail("anna@agentur.de"))
                .thenReturn(Optional.of(user("anna@agentur.de", "$2a$10$hash")));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(organizationRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login liefert Token bei korrekten Zugangsdaten")
    void login_validCredentials_returnsToken() {
        var user = user("anna@agentur.de", "$2a$10$hash");
        var tokenResponse = new TokenResponse("token", Instant.now());

        when(userRepository.findByEmail("anna@agentur.de")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("geheimes-passwort", "$2a$10$hash")).thenReturn(true);
        when(tokenService.issueToken(user)).thenReturn(tokenResponse);

        var result = authService.login(new LoginRequest("Anna@Agentur.de", "geheimes-passwort"));

        assertThat(result).isSameAs(tokenResponse);
    }

    @Test
    @DisplayName("login: unbekannte E-Mail und falsches Passwort verhalten sich identisch (401)")
    void login_unknownEmailAndWrongPassword_behaveIdentically() {
        when(userRepository.findByEmail("unbekannt@agentur.de")).thenReturn(Optional.empty());
        // Auch bei unbekannter E-Mail laeuft der Dummy-Hash-Vergleich (Timing-Schutz).
        when(passwordEncoder.matches(eq("egal"), anyString())).thenReturn(false);

        var user = user("anna@agentur.de", "$2a$10$hash");
        when(userRepository.findByEmail("anna@agentur.de")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("falsches-passwort", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("unbekannt@agentur.de", "egal")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("anna@agentur.de", "falsches-passwort")))
                .isInstanceOf(BadCredentialsException.class);

        verify(tokenService, never()).issueToken(any());
    }

    @Test
    @DisplayName("login: auch bei unbekannter E-Mail laeuft der BCrypt-Vergleich (kein Timing-Leak)")
    void login_unknownEmail_stillRunsPasswordComparison() {
        when(userRepository.findByEmail("unbekannt@agentur.de")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unbekannt@agentur.de", "egal")))
                .isInstanceOf(BadCredentialsException.class);

        // Dummy-Hash-Vergleich statt Short-Circuit: matches() wird IMMER aufgerufen.
        verify(passwordEncoder).matches(eq("egal"), anyString());
    }

    @Test
    @DisplayName("register: Passwort ueber 72 UTF-8-Bytes wird mit PasswordTooLongException abgelehnt")
    void register_passwordOver72Bytes_throws() {
        // 40 Umlaute = 40 Zeichen (besteht @Size max 72), aber 80 UTF-8-Bytes.
        var request = new RegisterRequest("Agentur", "anna@agentur.de", "ä".repeat(40));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(PasswordTooLongException.class);

        verify(organizationRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("login: Passwort ueber 72 UTF-8-Bytes liefert 401, ohne BCrypt aufzurufen")
    void login_passwordOver72Bytes_failsWithoutBcrypt() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("anna@agentur.de", "ä".repeat(40))))
                .isInstanceOf(BadCredentialsException.class);

        // BCrypt (Spring Security 6.5+) wirft bei > 72 Bytes – darf nie erreicht werden.
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenService, never()).issueToken(any());
    }

    private AppUser user(String email, String passwordHash) {
        return new AppUser(new Organization("Agentur"), email, passwordHash, UserRole.ADMIN);
    }
}
