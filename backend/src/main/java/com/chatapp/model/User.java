package com.chatapp.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * JPA Entity representing an application user stored in MySQL.
 * - Passwords must be stored hashed (BCrypt) — never plain text.
 * - `status` tracks email verification state (PENDING_VERIFICATION, ACTIVE).
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(nullable = false)
    private String status; // PENDING_VERIFICATION, ACTIVE
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles;

    public User() {}

    public User(String email, String password, String displayName, String status, List<String> roles) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.status = status;
        this.roles = roles;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
