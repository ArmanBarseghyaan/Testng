package student.tests;

import am.gortsup.testing.util.DateTimeUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;

/**
 * Тестовый класс для проверки методов DateTimeUtils.
 * Покрывает високосные года, расчет разницы дней, проверку выходных,
 * подсчет и прибавление рабочих дней, пересечение интервалов и попадание даты в диапазон.
 */
public class DateTimeUtilsTest {

    // ==========================================
    // 1. ТЕСТЫ ДЛЯ isLeapYear
    // ==========================================

    /**
     * DataProvider для проверки правил високосного года по Григорианскому календарю:
     * - делится на 4
     * - если делится на 100, должен делиться на 400
     */
    @DataProvider(name = "leapYearData")
    public Object[][] leapYearData() {
        return new Object[][]{
                {2024, true},   // делится на 4, не на 100
                {2000, true},   // делится на 400
                {1900, false},  // делится на 100, но не на 400
                {2023, false},  // не делится на 4
                {2400, true}    // делится на 400
        };
    }

    /**
     * Тест проверки високосного года:
     * Проверяет все граничные правила деления на 4, 100 и 400.
     */
    @Test(dataProvider = "leapYearData", groups = {"datetime"})
    public void testIsLeapYear(int year, boolean expectedLeap) {
        boolean actual = DateTimeUtils.isLeapYear(year);
        Assert.assertEquals(actual, expectedLeap,
                "Год " + year + " определен неверно на високосность");
    }

    // ==========================================
    // 2. ТЕСТЫ ДЛЯ calculateDaysBetween
    // ==========================================

