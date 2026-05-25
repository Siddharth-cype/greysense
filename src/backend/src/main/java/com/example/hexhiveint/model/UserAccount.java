package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA entity representing a user account for dashboard authentication.
 *
 * <p><strong>Note:</strong> This is a simplified credential store for
 * development purposes. In production, passwords should be hashed
 * using BCrypt and authentication should be handled via Spring Security.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserAccount {

    /** Unique username serving as the primary key. */
    @Id
    private String username;

    /** Plaintext password (development only — hash in production). */
    private String password;
}
