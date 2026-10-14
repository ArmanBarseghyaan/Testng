package student.tests;

import am.gortsup.testing.util.ValidationUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки методов ValidationUtils.
 * Покрывает валидацию email, паролей, номеров телефонов, почтовых индексов,
 * IBAN, IPv4 адресов, URL, санитайзера XSS и проверки положительных чисел.
 */
public class ValidationUtilsTest {

    // ==========================================
    // 1. ТЕСТЫ ДЛЯ isValidEmail
    // ==========================================

    /**
     * DataProvider с валидными адресами электронной почты.
     * Проверяет стандартные форматы, поддомены, плюсы и различные TLD.
     */
    @DataProvider(name = "validEmails")
    public Object[][] validEmails() {
        return new Object[][]{
                {"user@example.com"},
                {"first.last@sub.domain.org"},
                {"customer+newsletter@company.co.uk"},
                {"student123@university.am"},
                {"simple_user@domain.io"}
        };
    }

    /**
     * Позитивный тест валидации email:
     * Проверяет, что корректно составленные email-адреса возвращают true.
     */
    @Test(dataProvider = "validEmails", groups = {"validation", "smoke"})
    public void testValidEmailFormats(String email) {
        Assert.assertTrue(ValidationUtils.isValidEmail(email),
                "Ожидалось, что email '" + email + "' будет признан валидным");
    }

    /**
     * DataProvider с невалидными адресами электронной почты.
     * Покрывает пустые значения, отсутствие символа @, двойные точки, некорректные домены и превышение длины.
     */
    @DataProvider(name = "invalidEmails")
    public Object[][] invalidEmails() {
        return new Object[][]{
                {null},
                {""},
                {"   "},
                {"plainaddress"},
                {"missingAtSign.com"},
                {"@nodomain.com"},
                {"user@.com"},
                {"user..name@domain.com"},
                {"user@domain..com"},
                {"user@domain.c"}, // TLD меньше 2 символов
                {"a".repeat(250) + "@domain.com"} // Превышение 254 символов
        };
    }

    /**
     * Негативный тест валидации email:
     * Проверяет, что невалидные форматы, граничные длины и null возвращают false.
     */
    @Test(dataProvider = "invalidEmails", groups = {"validation", "negative"})
    public void testInvalidEmailFormats(String email) {
        Assert.assertFalse(ValidationUtils.isValidEmail(email),
                "Ожидалось, что email '" + email + "' будет признан невалидным");
    }

    // ==========================================
    // 2. ТЕСТЫ ДЛЯ isValidPassword
    // ==========================================

    /**
     * Позитивный тест сложности пароля:
     * Проверяет, что пароли, удовлетворяющие всем требованиям безопасности
     * (8-64 символа, верхний и нижний регистр, цифра, спецсимвол), признаются валидными.
     */
    @Test(groups = {"validation", "security"})
    public void testValidPasswordComplexity() {
        Assert.assertTrue(ValidationUtils.isValidPassword("P@ssw0rd1"),
                "Пароль 'P@ssw0rd1' должен удовлетворять критериям сложности");
        Assert.assertTrue(ValidationUtils.isValidPassword("Secure#2026Strong"),
                "Пароль с диезом и цифрами должен быть валидным");
        Assert.assertTrue(ValidationUtils.isValidPassword("A1b!aaaa"),
                "Минимальный валидный пароль (8 символов) должен проходить валидацию");
    }

    /**
     * DataProvider с некорректными паролями, нарушающими определенные правила:
     * - длина менее 8 символов
     * - длина более 64 символов
     * - отсутствие заглавной буквы
     * - отсутствие строчной буквы
     * - отсутствие цифры
     * - отсутствие спецсимвола
     * - наличие пробельных символов (пробел, таб)
     * - null значение
     */
    @DataProvider(name = "invalidPasswords")
    public Object[][] invalidPasswords() {
        return new Object[][]{
                {null},
                {""},
                {"Short1!"},                                // 7 символов (меньше 8)
                {"a".repeat(60) + "A1!extraLongLong123"},   // больше 64 символов
                {"lowercase1!pass"},                        // нет заглавной буквы
                {"UPPERCASE1!PASS"},                        // нет строчной буквы
                {"NoDigits!Here"},                          // нет цифр
                {"NoSpecialChar123"},                       // нет спецсимвола
                {"Pass word 1!"},                           // содержит пробел
                {"Pass\tword1!"}                            // содержит табуляцию
        };
    }

