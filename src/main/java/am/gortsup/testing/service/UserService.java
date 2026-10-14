package am.gortsup.testing.service;

import am.gortsup.testing.exception.AccountLockedException;
import am.gortsup.testing.exception.AuthenticationException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Role;
import am.gortsup.testing.model.User;
import am.gortsup.testing.util.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user registration, authentication, security policies, and permissions.
 */
public class UserService {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToId = new ConcurrentHashMap<>();
    private final Map<String, String> emailToId = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = 3;

    /**
     * Registers a new user.
     *
     * @param username unique username (3-30 chars, alphanumeric + underscore)
     * @param email unique valid email address
     * @param password strong password meeting security requirements
     * @param role user role (defaults to CUSTOMER if null)
     * @return created User entity
     * @throws ValidationException if inputs are invalid or username/email already taken
     */
    public User registerUser(String username, String email, String password, Role role) {
        if (username == null || !username.matches("^[A-Za-z0-9_]{3,30}$")) {
            throw new ValidationException("Username must be 3-30 alphanumeric characters or underscores");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new ValidationException("Invalid email format: " + email);
        }
        if (!ValidationUtils.isValidPassword(password)) {
            throw new ValidationException("Password does not meet complexity requirements");
        }

        String usernameKey = username.toLowerCase();
        if (usernameToId.containsKey(usernameKey)) {
            throw new ValidationException("Username already exists: " + username);
        }
        String emailKey = email.trim().toLowerCase();
        if (emailToId.containsKey(emailKey)) {
            throw new ValidationException("Email already registered: " + email);
        }

        String userId = UUID.randomUUID().toString();
        User user = new User(userId, username, email.trim(), password, role == null ? Role.CUSTOMER : role);
        usersById.put(userId, user);
        usernameToId.put(usernameKey, userId);
        emailToId.put(emailKey, userId);
        return user;
    }

    /**
     * Authenticates a user by username and password.
     * Failed attempts are tracked; after 3 consecutive failures, the account is locked.
     *
     * @param username user username
     * @param password user password
     * @return authenticated User
     * @throws ValidationException if username or password is null or blank
     * @throws AuthenticationException if username not found or password incorrect
     * @throws AccountLockedException if account is locked
     */
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password must not be empty");
        }

        String userId = usernameToId.get(username.trim().toLowerCase());
        if (userId == null) {
            throw new AuthenticationException("Invalid username or password");
        }

        User user = usersById.get(userId);
        if (user.isLocked()) {
            throw new AccountLockedException("Account is locked due to too many failed attempts. Contact admin.");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("User account is inactive");
        }

        if (!user.getPassword().equals(password)) {
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLocked(true);
                throw new AccountLockedException("Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts.");
            }
            throw new AuthenticationException("Invalid username or password");
        }

        user.resetFailedAttempts();
        return user;
    }

    /**
     * Updates user password after verifying the old password.
     *
     * @param userId user identifier
     * @param oldPassword current password
     * @param newPassword new password meeting complexity criteria
     * @throws ResourceNotFoundException if user does not exist
     * @throws AuthenticationException if old password does not match
     * @throws ValidationException if new password is weak or matches old password
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!user.getPassword().equals(oldPassword)) {
            throw new AuthenticationException("Old password does not match");
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new ValidationException("New password does not meet security requirements");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ValidationException("New password cannot be the same as old password");
        }
        user.setPassword(newPassword);
    }

    /**
     * Locks a user account.
     *
     * @param userId user ID
     * @throws ResourceNotFoundException if user does not exist
     */
    public void lockUser(String userId) {
        User user = getUserById(userId);
        user.setLocked(true);
    }

    /**
     * Unlocks a user account and resets failed login attempts.
     *
     * @param userId user ID
     * @throws ResourceNotFoundException if user does not exist
     */
    public void unlockUser(String userId) {
        User user = getUserById(userId);
        user.setLocked(false);
        user.resetFailedAttempts();
    }

    /**
     * Finds a user by ID.
     *
     * @param userId user ID
     * @return User entity
     * @throws ResourceNotFoundException if user does not exist
     */
    public User getUserById(String userId) {
        if (userId == null) {
            throw new ResourceNotFoundException("User ID cannot be null");
        }
        User user = usersById.get(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        return user;
    }

    /**
     * Finds a user by username.
     *
     * @param username username to find
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserByUsername(String username) {
        if (username == null) {
            throw new ResourceNotFoundException("Username cannot be null");
        }
        String id = usernameToId.get(username.trim().toLowerCase());
        if (id == null) {
            throw new ResourceNotFoundException("User not found with username: " + username);
        }
        return usersById.get(id);
    }

    /**
     * Updates user's email address.
     *
     * @param userId user ID
     * @param newEmail valid new email address
     * @throws ResourceNotFoundException if user does not exist
     * @throws ValidationException if email invalid or already in use
     */
    public void updateEmail(String userId, String newEmail) {
        User user = getUserById(userId);
        if (!ValidationUtils.isValidEmail(newEmail)) {
            throw new ValidationException("Invalid email format: " + newEmail);
        }
        String newEmailKey = newEmail.trim().toLowerCase();
        String currentEmailKey = user.getEmail().toLowerCase();

        if (!currentEmailKey.equals(newEmailKey) && emailToId.containsKey(newEmailKey)) {
            throw new ValidationException("Email already registered: " + newEmail);
        }

        emailToId.remove(currentEmailKey);
        user.setEmail(newEmail.trim());
        emailToId.put(newEmailKey, userId);
    }

    /**
     * Checks if a user has a specific permission.
     * Role hierarchy:
     * - ADMIN: all permissions
     * - MANAGER: "READ", "WRITE", "MANAGE_INVENTORY", "MANAGE_ORDERS"
     * - CUSTOMER: "READ", "CREATE_ORDER", "VIEW_OWN_PROFILE"
     * - GUEST: "READ"
     *
     * @param userId user identifier
     * @param permission permission string
     * @return true if permitted, false otherwise
     */
    public boolean hasPermission(String userId, String permission) {
        User user = getUserById(userId);
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        String perm = permission.trim().toUpperCase();
        switch (user.getRole()) {
            case MANAGER:
                return Set.of("READ", "WRITE", "MANAGE_INVENTORY", "MANAGE_ORDERS").contains(perm);
            case CUSTOMER:
                return Set.of("READ", "CREATE_ORDER", "VIEW_OWN_PROFILE").contains(perm);
            case GUEST:
                return "READ".equals(perm);
            default:
                return false;
        }
    }

    /**
     * Retrieves all registered users.
     *
     * @return unmodifiable list of users
     */
    public List<User> getAllUsers() {
        return Collections.unmodifiableList(new ArrayList<>(usersById.values()));
    }

    /**
     * Deactivates a user account.
     *
     * @param userId user identifier
     * @throws ResourceNotFoundException if user does not exist
     */
    public void deactivateUser(String userId) {
        User user = getUserById(userId);
        user.setActive(false);
    }
}
