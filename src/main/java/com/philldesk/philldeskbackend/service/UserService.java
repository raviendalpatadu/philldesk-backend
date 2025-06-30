package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.User;
import com.philldesk.philldeskbackend.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    Page<User> getAllUsers(Pageable pageable);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);
    List<User> getUsersByRole(Role.RoleName roleName);
    List<User> getActiveUsers();
    User saveUser(User user);
    User updateUser(User user);
    void deleteUser(Long id);
    void deactivateUser(Long id);
    void activateUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean isUsernameAvailable(String username);
    boolean isEmailAvailable(String email);
}
