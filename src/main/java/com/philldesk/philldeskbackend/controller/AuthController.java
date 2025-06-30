package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.dto.ApiResponse;
import com.philldesk.philldeskbackend.dto.JwtResponse;
import com.philldesk.philldeskbackend.dto.LoginRequest;
import com.philldesk.philldeskbackend.dto.SignupRequest;
import com.philldesk.philldeskbackend.entity.Role;
import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.repository.RoleRepository;
import com.philldesk.philldeskbackend.repository.UserRepository;
import com.philldesk.philldeskbackend.security.JwtUtils;
import com.philldesk.philldeskbackend.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final String ROLE_NOT_FOUND_ERROR = "Error: Role is not found.";
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder encoder,
                         JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        return ResponseEntity.ok(new JwtResponse(jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Object>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(false, "Error: Username is already taken!", null));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(false, "Error: Email is already in use!", null));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());

        String strRole = signUpRequest.getRole();
        Role role;

        if (strRole == null) {
            role = roleRepository.findByName(Role.RoleName.PHARMACIST)
                    .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
        } else {
            switch (strRole) {
                case "ADMIN":
                    role = roleRepository.findByName(Role.RoleName.ADMIN)
                            .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
                    break;
                case "CUSTOMER":
                    role = roleRepository.findByName(Role.RoleName.CUSTOMER)
                            .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
                    break;
                default:
                    role = roleRepository.findByName(Role.RoleName.PHARMACIST)
                            .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
            }
        }

        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully!", null));
    }
}
