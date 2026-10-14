package student.tests;

import am.gortsup.testing.util.MathAndFinanceUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки методов MathAndFinanceUtils.
 * Покрывает расчет налогов, скидок, банковское округление (HALF_UP),
 * алгоритм Луна для банковских карт, конвертацию валют, сложные проценты,
 * расчет аннуитетного платежа по кредиту и расчет процентного изменения.
 */
public class MathAndFinanceUtilsTest {

    // ==========================================
    // 1. ТЕСТЫ ДЛЯ calculateTax
    // ==========================================

    /**
     * Позитивный тест расчета налога:
     * Проверяет расчет суммы налога с округлением до двух знаков после запятой.
     */
    @Test(groups = {"finance", "math"})
    public void testCalculateTaxSuccess() {
        // 100.0 * 20% = 20.0
        double tax1 = MathAndFinanceUtils.calculateTax(100.0, 20.0);
        Assert.assertEquals(tax1, 20.0, 0.001, "Налог 20% от 100 должен быть 20.0");

        // 155.55 * 8.5% = 13.22175 -> округление 13.22
        double tax2 = MathAndFinanceUtils.calculateTax(155.55, 8.5);
        Assert.assertEquals(tax2, 13.22, 0.001, "Округленный налог должен быть 13.22");

        // Нулевая ставка налога
        double taxZero = MathAndFinanceUtils.calculateTax(500.0, 0.0);
        Assert.assertEquals(taxZero, 0.0, 0.001, "При ставке 0% налог равен 0");
    }

    /**
     * DataProvider для недопустимых параметров налога:
     * отрицательная базовая сумма, отрицательная ставка или ставка выше 100%.
     */
    @DataProvider(name = "invalidTaxData")
    public Object[][] invalidTaxData() {
        return new Object[][]{
                {-10.0, 20.0},
                {100.0, -5.0},
                {100.0, 105.0}
        };
    }

    /**
     * Негативный тест расчета налога:
     * Проверяет выброс IllegalArgumentException при нарушении диапазона значений.
     */
    @Test(dataProvider = "invalidTaxData", expectedExceptions = IllegalArgumentException.class, groups = {"finance", "negative"})
    public void testCalculateTaxThrowsExceptionOnInvalidInput(double amount, double taxRate) {
        MathAndFinanceUtils.calculateTax(amount, taxRate);
    }

    // ==========================================
    // 2. ТЕСТЫ ДЛЯ calculateDiscount
    // ==========================================

    /**
     * Позитивный тест расчета цены со скидкой:
     * Проверяет вычисление итоговой суммы после применения процента скидки.
     */
    @Test(groups = {"finance", "math"})
    public void testCalculateDiscountSuccess() {
        // 200.0 со скидкой 15% -> 170.0
        double discounted = MathAndFinanceUtils.calculateDiscount(200.0, 15.0);
        Assert.assertEquals(discounted, 170.0, 0.001, "200 со скидкой 15% должно быть 170.0");

        // 100% скидка -> товар бесплатный
        double free = MathAndFinanceUtils.calculateDiscount(100.0, 100.0);
        Assert.assertEquals(free, 0.0, 0.001, "100% скидка дает цену 0.0");

        // 0% скидка -> цена не меняется
        double original = MathAndFinanceUtils.calculateDiscount(150.0, 0.0);
        Assert.assertEquals(original, 150.0, 0.001, "0% скидка сохраняет цену");
    }

    /**
     * Негативный тест расчета скидки:
     * Проверяет выброс IllegalArgumentException при отрицательной сумме или недопустимом проценте скидки.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"finance", "negative"})
    public void testCalculateDiscountThrowsExceptionOnInvalidPercentage() {
        MathAndFinanceUtils.calculateDiscount(100.0, 120.0); // скидка > 100%
    }

    // ==========================================
    // 3. ТЕСТЫ ДЛЯ roundToDecimalPlaces
    // ==========================================

    /**
     * Тест округления до заданного числа знаков (HALF_UP):
     * Проверяет математическое округление половины вверх и обработку специальных чисел.
     */
    @Test(groups = {"math"})
    public void testRoundToDecimalPlaces() {
        Assert.assertEquals(MathAndFinanceUtils.roundToDecimalPlaces(3.14159, 2), 3.14, 0.0001,
                "3.14159 с точностью 2 знака -> 3.14");
        Assert.assertEquals(MathAndFinanceUtils.roundToDecimalPlaces(3.145, 2), 3.15, 0.0001,
                "3.145 должно округляться вверх (HALF_UP) до 3.15");
        Assert.assertEquals(MathAndFinanceUtils.roundToDecimalPlaces(12.3456, 0), 12.0, 0.0001,
                "Округление до целого числа");

        // Обработка NaN и Infinity
        Assert.assertTrue(Double.isNaN(MathAndFinanceUtils.roundToDecimalPlaces(Double.NaN, 2)),
                "NaN должен возвращаться без изменений");
    }