    /**
     * Позитивный тест подсчета календарных дней между датами:
     * Проверяет прямую и обратную разницу.
     */
    @Test(groups = {"datetime"})
    public void testCalculateDaysBetweenSuccess() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 11);

        long days = DateTimeUtils.calculateDaysBetween(start, end);
        Assert.assertEquals(days, 10, "Между 1 и 11 января должно быть ровно 10 дней");

        // Обратная разница дает отрицательное значение
        long negativeDays = DateTimeUtils.calculateDaysBetween(end, start);
        Assert.assertEquals(negativeDays, -10, "Разница в обратном порядке должна быть отрицательной");
    }

    /**
     * Негативный тест подсчета дней:
     * Проверяет выброс IllegalArgumentException при передаче null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"datetime", "negative"})
    public void testCalculateDaysBetweenThrowsExceptionOnNull() {
        DateTimeUtils.calculateDaysBetween(null, LocalDate.now());
    }

    // ==========================================
    // 3. ТЕСТЫ ДЛЯ isWeekend
    // ==========================================

    /**
     * Тест проверки выходного дня (суббота или воскресенье):
     * Проверяет субботу, воскресенье и будний день.
     */
    @Test(groups = {"datetime"})
    public void testIsWeekend() {
        // 2026-10-17 это суббота, 2026-10-18 это воскресенье, 2026-10-19 это понедельник
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        LocalDate sunday = LocalDate.of(2026, 10, 18);
        LocalDate monday = LocalDate.of(2026, 10, 19);

        Assert.assertTrue(DateTimeUtils.isWeekend(saturday), "Суббота должна определяться как выходной");
        Assert.assertTrue(DateTimeUtils.isWeekend(sunday), "Воскресенье должно определяться как выходной");
        Assert.assertFalse(DateTimeUtils.isWeekend(monday), "Понедельник не выходной день");
    }

    // ==========================================
    // 4. ТЕСТЫ ДЛЯ calculateBusinessDaysBetween
    // ==========================================

    /**
     * Позитивный тест подсчета рабочих дней (пн-пт):
     * Проверяет пропуск субботы и воскресенья в интервале [start, end).
     */
    @Test(groups = {"datetime"})
    public void testCalculateBusinessDaysBetween() {
        // С пятницы 2026-10-16 по понедельник 2026-10-19:
        // Пятница учитывается (1 день), сб/вс пропускаются, понедельник не включен (exclusive) -> 1 день
        LocalDate friday = LocalDate.of(2026, 10, 16);
        LocalDate monday = LocalDate.of(2026, 10, 19);
        int businessDays = DateTimeUtils.calculateBusinessDaysBetween(friday, monday);
        Assert.assertEquals(businessDays, 1, "С пятницы до понедельника ровно 1 рабочий день");

        // Полная рабочая неделя: с понедельника по следующую пятницу (5 рабочих дней)
        LocalDate nextFriday = LocalDate.of(2026, 10, 23);
        int fullWeekDays = DateTimeUtils.calculateBusinessDaysBetween(monday, nextFriday);
        Assert.assertEquals(fullWeekDays, 4, "С понедельника до пятницы эксклюзивно 4 дня");

        // Если start >= end, возвращается 0
        Assert.assertEquals(DateTimeUtils.calculateBusinessDaysBetween(monday, monday), 0,
                "Если даты совпадают, количество рабочих дней 0");
    }

    // ==========================================
    // 5. ТЕСТЫ ДЛЯ addBusinessDays
    // ==========================================

    /**
     * Тест прибавления рабочих дней к дате:
     * Проверяет перенос через выходные дни.
     */
    @Test(groups = {"datetime"})
    public void testAddBusinessDays() {
        // Пятница 2026-10-16 + 1 рабочий день -> Понедельник 2026-10-19
        LocalDate friday = LocalDate.of(2026, 10, 16);
        LocalDate expectedMonday = LocalDate.of(2026, 10, 19);

        LocalDate result = DateTimeUtils.addBusinessDays(friday, 1);
        Assert.assertEquals(result, expectedMonday,
                "Добавление 1 рабочего дня к пятнице должно переносить на понедельник");

        // Добавление 0 дней возвращает ту же дату
        Assert.assertEquals(DateTimeUtils.addBusinessDays(friday, 0), friday,
                "Добавление 0 дней должно возвращать исходную дату");
    }

    // ==========================================
    // 6. ТЕСТЫ ДЛЯ doIntervalsOverlap
    // ==========================================

    /**
     * Тест пересечения двух закрытых интервалов дат [start1, end1] и [start2, end2]:
     * Проверяет частичное перекрытие, вложенность, касание и отсутствие пересечений.
     */
    @Test(groups = {"datetime"})
    public void testDoIntervalsOverlap() {
        LocalDate d1 = LocalDate.of(2026, 1, 1);
        LocalDate d5 = LocalDate.of(2026, 1, 5);
        LocalDate d4 = LocalDate.of(2026, 1, 4);
        LocalDate d10 = LocalDate.of(2026, 1, 10);
        LocalDate d12 = LocalDate.of(2026, 1, 12);

        // Интервалы [1, 5] и [4, 10] перекрываются
        Assert.assertTrue(DateTimeUtils.doIntervalsOverlap(d1, d5, d4, d10),
                "Интервалы должны пересекаться");

        // Интервалы [1, 5] и [10, 12] не перекрываются
        Assert.assertFalse(DateTimeUtils.doIntervalsOverlap(d1, d5, d10, d12),
                "Непересекающиеся интервалы должны возвращать false");
    }

    // ==========================================
    // 7. ТЕСТЫ ДЛЯ isDateInRange
    // ==========================================

    /**
     * Тест попадания даты в закрытый диапазон [min, max]:
     * Проверяет дату внутри диапазона, граничные значения (min и max) и дату вне диапазона.
     */
    @Test(groups = {"datetime"})
    public void testIsDateInRange() {
        LocalDate min = LocalDate.of(2026, 5, 1);
        LocalDate max = LocalDate.of(2026, 5, 31);
        LocalDate mid = LocalDate.of(2026, 5, 15);
        LocalDate outside = LocalDate.of(2026, 6, 1);

        Assert.assertTrue(DateTimeUtils.isDateInRange(mid, min, max), "Дата внутри диапазона");
        Assert.assertTrue(DateTimeUtils.isDateInRange(min, min, max), "Левая граница включительна");
        Assert.assertTrue(DateTimeUtils.isDateInRange(max, min, max), "Правая граница включительна");
        Assert.assertFalse(DateTimeUtils.isDateInRange(outside, min, max), "Дата вне диапазона возвращает false");
    }

    /**
     * Негативный тест диапазона дат:
     * Проверяет исключение, если min дата позже max даты.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"datetime", "negative"})
    public void testIsDateInRangeThrowsExceptionWhenMinAfterMax() {
        LocalDate d1 = LocalDate.of(2026, 5, 31);
        LocalDate d2 = LocalDate.of(2026, 5, 1);
        DateTimeUtils.isDateInRange(LocalDate.now(), d1, d2);
    }
}
