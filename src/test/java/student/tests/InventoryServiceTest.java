package student.tests;

import am.gortsup.testing.exception.InsufficientStockException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.service.InventoryService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Тестовый класс для проверки InventoryService.
 * Покрывает управление остатками на складе, проверку наличия (isInStock),
 * атомарное резервирование остатков, освобождение резерва (release),
 * списание резерва при покупке (deduct) и пополнение склада (restock).
 */
public class InventoryServiceTest {

    private InventoryService inventoryService;

    /**
     * Перед каждым тестом инициализируется чистый экземпляр сервиса инвентаря.
     */
    @BeforeMethod
    public void setUp() {
        inventoryService = new InventoryService();
    }

    // ==========================================
    // 1. ТЕСТЫ УСТАНОВКИ И ПРОВЕРКИ НАЛИЧИЯ
    // ==========================================

    /**
     * Позитивный тест установки остатка товара:
     * Проверяет корректное сохранение доступного остатка и инициализацию резерва значением 0.
     */
    @Test(groups = {"inventory", "smoke"})
    public void testSetStockAndGetStock() {
        inventoryService.setStock("PROD-1", 50);

        Assert.assertEquals(inventoryService.getStock("PROD-1"), 50, "Доступный остаток должен быть 50");
        Assert.assertEquals(inventoryService.getReservedStock("PROD-1"), 0, "Резерв должен быть равен 0");
    }

    /**
     * Тест получения остатка по неотслеживаемому или null товару:
     * Должен безопасно возвращать 0.
     */
    @Test(groups = {"inventory"})
    public void testGetStockUnlistedProductReturnsZero() {
        Assert.assertEquals(inventoryService.getStock("UNKNOWN"), 0, "Неизвестный товар возвращает 0");
        Assert.assertEquals(inventoryService.getStock(null), 0, "Null товар возвращает 0");
    }

    /**
     * Негативный тест установки отрицательного остатка:
     * Проверяет выброс ValidationException.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"inventory", "negative"})
    public void testSetStockNegativeQuantityThrowsException() {
        inventoryService.setStock("PROD-1", -10);
    }

    /**
     * Тест проверки наличия (isInStock):
     * Проверяет достаточность доступного остатка и обработку неположительного запроса.
     */
    @Test(groups = {"inventory"})
    public void testIsInStock() {
        inventoryService.setStock("PROD-2", 10);

        Assert.assertTrue(inventoryService.isInStock("PROD-2", 5), "5 единиц при наличии 10 должно быть true");
        Assert.assertTrue(inventoryService.isInStock("PROD-2", 10), "Точное совпадение (граница) должно быть true");
        Assert.assertFalse(inventoryService.isInStock("PROD-2", 11), "Запрос больше остатка должен возвращать false");
        Assert.assertFalse(inventoryService.isInStock("PROD-2", 0), "Запрос 0 единиц возвращает false");
    }

    // ==========================================
    // 2. ТЕСТЫ РЕЗЕРВИРОВАНИЯ И ОСВОБОЖДЕНИЯ
    // ==========================================

    /**
     * Позитивный тест резервирования остатка:
     * Проверяет перевод количества из доступного остатка в зарезервированный.
     */
    @Test(groups = {"inventory"})
    public void testReserveStockSuccess() {
        inventoryService.setStock("PROD-3", 100);

        inventoryService.reserveStock("PROD-3", 30);
        Assert.assertEquals(inventoryService.getStock("PROD-3"), 70, "Доступный остаток должен уменьшиться на 30");
        Assert.assertEquals(inventoryService.getReservedStock("PROD-3"), 30, "Резерв должен увеличиться на 30");
    }

    /**
     * Негативный тест резервирования при нехватке товара:
     * Проверяет выброс InsufficientStockException при попытке зарезервировать больше, чем есть на складе.
     */
    @Test(expectedExceptions = InsufficientStockException.class, groups = {"inventory", "negative"})
    public void testReserveStockInsufficientThrowsException() {
        inventoryService.setStock("PROD-4", 15);
        // Запрос 20 при наличии 15
        inventoryService.reserveStock("PROD-4", 20);
    }

    /**
     * Позитивный тест освобождения зарезервированного товара (например, при отмене заказа):
     * Проверяет возврат зарезервированного товара обратно в доступный остаток.
     */
    @Test(groups = {"inventory"})
    public void testReleaseStockSuccess() {
        inventoryService.setStock("PROD-5", 50);
        inventoryService.reserveStock("PROD-5", 20); // Склад: 30 доступно, 20 в резерве

        inventoryService.releaseStock("PROD-5", 15);
        Assert.assertEquals(inventoryService.getStock("PROD-5"), 45, "Доступно должно стать 45");
        Assert.assertEquals(inventoryService.getReservedStock("PROD-5"), 5, "В резерве должно остаться 5");
    }

    /**
     * Негативный тест освобождения резерва:
     * Проверяет запрет освобождения количества, превышающего текущий резерв.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"inventory", "negative"})
    public void testReleaseStockExceedingReservedThrowsException() {
        inventoryService.setStock("PROD-6", 50);
        inventoryService.reserveStock("PROD-6", 10);
        // Попытка освободить 15 при резерве 10
        inventoryService.releaseStock("PROD-6", 15);
    }

    // ==========================================
    // 3. ТЕСТЫ СПИСАНИЯ И ПОПОЛНЕНИЯ
    // ==========================================

    /**
     * Позитивный тест списания зарезервированного товара при завершении покупки (deductStock):
     * Проверяет окончательное списание из резерва без изменения доступного остатка.
     */
    @Test(groups = {"inventory"})
    public void testDeductStockSuccess() {
        inventoryService.setStock("PROD-7", 40);
        inventoryService.reserveStock("PROD-7", 25); // Склад: 15 доступно, 25 резерв

        inventoryService.deductStock("PROD-7", 25);
        Assert.assertEquals(inventoryService.getStock("PROD-7"), 15, "Доступный остаток остается 15");
        Assert.assertEquals(inventoryService.getReservedStock("PROD-7"), 0, "Резерв полностью списан в 0");
    }

    /**
     * Позитивный тест пополнения остатка товара (restock):
     * Проверяет добавление новых единиц товара в доступный остаток.
     */
    @Test(groups = {"inventory"})
    public void testRestockSuccess() {
        inventoryService.setStock("PROD-8", 20);
        inventoryService.restock("PROD-8", 30);

        Assert.assertEquals(inventoryService.getStock("PROD-8"), 50, "Остаток должен увеличиться до 50");
    }
}
