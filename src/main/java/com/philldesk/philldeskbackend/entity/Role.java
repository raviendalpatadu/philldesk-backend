package com.philldesk.philldeskbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleName name;
    
    @Column(length = 500)
    private String description;
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    private Set<User> users;
    
    public enum RoleName {
        ADMIN,
        PHARMACIST,
        CUSTOMER
    }
}
