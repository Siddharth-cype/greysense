package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link UserAccount} entities.
 *
 * <p>Provides CRUD operations for user authentication records.
 * Primary key is the username string.</p>
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
}
