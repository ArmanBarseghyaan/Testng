package student.tests;

import am.gortsup.testing.exception.InvalidCouponException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.*;
import am.gortsup.testing.service.ShoppingCartService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;

/**
 * Тестовый класс для проверки ShoppingCartService.
 * Покрывает добавление товаров в корзину, лимиты количества (1-99),
 * обновление позиций, удаление, очистку корзины, расчет промежуточных итогов (subtotal),
 * применение процентных и фиксированных купонов, валидацию сроков действия купона
 * и расчет финальной стоимости с учетом налогов.
 */
public class ShoppingCartServiceTest {

    private ShoppingCartService cartService;
    private Product activeProduct1;
    private Product activeProduct2;
    private Product inactiveProduct;

    /**
     * Перед каждым тестом инициализируется чистый сервис корзины и тестовые товары.
     */
    @BeforeMethod
    public void setUp() {
        cartService = new ShoppingCartService();
        activeProduct1 = new Product("P-1", "Wireless Mouse", ProductCategory.ELECTRONICS, 50.0);
        activeProduct2 = new Product("P-2", "USB-C Cable", ProductCategory.ELECTRONICS, 20.0);
        inactiveProduct = new Product("P-INACTIVE", "Old Model", ProductCategory.ELECTRONICS, 30.0);
        inactiveProduct.setActive(false);
    }

    // ==========================================
    // 1. ТЕСТЫ ДОБАВЛЕНИЯ И ИЗМЕНЕНИЯ ТОВАРОВ
    // ==========================================

    /**
     * Позитивный тест добавления товаров в корзину:
     * Проверяет расчет количества единиц и промежуточной суммы subtotal.
     */
    @Test(groups = {"cart", "smoke"})
    public void testAddItemsAndCalculateSubtotal() {
        cartService.addItem("cart-1", activeProduct1, 2); // 2 * 50 = 100
        cartService.addItem("cart-1", activeProduct2, 1); // 1 * 20 = 20

        Assert.assertEquals(cartService.getItemCount("cart-1"), 3, "Общее число товаров должно быть 3");
        Assert.assertEquals(cartService.calculateSubtotal("cart-1"), 120.0, 0.01,
                "Subtotal должен составлять 120.0");
    }

    /**
     * Позитивный тест инкремента количества при повторном добавлении того же товара.
     */
    @Test(groups = {"cart"})
    public void testAddSameItemIncrementsQuantity() {
        cartService.addItem("cart-2", activeProduct1, 2);
        cartService.addItem("cart-2", activeProduct1, 3);

        Assert.assertEquals(cartService.getItemCount("cart-2"), 5, "Количество товара должно увеличиться до 5");
        Assert.assertEquals(cartService.getCartItems("cart-2").size(), 1, "В корзине должна быть одна позиция");
    }

    /**
     * Негативный тест: запрет добавления неактивного товара в корзину.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"cart", "negative"})
    public void testAddInactiveProductThrowsException() {
        cartService.addItem("cart-3", inactiveProduct, 1);
    }

    /**
     * Негативный тест: ограничение максимального количества товара (не более 99 штук).
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"cart", "negative"})
    public void testAddItemExceedingMaxLimitThrowsException() {
        cartService.addItem("cart-4", activeProduct1, 100);
    }

    /**
     * Позитивный тест обновления количества товара (updateQuantity).
     */
    @Test(groups = {"cart"})
    public void testUpdateQuantitySuccess() {
        cartService.addItem("cart-5", activeProduct1, 2);
        cartService.updateQuantity("cart-5", "P-1", 5);

        Assert.assertEquals(cartService.getItemCount("cart-5"), 5, "Количество должно обновиться до 5");
        Assert.assertEquals(cartService.calculateSubtotal("cart-5"), 250.0, 0.01);
    }

    /**
     * Тест удаления товара из корзины (removeItem).
     */
    @Test(groups = {"cart"})
    public void testRemoveItemSuccess() {
        cartService.addItem("cart-6", activeProduct1, 2);
        cartService.removeItem("cart-6", "P-1");

        Assert.assertEquals(cartService.getItemCount("cart-6"), 0, "Корзина должна стать пустой");
        Assert.assertEquals(cartService.calculateSubtotal("cart-6"), 0.0, 0.01);
    }

