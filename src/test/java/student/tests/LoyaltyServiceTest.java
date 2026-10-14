package student.tests;

import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.LoyaltyTier;
import am.gortsup.testing.service.LoyaltyService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки LoyaltyService.
 * Покрывает начисление и списание баллов лояльности, анализ граничных значений
 * для уровней лояльности (BRONZE, SILVER, GOLD, PLATINUM), расчет скидок по уровню
 * и перевод баллов между пользователями.
 */
public class LoyaltyServiceTest {

    private LoyaltyService loyaltyService;

    /**
     * Перед каждым тестом создается новый экземпляр сервиса лояльности.
     */
    @BeforeMethod
    public void setUp() {
        loyaltyService = new LoyaltyService();
    }

    // ==========================================
    // 1. ТЕСТЫ НАЧИСЛЕНИЯ И СПИСАНИЯ БАЛЛОВ
    // ==========================================

    /**
     * Позитивный тест начисления баллов:
     * Проверяет одновременное обновление текущего доступного баланса и пожизненных баллов (lifetime).
     */
    @Test(groups = {"loyalty", "smoke"})
    public void testAddPointsSuccess() {
        loyaltyService.addPoints("user-1", 500);

        Assert.assertEquals(loyaltyService.getPoints("user-1"), 500, "Текущий баланс должен быть 500");
        Assert.assertEquals(loyaltyService.getLifetimePoints("user-1"), 500, "Пожизненный баланс должен быть 500");
    }

    /**
     * Негативный тест: запрет начисления нулевого или отрицательного количества баллов.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"loyalty", "negative"})
    public void testAddNegativePointsThrowsException() {
        loyaltyService.addPoints("user-1", -10);
    }

    /**
     * Позитивный тест списания баллов (redeem):
     * Проверяет, что списание уменьшает текущий баланс, но НЕ уменьшает пожизненные баллы (lifetime).
     */
    @Test(groups = {"loyalty"})
    public void testRedeemPointsPreservesLifetime() {
        loyaltyService.addPoints("user-2", 1500); // 1500 текущих и 1500 lifetime
        loyaltyService.redeemPoints("user-2", 500);

        Assert.assertEquals(loyaltyService.getPoints("user-2"), 1000, "Текущий баланс должен уменьшиться до 1000");
        Assert.assertEquals(loyaltyService.getLifetimePoints("user-2"), 1500,
                "Пожизненные баллы не должны уменьшаться при списании");
    }

    /**
     * Негативный тест списания баллов:
     * Проверяет выброс ValidationException при попытке списать больше баллов, чем есть на балансе.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"loyalty", "negative"})
    public void testRedeemPointsInsufficientBalanceThrowsException() {
        loyaltyService.addPoints("user-3", 100);
        loyaltyService.redeemPoints("user-3", 150);
    }

    // ==========================================
    // 2. ГРАНИЧНЫЕ ТЕСТЫ УРОВНЕЙ ЛОЯЛЬНОСТИ
    // ==========================================

    /**
     * DataProvider для анализа граничных значений уровней лояльности:
     * - 0 to 999: BRONZE
     * - 1000 to 4999: SILVER
     * - 5000 to 9999: GOLD
     * - 10000+: PLATINUM
     */
    @DataProvider(name = "tierBoundaryData")
    public Object[][] tierBoundaryData() {
        return new Object[][]{
                {0, LoyaltyTier.BRONZE},
                {999, LoyaltyTier.BRONZE},
                {1000, LoyaltyTier.SILVER},
                {4999, LoyaltyTier.SILVER},
                {5000, LoyaltyTier.GOLD},
                {9999, LoyaltyTier.GOLD},
                {10000, LoyaltyTier.PLATINUM},
                {25000, LoyaltyTier.PLATINUM}
        };
    }

    /**
     * Граничный тест расчета уровня лояльности:
     * Проверяет точные граничные переходы между уровнями.
     */
    @Test(dataProvider = "tierBoundaryData", groups = {"loyalty", "boundary"})
    public void testCalculateTierBoundaries(int points, LoyaltyTier expectedTier) {
        LoyaltyTier actualTier = loyaltyService.calculateTier(points);
        Assert.assertEquals(actualTier, expectedTier,
                "При " + points + " баллах должен присваиваться уровень " + expectedTier);
    }

    /**
     * Тест проверки скидки для каждого уровня лояльности:
     * BRONZE (0%), SILVER (5%), GOLD (10%), PLATINUM (15%).
     */
    @Test(groups = {"loyalty", "finance"})
    public void testTierDiscountPercentages() {
        Assert.assertEquals(loyaltyService.getTierDiscountPercentage(LoyaltyTier.BRONZE), 0.0);
        Assert.assertEquals(loyaltyService.getTierDiscountPercentage(LoyaltyTier.SILVER), 5.0);
        Assert.assertEquals(loyaltyService.getTierDiscountPercentage(LoyaltyTier.GOLD), 10.0);
        Assert.assertEquals(loyaltyService.getTierDiscountPercentage(LoyaltyTier.PLATINUM), 15.0);
        Assert.assertEquals(loyaltyService.getTierDiscountPercentage(null), 0.0, "Null уровень дает 0%");
    }

    // ==========================================
    // 3. ТЕСТЫ ПЕРЕВОДА БАЛЛОВ (TRANSFER)
    // ==========================================

    /**
     * Позитивный тест перевода баллов между двумя пользователями:
     * Проверяет уменьшение баланса у отправителя и увеличение баланса и lifetime у получателя.
     */
    @Test(groups = {"loyalty"})
    public void testTransferPointsSuccess() {
        loyaltyService.addPoints("sender", 300);
        loyaltyService.addPoints("receiver", 50);

        loyaltyService.transferPoints("sender", "receiver", 100);

        Assert.assertEquals(loyaltyService.getPoints("sender"), 200, "У отправителя должно остаться 200");
        Assert.assertEquals(loyaltyService.getPoints("receiver"), 150, "У получателя должно стать 150");
        Assert.assertEquals(loyaltyService.getLifetimePoints("receiver"), 150, "Lifetime получателя вырос на 100");
    }

    /**
     * Негативный тест: запрет перевода баллов самому себе.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"loyalty", "negative"})
    public void testTransferPointsToSelfThrowsException() {
        loyaltyService.addPoints("self_user", 500);
        loyaltyService.transferPoints("self_user", "self_user", 100);
    }

    /**
     * Негативный тест: запрет перевода при недостаточном балансе отправителя.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"loyalty", "negative"})
    public void testTransferPointsInsufficientBalanceThrowsException() {
        loyaltyService.addPoints("poor_sender", 50);
        loyaltyService.transferPoints("poor_sender", "receiver", 100);
    }
}
