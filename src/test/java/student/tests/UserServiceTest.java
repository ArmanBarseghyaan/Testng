package student.tests;

import am.gortsup.testing.exception.AccountLockedException;
import am.gortsup.testing.exception.AuthenticationException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Role;
import am.gortsup.testing.model.User;
import am.gortsup.testing.service.UserService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки UserService.
 * Покрывает регистрацию, аутентификацию, блокировку после 3 неверных попыток входа,
 * смену пароля, проверку ролевой модели прав (RBAC), деактивацию и обновление профиля.
 */
public class UserServiceTest {

    private UserService userService;

    /**
     * Перед каждым тестом инициализируется чистый экземпляр сервиса пользователей,
     * чтобы обеспечить изолированность тестов.
     */
    @BeforeMethod
    public void setUp() {
        userService = new UserService();
    }

    // ==========================================
    // 1. ТЕСТЫ РЕГИСТРАЦИИ ПОЛЬЗОВАТЕЛЕЙ
    // ==========================================

    /**
     * Позитивный тест регистрации пользователя:
     * Проверяет успешное создание учетной записи с валидными данными,
     * назначение роли CUSTOMER по умолчанию (при передаче null) и статус active.
     */
    @Test(groups = {"user", "smoke"})
    public void testRegisterUserSuccess() {
        User user = userService.registerUser("john_doe", "john@example.com", "P@ssw0rd123!", null);

        Assert.assertNotNull(user.getId(), "Идентификатор пользователя должен быть сгенерирован");
        Assert.assertEquals(user.getUsername(), "john_doe", "Имя пользователя должно совпадать");
        Assert.assertEquals(user.getEmail(), "john@example.com", "Email должен совпадать");
        Assert.assertEquals(user.getRole(), Role.CUSTOMER, "Роль по умолчанию должна быть CUSTOMER");
        Assert.assertTrue(user.isActive(), "Новый пользователь должен быть активным");
        Assert.assertFalse(user.isLocked(), "Новый пользователь не должен быть заблокирован");
    }