    /**
     * Негативный тест сложности пароля:
     * Проверяет отсечение паролей, не соответствующих хотя бы одному правилу сложности.
     */
    @Test(dataProvider = "invalidPasswords", groups = {"validation", "security", "negative"})
    public void testInvalidPasswordComplexity(String password) {
        Assert.assertFalse(ValidationUtils.isValidPassword(password),
                "Пароль '" + password + "' не должен проходить валидацию сложности");
    }

    // ==========================================
    // 3. ТЕСТЫ ДЛЯ isValidPhoneNumber
    // ==========================================

    /**
     * Позитивный тест проверки номера телефона:
     * Проверяет поддержку международного формата E.164, скобок, дефисов и пробелов.
     */
    @Test(groups = {"validation"})
    public void testValidPhoneNumbers() {
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("+1 (555) 123-4567"),
                "Американский формат телефона должен быть валидным");
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("+374 91 123456"),
                "Армянский формат с пробелами должен быть валидным");
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("1234567"),
                "Минимальная граница (7 цифр) должна быть валидной");
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("+123456789012345"),
                "Максимальная граница (15 цифр) должна быть валидной");
    }

    /**
     * Негативный тест проверки номера телефона:
     * Проверяет отклонение строк с недостаточным (<7) или избыточным (>15) количеством цифр,
     * а также буквенных символов и пустых значений.
     */
    @Test(groups = {"validation", "negative"})
    public void testInvalidPhoneNumbers() {
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber(null), "Null номер невалиден");
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber(""), "Пустой номер невалиден");
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("123456"), "6 цифр (меньше 7) невалидно");
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("+1234567890123456"), "16 цифр (> 15) невалидно");
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("call-me-maybe"), "Буквенный номер невалиден");
    }

    // ==========================================
    // 4. ТЕСТЫ ДЛЯ isValidPostalCode
    // ==========================================

    /**
     * Тест проверки почтовых индексов различных стран:
     * Покрывает США (5 цифр и ZIP+4), Великобританию, Армению, Канаду и общие форматы.
     */
    @Test(groups = {"validation"})
    public void testValidPostalCodesByCountry() {
        // США: 5 цифр или 5+4
        Assert.assertTrue(ValidationUtils.isValidPostalCode("90210", "US"), "US 5-значный индекс валиден");
        Assert.assertTrue(ValidationUtils.isValidPostalCode("90210-1234", "US"), "US ZIP+4 индекс валиден");

        // Великобритания
        Assert.assertTrue(ValidationUtils.isValidPostalCode("SW1A 1AA", "UK"), "UK индекс валиден");
        Assert.assertTrue(ValidationUtils.isValidPostalCode("W1A 0AX", "GB"), "GB индекс валиден");

        // Армения: ровно 4 цифры
        Assert.assertTrue(ValidationUtils.isValidPostalCode("0010", "AM"), "Армянский 4-значный индекс валиден");

        // Канада: A1A 1A1
        Assert.assertTrue(ValidationUtils.isValidPostalCode("K1A 0B1", "CA"), "Канадский индекс валиден");

        // Другие страны (3-10 буквенно-цифровых символов)
        Assert.assertTrue(ValidationUtils.isValidPostalCode("75008", "FR"), "Французский индекс валиден");
    }

    /**
     * Негативный тест проверки почтовых индексов:
     * Проверяет индексы с неверным форматом для конкретной страны и null параметры.
     */
    @Test(groups = {"validation", "negative"})
    public void testInvalidPostalCodes() {
        Assert.assertFalse(ValidationUtils.isValidPostalCode(null, "US"), "Null индекс невалиден");
        Assert.assertFalse(ValidationUtils.isValidPostalCode("90210", null), "Null код страны невалиден");
        Assert.assertFalse(ValidationUtils.isValidPostalCode("9021", "US"), "US 4 цифры невалидны");
        Assert.assertFalse(ValidationUtils.isValidPostalCode("001", "AM"), "AM 3 цифры невалидны");
        Assert.assertFalse(ValidationUtils.isValidPostalCode("00010", "AM"), "AM 5 цифр невалидны");
        Assert.assertFalse(ValidationUtils.isValidPostalCode("INVALID_LONG_CODE_12345", "DE"),
                "Слишком длинный код для другой страны невалиден");
    }

    // ==========================================
    // 5. ТЕСТЫ ДЛЯ isValidIban
    // ==========================================

    /**
     * Тест проверки валидного IBAN:
     * Проверяет корректный расчет контрольной суммы ISO 7064 mod-97.
     */
    @Test(groups = {"validation", "finance"})
    public void testValidIban() {
        // Известный валидный IBAN Великобритании (GB82 WEST 1234 5698 7654 32)
        Assert.assertTrue(ValidationUtils.isValidIban("GB82WEST12345698765432"),
                "Корректный IBAN Великобритании должен проходить проверку mod-97");
        // Проверка с пробелами
        Assert.assertTrue(ValidationUtils.isValidIban("GB82 WEST 1234 5698 7654 32"),
                "IBAN с пробелами должен очищаться и проходить проверку");
    }

    /**
     * Негативный тест проверки IBAN:
     * Проверяет null, искаженную контрольную сумму, недопустимые символы и неверную длину.
     */
    @Test(groups = {"validation", "finance", "negative"})
    public void testInvalidIban() {
        Assert.assertFalse(ValidationUtils.isValidIban(null), "Null IBAN невалиден");
        Assert.assertFalse(ValidationUtils.isValidIban("GB82WEST123"), "Слишком короткий IBAN (<15 символов)");
        Assert.assertFalse(ValidationUtils.isValidIban("GB82WEST123456987654321111111111111111111"), "Слишком длинный IBAN (>34)");
        Assert.assertFalse(ValidationUtils.isValidIban("GB00WEST12345698765432"), "Неверная контрольная сумма mod-97");
    }

    // ==========================================
    // 6. ТЕСТЫ ДЛЯ isValidIpAddress
    // ==========================================

    /**
     * Позитивный тест проверки IPv4 адресов:
     * Проверяет граничные значения каждого октета (0 и 255) и стандартные IP.
     */
    @Test(groups = {"validation"})
    public void testValidIpAddress() {
        Assert.assertTrue(ValidationUtils.isValidIpAddress("192.168.1.1"), "Стандартный локальный IP валиден");
        Assert.assertTrue(ValidationUtils.isValidIpAddress("0.0.0.0"), "Минимальная граница IPv4 валидна");
        Assert.assertTrue(ValidationUtils.isValidIpAddress("255.255.255.255"), "Максимальная граница IPv4 валидна");
        Assert.assertTrue(ValidationUtils.isValidIpAddress("10.0.0.1"), "Адрес подсети валиден");
    }

    /**
     * Негативный тест проверки IPv4 адресов:
     * Проверяет октеты > 255, лидирующие нули ("01"), недостаточное количество октетов и спецсимволы.
     */
    @Test(groups = {"validation", "negative"})
    public void testInvalidIpAddress() {
        Assert.assertFalse(ValidationUtils.isValidIpAddress(null), "Null IP невалиден");
        Assert.assertFalse(ValidationUtils.isValidIpAddress(""), "Пустой IP невалиден");
        Assert.assertFalse(ValidationUtils.isValidIpAddress("256.0.0.1"), "Октет > 255 невалиден");
        Assert.assertFalse(ValidationUtils.isValidIpAddress("192.168.01.1"), "Лидирующий ноль в октете запрещен");
        Assert.assertFalse(ValidationUtils.isValidIpAddress("192.168.1"), "3 октета невалидны");
        Assert.assertFalse(ValidationUtils.isValidIpAddress("192.168.1.1.1"), "5 октетов невалидны");
        Assert.assertFalse(ValidationUtils.isValidIpAddress("abc.def.ghi.jkl"), "Буквенные октеты невалидны");
    }

    // ==========================================
    // 7. ТЕСТЫ ДЛЯ isValidUrl
    // ==========================================

    /**
     * Позитивный тест проверки URL:
     * Проверяет HTTP и HTTPS протоколы, порты, параметры запроса и якоря.
     */
    @Test(groups = {"validation"})
    public void testValidUrl() {
        Assert.assertTrue(ValidationUtils.isValidUrl("https://example.com"), "HTTPS URL валиден");
        Assert.assertTrue(ValidationUtils.isValidUrl("http://sub.domain.org/path?query=1&b=2#sec"),
                "URL с параметрами и хешем валиден");
        Assert.assertTrue(ValidationUtils.isValidUrl("https://www.google.com/search?q=testng"),
                "URL поискового запроса валиден");
    }

    /**
     * Негативный тест проверки URL:
     * Проверяет отклонение неподдерживаемых протоколов (ftp), отсутствия схемы и битых строк.
     */
    @Test(groups = {"validation", "negative"})
    public void testInvalidUrl() {
        Assert.assertFalse(ValidationUtils.isValidUrl(null), "Null URL невалиден");
        Assert.assertFalse(ValidationUtils.isValidUrl(""), "Пустой URL невалиден");
        Assert.assertFalse(ValidationUtils.isValidUrl("ftp://example.com"), "FTP протокол не разрешен");
        Assert.assertFalse(ValidationUtils.isValidUrl("just-a-plain-string"), "Строка без схемы невалидна");
        Assert.assertFalse(ValidationUtils.isValidUrl("http:/missing-slash.com"), "Сломанный протокол невалиден");
    }

    // ==========================================
    // 8. ТЕСТЫ ДЛЯ sanitizeInput
    // ==========================================

    /**
     * Тест очистки пользовательского ввода от потенциальных XSS-инъекций:
     * Проверяет замену символов &, <, >, ", ' на соответствующие HTML-сущности.
     */
    @Test(groups = {"validation", "security"})
    public void testSanitizeInputAgainstXss() {
        String dangerousInput = "<script>alert('XSS & danger');</script> \"quoted\"";
        String expected = "&lt;script&gt;alert(&#x27;XSS &amp; danger&#x27;);&lt;/script&gt; &quot;quoted&quot;";
        String actual = ValidationUtils.sanitizeInput(dangerousInput);
        Assert.assertEquals(actual, expected, "HTML спецсимволы должны быть экранированы");

        // Проверка граничного условия с null
        Assert.assertEquals(ValidationUtils.sanitizeInput(null), "",
                "Null ввод должен возвращать пустую строку");
    }

    // ==========================================
    // 9. ТЕСТЫ ДЛЯ isPositiveNumber
    // ==========================================

    /**
     * Тест проверки строго положительного числа:
     * Проверяет поведение для положительных, отрицательных, нулевых и null значений.
     */
    @Test(groups = {"validation"})
    public void testIsPositiveNumber() {
        Assert.assertTrue(ValidationUtils.isPositiveNumber(10), "Целое положительное число > 0");
        Assert.assertTrue(ValidationUtils.isPositiveNumber(0.0001), "Дробное число > 0");
        Assert.assertFalse(ValidationUtils.isPositiveNumber(0), "Ноль не является строго положительным");
        Assert.assertFalse(ValidationUtils.isPositiveNumber(-5), "Отрицательное число не является положительным");
        Assert.assertFalse(ValidationUtils.isPositiveNumber(null), "Null значение возвращает false");
    }
}
