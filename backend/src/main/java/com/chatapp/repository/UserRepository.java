package com.chatapp.repository;

// ============================================================================
// IMPORTS
// ============================================================================

import com.chatapp.model.User;  // The User entity we're managing

// Spring Data JPA - provides automatic database operations
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;  // Container that may or may not contain a value

/**
 * ============================================================================
 * USER REPOSITORY - Interface for User database operations
 * ============================================================================
 * 
 * WHAT IS A JPA REPOSITORY?
 * -------------------------
 * A Repository is an interface that provides database operations (CRUD) for
 * an entity without writing any SQL! Spring Data JPA automatically generates
 * the implementation at runtime.
 * 
 * By extending JpaRepository<User, Long>, we get these methods FOR FREE:
 * 
 * CREATE:
 * - save(User entity)           → INSERT INTO users ...
 * - saveAll(Iterable<User>)     → Bulk insert
 * 
 * READ:
 * - findById(Long id)           → SELECT * FROM users WHERE id = ?
 * - findAll()                   → SELECT * FROM users
 * - count()                     → SELECT COUNT(*) FROM users
 * - existsById(Long id)         → Returns true if exists
 * 
 * UPDATE:
 * - save(User entity)           → UPDATE users SET ... WHERE id = ?
 *   (If entity has an ID, it updates; otherwise, it inserts)
 * 
 * DELETE:
 * - deleteById(Long id)         → DELETE FROM users WHERE id = ?
 * - delete(User entity)         → DELETE using entity's ID
 * - deleteAll()                 → DELETE FROM users (careful!)
 * 
 * WHY USE JpaRepository INSTEAD OF WRITING SQL?
 * ---------------------------------------------
 * 1. Less code - no boilerplate SQL
 * 2. Type-safe - compiler catches errors
 * 3. Automatic parameter binding - prevents SQL injection
 * 4. Database agnostic - works with MySQL, PostgreSQL, etc.
 * 5. Built-in pagination and sorting
 * 
 * TYPE PARAMETERS EXPLAINED:
 * JpaRepository<User, Long>
 *              ↑     ↑
 *              │     └── Type of the primary key (User.id is Long)
 *              └── The entity class this repository manages
 * 
 * HOW THIS WORKS (Magic of Spring Data JPA):
 * ------------------------------------------
 * 1. At startup, Spring scans for interfaces extending JpaRepository
 * 2. Spring creates a proxy implementation automatically
 * 3. Method names are parsed to generate SQL queries
 *    - findByEmail → SELECT * FROM users WHERE email = ?
 *    - existsByEmail → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 * 
 * @see User The entity class this repository manages
 * @see AuthService Where this repository is used
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds a user by their email address.
     * 
     * METHOD NAME CONVENTION (Query Derivation):
     * Spring parses the method name to generate SQL:
     * - "find"   → SELECT operation
     * - "By"     → WHERE clause starts here
     * - "Email"  → Column name to filter on
     * 
     * Generated SQL: SELECT * FROM users WHERE email = ?
     * 
     * WHY RETURN Optional<User>?
     * - User might not exist → return Optional.empty()
     * - User exists → return Optional.of(user)
     * - This forces callers to handle both cases explicitly
     * - Prevents NullPointerException
     * 
     * USAGE EXAMPLES:
     * 
     * Example 1 - Using orElseThrow (common in login):
     * User user = userRepository.findByEmail("john@example.com")
     *     .orElseThrow(() -> new RuntimeException("User not found"));
     * 
     * Example 2 - Using isPresent() check:
     * Optional<User> optionalUser = userRepository.findByEmail(email);
     * if (optionalUser.isPresent()) {
     *     User user = optionalUser.get();
     *     // Do something with user
     * }
     * 
     * @param email The email address to search for
     * @return Optional containing the User if found, or empty if not found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if a user with the given email already exists.
     * 
     * METHOD NAME CONVENTION:
     * - "exists" → Returns boolean (true/false)
     * - "By"     → WHERE clause starts here
     * - "Email"  → Column name to check
     * 
     * Generated SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * (Or: SELECT EXISTS(SELECT 1 FROM users WHERE email = ?))
     * 
     * WHY USE THIS INSTEAD OF findByEmail?
     * - More efficient - doesn't load entire User object
     * - Only returns true/false
     * - Use for validation before creating new user
     * 
     * USAGE EXAMPLE (in registration):
     * if (userRepository.existsByEmail(request.getEmail())) {
     *     throw new EmailAlreadyExistsException("Email already registered");
     * }
     * 
     * @param email The email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}