    /**
     * Негативный тест регистрации с невалидным именем пользователя:
     * Проверяет выброс ValidationException, если username меньше 3 символов или содержит недопустимые символы.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testRegisterUserWithInvalidUsernameThrowsException() {
        userService.registerUser("ab", "ab@example.com", "P@ssw0rd123!", Role.CUSTOMER);
    }

    /**
     * Негативный тест регистрации с невалидным email:
     * Проверяет выброс ValidationException при некорректном формате email.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testRegisterUserWithInvalidEmailThrowsException() {
        userService.registerUser("valid_user", "invalid-email", "P@ssw0rd123!", Role.CUSTOMER);
    }

    /**
     * Негативный тест регистрации с простым паролем:
     * Проверяет выброс ValidationException, если пароль не соответствует требованиям сложности.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testRegisterUserWithWeakPasswordThrowsException() {
        userService.registerUser("valid_user", "valid@example.com", "weak", Role.CUSTOMER);
    }

    /**
     * Негативный тест на дубликат имени пользователя:
     * Проверяет запрет создания второго пользователя с тем же логином (без учета регистра).
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testRegisterUserDuplicateUsernameThrowsException() {
        userService.registerUser("alice_smith", "alice1@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        // Повторная регистрация с тем же именем в другом регистре
        userService.registerUser("ALICE_SMITH", "alice2@example.com", "P@ssw0rd123!", Role.CUSTOMER);
    }

    /**
     * Негативный тест на дубликат email:
     * Проверяет запрет регистрации нескольких пользователей с одним адресом почты.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testRegisterUserDuplicateEmailThrowsException() {
        userService.registerUser("user_one", "unique@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.registerUser("user_two", "unique@example.com", "P@ssw0rd123!", Role.CUSTOMER);
    }

    // ==========================================
    // 2. ТЕСТЫ АУТЕНТИФИКАЦИИ И БЕЗОПАСНОСТИ
    // ==========================================

    /**
     * Позитивный тест успешной аутентификации:
     * Проверяет корректный вход по логину и паролю и сброс счетчика неудачных попыток.
     */
    @Test(groups = {"user", "auth", "smoke"})
    public void testAuthenticateSuccess() {
        userService.registerUser("test_auth_user", "auth@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        User authenticated = userService.authenticate("test_auth_user", "P@ssw0rd123!");

        Assert.assertNotNull(authenticated, "Пользователь должен быть успешно авторизован");
        Assert.assertEquals(authenticated.getFailedLoginAttempts(), 0, "Счетчик попыток должен быть равен 0");
    }

    /**
     * Негативный тест аутентификации с неверным паролем:
     * Проверяет инкремент счетчика неудачных попыток и выброс AuthenticationException.
     */
    @Test(expectedExceptions = AuthenticationException.class, groups = {"user", "auth", "negative"})
    public void testAuthenticateInvalidPasswordIncrementsAttempts() {
        userService.registerUser("bob_marley", "bob@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.authenticate("bob_marley", "WrongPassword123!");
    }

    /**
     * Тест блокировки аккаунта после 3 последовательных неудачных попыток входа:
     * Проверяет, что третья неудачная попытка устанавливает статус locked=true и выбрасывает AccountLockedException.
     */
    @Test(expectedExceptions = AccountLockedException.class, groups = {"user", "security"})
    public void testAccountLockoutAfterThreeFailedAttempts() {
        userService.registerUser("lock_me", "lock@example.com", "P@ssw0rd123!", Role.CUSTOMER);

        // 1-я неверная попытка
        try {
            userService.authenticate("lock_me", "Wrong1!");
        } catch (AuthenticationException ignored) {}

        // 2-я неверная попытка
        try {
            userService.authenticate("lock_me", "Wrong2!");
        } catch (AuthenticationException ignored) {}

        // 3-я неверная попытка -> аккаунт должен заблокироваться и выбросить AccountLockedException
        userService.authenticate("lock_me", "Wrong3!");
    }

    /**
     * Негативный тест: вход в заблокированный аккаунт даже с верным паролем запрещен.
     */
    @Test(expectedExceptions = AccountLockedException.class, groups = {"user", "security", "negative"})
    public void testAuthenticateLockedUserThrowsAccountLockedException() {
        User user = userService.registerUser("locked_user", "locked@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.lockUser(user.getId());

        // Попытка входа с верным паролем в заблокированный аккаунт
        userService.authenticate("locked_user", "P@ssw0rd123!");
    }

    /**
     * Позитивный тест разблокировки аккаунта:
     * Проверяет, что unlockUser снимает блокировку и сбрасывает счетчик неудачных входов.
     */
    @Test(groups = {"user", "security"})
    public void testUnlockUserRestoresLogin() {
        User user = userService.registerUser("user_to_unlock", "unlock@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.lockUser(user.getId());
        Assert.assertTrue(user.isLocked(), "Пользователь должен быть заблокирован");

        userService.unlockUser(user.getId());
        Assert.assertFalse(user.isLocked(), "Пользователь должен быть разблокирован");
        Assert.assertEquals(user.getFailedLoginAttempts(), 0, "Счетчик попыток сброшен");

        // Теперь вход должен пройти успешно
        User authUser = userService.authenticate("user_to_unlock", "P@ssw0rd123!");
        Assert.assertNotNull(authUser);
    }

    /**
     * Негативный тест аутентификации деактивированного пользователя:
     * Проверяет выброс AuthenticationException при попытке входа неактивного пользователя.
     */
    @Test(expectedExceptions = AuthenticationException.class, groups = {"user", "negative"})
    public void testAuthenticateDeactivatedUserThrowsException() {
        User user = userService.registerUser("deactivated", "deact@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.deactivateUser(user.getId());

        userService.authenticate("deactivated", "P@ssw0rd123!");
    }

    // ==========================================
    // 3. ТЕСТЫ СМЕНЫ ПАРОЛЯ
    // ==========================================

    /**
     * Позитивный тест смены пароля:
     * Проверяет успешную смену пароля при правильном старом и сложном новом пароле.
     */
    @Test(groups = {"user", "security"})
    public void testChangePasswordSuccess() {
        User user = userService.registerUser("change_pass", "cp@example.com", "OldP@ssw0rd1!", Role.CUSTOMER);
        userService.changePassword(user.getId(), "OldP@ssw0rd1!", "NewP@ssw0rd2!");

        // Проверяем, что вход с новым паролем успешен
        User auth = userService.authenticate("change_pass", "NewP@ssw0rd2!");
        Assert.assertNotNull(auth);
    }

    /**
     * Негативный тест смены пароля:
     * Проверяет запрет установки нового пароля, совпадающего со старым.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"user", "negative"})
    public void testChangePasswordIdenticalThrowsException() {
        User user = userService.registerUser("same_pass", "sp@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.changePassword(user.getId(), "P@ssw0rd123!", "P@ssw0rd123!");
    }

    /**
     * Негативный тест смены пароля:
     * Проверяет ошибку при несовпадении старого пароля.
     */
    @Test(expectedExceptions = AuthenticationException.class, groups = {"user", "negative"})
    public void testChangePasswordWrongOldPasswordThrowsException() {
        User user = userService.registerUser("wrong_old", "wo@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.changePassword(user.getId(), "IncorrectOldPass1!", "NewValidP@ss1!");
    }

    // ==========================================
    // 4. ТЕСТЫ ПРАВ ДОСТУПА (RBAC)
    // ==========================================

    /**
     * Тест ролевой модели доступа:
     * Проверяет права ролей ADMIN, MANAGER, CUSTOMER и GUEST:
     * - ADMIN имеет любые права;
     * - MANAGER имеет доступ к управлению складом и заказами;
     * - CUSTOMER имеет доступ к чтению, созданию заказов и профилю;
     * - GUEST имеет только READ.
     */
    @Test(groups = {"user", "security"})
    public void testRolePermissionsHierarchy() {
        User admin = userService.registerUser("admin_user", "admin@store.com", "P@ssw0rd123!", Role.ADMIN);
        User manager = userService.registerUser("mgr_user", "mgr@store.com", "P@ssw0rd123!", Role.MANAGER);
        User customer = userService.registerUser("cust_user", "cust@store.com", "P@ssw0rd123!", Role.CUSTOMER);
        User guest = userService.registerUser("guest_user", "guest@store.com", "P@ssw0rd123!", Role.GUEST);

        // ADMIN
        Assert.assertTrue(userService.hasPermission(admin.getId(), "SUPER_PRIVILEGE"), "ADMIN имеет любые права");

        // MANAGER
        Assert.assertTrue(userService.hasPermission(manager.getId(), "MANAGE_INVENTORY"), "MANAGER может управлять складом");
        Assert.assertFalse(userService.hasPermission(manager.getId(), "SUPER_ADMIN_ONLY"), "MANAGER не имеет прав ADMIN");

        // CUSTOMER
        Assert.assertTrue(userService.hasPermission(customer.getId(), "CREATE_ORDER"), "CUSTOMER может создавать заказ");
        Assert.assertFalse(userService.hasPermission(customer.getId(), "MANAGE_INVENTORY"), "CUSTOMER не может управлять складом");

        // GUEST
        Assert.assertTrue(userService.hasPermission(guest.getId(), "READ"), "GUEST может читать");
        Assert.assertFalse(userService.hasPermission(guest.getId(), "CREATE_ORDER"), "GUEST не может создавать заказ");
    }

    // ==========================================
    // 5. ТЕСТЫ ОБНОВЛЕНИЯ EMAIL И ПОИСКА
    // ==========================================

    /**
     * Позитивный тест обновления email:
     * Проверяет изменение почты и доступность поиска по новой почте.
     */
    @Test(groups = {"user"})
    public void testUpdateEmailSuccess() {
        User user = userService.registerUser("email_changer", "old_email@example.com", "P@ssw0rd123!", Role.CUSTOMER);
        userService.updateEmail(user.getId(), "new_email@example.com");

        User updated = userService.getUserById(user.getId());
        Assert.assertEquals(updated.getEmail(), "new_email@example.com", "Email должен обновиться");
    }

    /**
     * Негативный тест поиска несуществующего пользователя:
     * Проверяет выброс ResourceNotFoundException.
     */
    @Test(expectedExceptions = ResourceNotFoundException.class, groups = {"user", "negative"})
    public void testGetUserByNonExistentIdThrowsException() {
        userService.getUserById("non-existent-user-id");
    }
}
