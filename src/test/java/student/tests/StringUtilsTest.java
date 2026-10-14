package student.tests;

import am.gortsup.testing.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки методов StringUtils.
 * Покрывает генерацию URL-слагов, капитализацию слов, обрезку строк с суффиксами,
 * подсчет вхождений подстроки, палиндромы, маскирование номеров банковских карт и реверс слов.
 */
public class StringUtilsTest {

    // ==========================================
    // 1. ТЕСТЫ ДЛЯ toSlug
    // ==========================================

    /**
     * DataProvider для проверки преобразования строк в слаги.
     * Проверяет удаление диакритических знаков, замену пробелов на дефисы,
     * схлопывание повторных дефисов и перевод в нижний регистр.
     */
    @DataProvider(name = "slugDataProvider")
    public Object[][] slugDataProvider() {
        return new Object[][]{
                {"Hello World", "hello-world"},
                {"  Café & Restaurant  ", "cafe-restaurant"},
                {"multiple---dashes and   spaces", "multiple-dashes-and-spaces"},
                {"Special!@# Characters$%^ Here", "special-characters-here"},
                {"", ""},
                {null, ""}
        };
    }

    /**
     * Позитивный и граничный тест генерации URL-слага:
     * Проверяет корректное форматирование текста в безопасный URL slug.
     */
    @Test(dataProvider = "slugDataProvider", groups = {"string"})
    public void testToSlug(String input, String expectedSlug) {
        String actual = StringUtils.toSlug(input);
        Assert.assertEquals(actual, expectedSlug,
                "Генерация слага для входа '" + input + "' вернула неверный результат");
    }

    // ==========================================
    // 2. ТЕСТЫ ДЛЯ capitalizeWords
    // ==========================================

    /**
     * Тест капитализации первых букв каждого слова:
     * Проверяет приведение первой буквы каждого слова к верхнему регистру,
     * а остальных букв слова к нижнему регистру (например "jOHn dOE" -> "John Doe").
     */
    @Test(groups = {"string"})
    public void testCapitalizeWords() {
        Assert.assertEquals(StringUtils.capitalizeWords("jOHn dOE"), "John Doe",
                "Слова с разным регистром должны быть нормализованы");
        Assert.assertEquals(StringUtils.capitalizeWords("qa automation engineering"), "Qa Automation Engineering",
                "Все первые буквы должны стать заглавными");
        Assert.assertEquals(StringUtils.capitalizeWords(""), "",
                "Пустая строка должна остаться пустой");
        Assert.assertEquals(StringUtils.capitalizeWords(null), "",
                "Null строка должна возвращать пустую строку");
    }

    // ==========================================
    // 3. ТЕСТЫ ДЛЯ truncate
    // ==========================================

    /**
     * Позитивный тест обрезки строки с добавлением суффикса:
     * Проверяет, что итоговая длина строки вместе с суффиксом не превышает maxLength.
     */
    @Test(groups = {"string"})
    public void testTruncateSuccess() {
        // Длина текста больше maxLength -> текст обрезается, суффикс добавляется
        String truncated = StringUtils.truncate("This is a long sentence for testing", 15, "...");
        Assert.assertEquals(truncated, "This is a lo...", "Обрезанная строка должна заканчиваться суффиксом");
        Assert.assertEquals(truncated.length(), 15, "Итоговая длина должна быть ровно maxLength");

        // Длина текста меньше maxLength -> текст не обрезается
        Assert.assertEquals(StringUtils.truncate("Short", 10, "..."), "Short",
                "Если текст короче maxLength, он не должен модифицироваться");

        // Граничное условие с null
        Assert.assertNull(StringUtils.truncate(null, 10, "..."),
                "Null текст должен возвращать null");
    }

