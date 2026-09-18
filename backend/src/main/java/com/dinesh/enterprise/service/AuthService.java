package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.auth.AuthResponse;
import com.dinesh.enterprise.dto.auth.LoginRequest;
import com.dinesh.enterprise.dto.auth.RegisterRequest;
import com.dinesh.enterprise.dto.auth.UserResponse;
import com.dinesh.enterprise.entity.Role;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.enums.UserStatus;
import com.dinesh.enterprise.exception.DuplicateResourceException;
import com.dinesh.enterprise.exception.InvalidCredentialsException;
import com.dinesh.enterprise.repository.RoleRepository;
import com.dinesh.enterprise.repository.UserRepository;
import com.dinesh.enterprise.security.CustomUserDetails;
import com.dinesh.enterprise.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication service handling user registration and login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user with the CUSTOMER role.
     *
     * @param request registration details
     * @return AuthResponse with JWT token and user info
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException(
                        "CUSTOMER role not found. Ensure DataInitializer has run successfully."));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getEmail());

        CustomUserDetails userDetails = new CustomUserDetails(saved);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, saved);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login credentials
     * @return AuthResponse with JWT token and user info
     * @throws InvalidCredentialsException if credentials are invalid or account is disabled/locked
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            log.info("User logged in: {}", userDetails.getUsername());
            return buildAuthResponse(token, userDetails.getUser());

        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password.");
        } catch (DisabledException ex) {
            throw new InvalidCredentialsException("Account is disabled.");
        } catch (LockedException ex) {
            throw new InvalidCredentialsException("Account is locked.");
        }
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationDuration())
                .user(userResponse)
                .build();
    }
}
