package com.philldesk.philldeskbackend.service;

import com.philldesk.philldeskbackend.entity.Role;
import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<Role> getAllRoles();
    Optional<Role> getRoleById(Long id);
    Optional<Role> getRoleByName(Role.RoleName name);
    Role saveRole(Role role);
    void deleteRole(Long id);
    boolean existsByName(Role.RoleName name);
}