    /**
     * Негативный тест удаления несуществующего товара:
     * Должен выбрасывать ResourceNotFoundException.
     */
    @Test(expectedExceptions = ResourceNotFoundException.class, groups = {"cart", "negative"})
    public void testRemoveNonExistentItemThrowsException() {
        cartService.addItem("cart-7", activeProduct1, 1);
        cartService.removeItem("cart-7", "NON-EXISTENT");
    }

    // ==========================================
    // 2. ТЕСТЫ КУПОНОВ И СКИДОК
    // ==========================================

    /**
     * Тест применения процентного купона:
     * Проверяет расчет суммы скидки и финального итога.
     */
    @Test(groups = {"cart", "coupon"})
    public void testApplyPercentageCoupon() {
        cartService.addItem("cart-8", activeProduct1, 2); // subtotal = 100.0

        // Купон на 20% скидку при минимальном заказе от 50.0
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 50.0,
                LocalDate.now().plusDays(10), 100);

        cartService.applyCoupon("cart-8", coupon, LocalDate.now());

        double discount = cartService.calculateDiscountAmount("cart-8");
        Assert.assertEquals(discount, 20.0, 0.01, "Скидка 20% от 100 должна быть 20.0");

        // Расчет итоговой стоимости с 10% налогом: (100 - 20) + 8.0 = 88.0
        double total = cartService.calculateTotal("cart-8", 10.0);
        Assert.assertEquals(total, 88.0, 0.01, "Итоговая сумма: 80 база + 8 налог = 88");
    }

    /**
     * Тест применения купона с фиксированной суммой скидки:
     * Проверяет списание фиксированной суммы (например, 15 долларов).
     */
    @Test(groups = {"cart", "coupon"})
    public void testApplyFixedAmountCoupon() {
        cartService.addItem("cart-9", activeProduct1, 2); // subtotal = 100.0

        Coupon fixedCoupon = new Coupon("MINUS15", DiscountType.FIXED_AMOUNT, 15.0, 50.0,
                LocalDate.now().plusDays(10), 50);

        cartService.applyCoupon("cart-9", fixedCoupon, LocalDate.now());
        Assert.assertEquals(cartService.calculateDiscountAmount("cart-9"), 15.0, 0.01,
                "Фиксированная скидка должна составлять 15.0");
    }

    /**
     * Негативный тест: применение просроченного купона выбрасывает InvalidCouponException.
     */
    @Test(expectedExceptions = InvalidCouponException.class, groups = {"cart", "coupon", "negative"})
    public void testApplyExpiredCouponThrowsException() {
        cartService.addItem("cart-10", activeProduct1, 2);

        // Купон истек вчера
        Coupon expired = new Coupon("EXPIRED", DiscountType.PERCENTAGE, 10.0, 10.0,
                LocalDate.now().minusDays(1), 10);

        cartService.applyCoupon("cart-10", expired, LocalDate.now());
    }

    /**
     * Негативный тест: применение купона при невыполнении минимальной суммы заказа minOrderAmount.
     */
    @Test(expectedExceptions = InvalidCouponException.class, groups = {"cart", "coupon", "negative"})
    public void testApplyCouponBelowMinOrderAmountThrowsException() {
        cartService.addItem("cart-11", activeProduct2, 1); // subtotal = 20.0

        // Минимальная сумма заказа 100.0
        Coupon highMinCoupon = new Coupon("BIGORDER", DiscountType.FIXED_AMOUNT, 10.0, 100.0,
                LocalDate.now().plusDays(5), 10);

        cartService.applyCoupon("cart-11", highMinCoupon, LocalDate.now());
    }

    /**
     * Тест очистки корзины (clearCart):
     * Проверяет удаление всех позиций и сброс купона.
     */
    @Test(groups = {"cart"})
    public void testClearCart() {
        cartService.addItem("cart-12", activeProduct1, 2);
        cartService.clearCart("cart-12");

        Assert.assertEquals(cartService.getItemCount("cart-12"), 0, "Корзина должна быть пустой");
        Assert.assertEquals(cartService.calculateSubtotal("cart-12"), 0.0);
        Assert.assertTrue(cartService.getAppliedCoupon("cart-12").isEmpty(), "Купон должен быть сброшен");
    }
}