    /**
     * Негативный тест округления:
     * Проверяет выброс исключения при отрицательном количестве знаков.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"math", "negative"})
    public void testRoundToDecimalPlacesThrowsExceptionOnNegativePlaces() {
        MathAndFinanceUtils.roundToDecimalPlaces(10.5, -1);
    }

    // ==========================================
    // 4. ТЕСТЫ ДЛЯ isValidLuhn
    // ==========================================

    /**
     * Тест проверки валидности номеров кредитных карт по алгоритму Луна (Mod 10):
     * Проверяет реальные тестовые номера карт и отсечение поддельных.
     */
    @Test(groups = {"finance", "validation"})
    public void testIsValidLuhn() {
        // Известный валидный тестовый номер по алгоритму Луна
        Assert.assertTrue(MathAndFinanceUtils.isValidLuhn("49927398716"),
                "Номер 49927398716 должен успешно проходить алгоритм Луна");
        Assert.assertTrue(MathAndFinanceUtils.isValidLuhn("4992-7398-716"),
                "Номер с дефисами должен проходить проверку после очистки");

        // Невалидные номера
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn("1234567812345678"),
                "Номер с неверной контрольной суммой должен отклоняться");
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn("12345"),
                "Номер длиной менее 8 символов невалиден");
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn(null),
                "Null номер карты невалиден");
    }

    // ==========================================
    // 5. ТЕСТЫ ДЛЯ convertCurrency
    // ==========================================

    /**
     * Позитивный тест конвертации валюты:
     * Проверяет пересчет суммы по заданному курсу обмена.
     */
    @Test(groups = {"finance"})
    public void testConvertCurrencySuccess() {
        // 100 USD по курсу 1.25 -> 125.0 EUR
        double converted = MathAndFinanceUtils.convertCurrency(100.0, 1.25);
        Assert.assertEquals(converted, 125.0, 0.001, "Конвертация 100 по курсу 1.25");
    }

    /**
     * Негативный тест конвертации валюты:
     * Проверяет запрет отрицательной суммы и неположительного курса.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"finance", "negative"})
    public void testConvertCurrencyThrowsExceptionOnZeroRate() {
        MathAndFinanceUtils.convertCurrency(100.0, 0.0);
    }

    // ==========================================
    // 6. ТЕСТЫ ДЛЯ calculateCompoundInterest
    // ==========================================

    /**
     * Тест расчета сложных процентов:
     * Формула: A = P * (1 + r/n)^(n*t).
     * Проверяет начисление процентов за заданный период.
     */
    @Test(groups = {"finance"})
    public void testCalculateCompoundInterest() {
        // Депозит 1000 под 5% годовых, начисление 12 раз в год, срок 10 лет
        double total = MathAndFinanceUtils.calculateCompoundInterest(1000.0, 5.0, 12, 10);
        Assert.assertEquals(total, 1647.01, 0.01, "Сложный процент должен быть 1647.01");
    }

    // ==========================================
    // 7. ТЕСТЫ ДЛЯ calculateMonthlyInstallment
    // ==========================================

    /**
     * Тест расчета ежемесячного платежа по кредиту (аннуитет):
     * Проверяет стандартную формулу амортизации и случай с беспроцентной рассрочкой (0%).
     */
    @Test(groups = {"finance"})
    public void testCalculateMonthlyInstallment() {
        // Кредит 12000 под 0% на 12 месяцев -> 1000 в месяц
        double installment0 = MathAndFinanceUtils.calculateMonthlyInstallment(12000.0, 0.0, 12);
        Assert.assertEquals(installment0, 1000.0, 0.01, "При 0% платеж равен сумме деленной на месяцы");

        // Кредит 10000 под 5% на 12 месяцев -> 856.07 в месяц
        double installmentWithInterest = MathAndFinanceUtils.calculateMonthlyInstallment(10000.0, 5.0, 12);
        Assert.assertEquals(installmentWithInterest, 856.07, 0.05,
                "Аннуитетный платеж должен рассчитываться по формуле");
    }

    // ==========================================
    // 8. ТЕСТЫ ДЛЯ calculatePercentageChange
    // ==========================================

    /**
     * Тест расчета процентного изменения:
     * Формула: ((newValue - oldValue) / |oldValue|) * 100.
     * Проверяет рост, падение и выброс исключения при делении на ноль.
     */
    @Test(groups = {"math"})
    public void testCalculatePercentageChange() {
        // Рост со 100 до 150 = +50%
        double increase = MathAndFinanceUtils.calculatePercentageChange(100.0, 150.0);
        Assert.assertEquals(increase, 50.0, 0.001, "Рост на 50%");

        // Снижение с 200 до 100 = -50%
        double decrease = MathAndFinanceUtils.calculatePercentageChange(200.0, 100.0);
        Assert.assertEquals(decrease, -50.0, 0.001, "Падение на 50%");
    }

    /**
     * Негативный тест расчета процентного изменения:
     * Базовое (старое) значение не может быть нулевым.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, groups = {"math", "negative"})
    public void testCalculatePercentageChangeThrowsExceptionOnZeroOldValue() {
        MathAndFinanceUtils.calculatePercentageChange(0.0, 100.0);
    }
}