    /**
     * Негативный тест обрезки строки:
     * Проверяет выброс IllegalArgumentException, если maxLength меньше длины суффикса.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"string", "negative"})
    public void testTruncateThrowsExceptionWhenMaxLengthLessThanSuffix() {
        // Длина суффикса "..." равна 3, а maxLength задан как 2 -> ошибка
        StringUtils.truncate("Hello World", 2, "...");
    }

    // ==========================================
    // 4. ТЕСТЫ ДЛЯ countOccurrences
    // ==========================================

    /**
     * Тест подсчета вхождений неперекрывающейся подстроки:
     * Проверяет точное совпадение с учетом регистра и краевые случаи (null, пустая подстрока).
     */
    @Test(groups = {"string"})
    public void testCountOccurrences() {
        Assert.assertEquals(StringUtils.countOccurrences("banana", "an"), 2,
                "В слове 'banana' подстрока 'an' встречается 2 раза");
        Assert.assertEquals(StringUtils.countOccurrences("banana", "na"), 2,
                "В слове 'banana' подстрока 'na' встречается 2 раза");
        Assert.assertEquals(StringUtils.countOccurrences("banana", "z"), 0,
                "Если подстрока не найдена, результат должен быть 0");
        Assert.assertEquals(StringUtils.countOccurrences(null, "a"), 0,
                "Null текст должен возвращать 0");
        Assert.assertEquals(StringUtils.countOccurrences("banana", ""), 0,
                "Пустая подстрока должна возвращать 0");
    }

    // ==========================================
    // 5. ТЕСТЫ ДЛЯ isPalindrome
    // ==========================================

    /**
     * Позитивный тест проверки палиндромов:
     * Проверяет фразы, читающиеся одинаково вперед и назад, игнорируя регистр, пробелы и знаки препинания.
     */
    @Test(groups = {"string"})
    public void testIsPalindromeValid() {
        Assert.assertTrue(StringUtils.isPalindrome("Racecar"), "Одно слово разного регистра должно быть палиндромом");
        Assert.assertTrue(StringUtils.isPalindrome("A man, a plan, a canal: Panama"),
                "Фраза со знаками препинания должна распознаваться как палиндром");
        Assert.assertTrue(StringUtils.isPalindrome("12321"), "Числовая строка-палиндром");
    }

    /**
     * Негативный тест проверки палиндромов:
     * Проверяет обычные строки, пустую строку и null.
     */
    @Test(groups = {"string", "negative"})
    public void testIsPalindromeInvalid() {
        Assert.assertFalse(StringUtils.isPalindrome("Hello World"), "Обычная строка не палиндром");
        Assert.assertFalse(StringUtils.isPalindrome(""), "Пустая строка возвращает false");
        Assert.assertFalse(StringUtils.isPalindrome(null), "Null строка возвращает false");
    }

    // ==========================================
    // 6. ТЕСТЫ ДЛЯ maskCreditCard
    // ==========================================

    /**
     * Тест маскирования номера кредитной карты:
     * Проверяет замену всех цифр кроме последних четырех на символ '*',
     * а также предварительное очищение от пробелов и дефисов.
     */
    @Test(groups = {"string", "security"})
    public void testMaskCreditCard() {
        Assert.assertEquals(StringUtils.maskCreditCard("4111 2222 3333 4444"), "************4444",
                "16-значная карта должна иметь 12 звездочек и 4 открытые цифры");
        Assert.assertEquals(StringUtils.maskCreditCard("1234-5678-9012"), "********9012",
                "12-значный номер с дефисами маскируется корректно");

        // Если цифр меньше 4, маскируются все цифры
        Assert.assertEquals(StringUtils.maskCreditCard("123"), "***",
                "Номер длиной меньше 4 цифр должен быть полностью замаскирован");
        Assert.assertEquals(StringUtils.maskCreditCard(null), "",
                "Null значение должно возвращать пустую строку");
    }

    // ==========================================
    // 7. ТЕСТЫ ДЛЯ reverseWords
    // ==========================================

    /**
     * Тест изменения порядка слов в предложении на обратный:
     * Проверяет реверс с сохранением одинарных пробелов между словами.
     */
    @Test(groups = {"string"})
    public void testReverseWords() {
        Assert.assertEquals(StringUtils.reverseWords("The quick brown fox"), "fox brown quick The",
                "Слова должны идти в обратном порядке");
        Assert.assertEquals(StringUtils.reverseWords("SingleWord"), "SingleWord",
                "Одно слово не меняется");
        Assert.assertEquals(StringUtils.reverseWords("   multiple   spaces   "), "spaces multiple",
                "Лишние пробелы должны удаляться");
        Assert.assertEquals(StringUtils.reverseWords(null), "",
                "Null должен возвращать пустую строку");
    }
}
