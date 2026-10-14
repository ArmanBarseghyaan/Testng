package student.tests;

import am.gortsup.testing.exception.IllegalOrderStateException;
import am.gortsup.testing.exception.PaymentFailedException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.*;
import am.gortsup.testing.service.OrderService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Тестовый класс для проверки жизненного цикла заказов в OrderService.
 * Покрывает полный конечный автомат заказа: создание PENDING -> оплата PAID ->
 * отправка SHIPPED -> доставка DELIVERED, а также валидацию карты по алгоритму Луна,
 * правила отмены (cancel) и возврата средств (refund).
 */
public class OrderServiceTest {

    private OrderService orderService;
    private Address address;
    private List<CartItem> sampleItems;
    private static final String VALID_LUHN_CARD = "49927398716";
    private static final String INVALID_LUHN_CARD = "1234567812345678";

    /**
     * Перед каждым тестом инициализируется чистый сервис заказов и типовые данные.
     */
    @BeforeMethod
    public void setUp() {
        orderService = new OrderService();
        address = new Address("45 Abovyan St", "Yerevan", "Kentron", "0001", "AM");
        Product product = new Product("PROD-10", "Mechanical Keyboard", ProductCategory.ELECTRONICS, 100.0);
        sampleItems = List.of(new CartItem(product, 1)); // 1 * 100.0
    }

    // ==========================================
    // 1. ТЕСТЫ СОЗДАНИЯ И РАСЧЕТА ЗАКАЗА
    // ==========================================

    /**
     * Позитивный тест создания заказа:
     * Проверяет генерацию ID, статус PENDING и расчет итоговой суммы с налогом 20%.
     * (100.0 база + 20% налог = 120.0).
     */
    @Test(groups = {"order", "smoke"})
    public void testCreatePendingOrder() {
        Order order = orderService.createOrder("user-100", sampleItems, address, 20.0, null);

        Assert.assertNotNull(order.getId(), "У заказа должен быть сгенерирован ID");
        Assert.assertEquals(order.getStatus(), OrderStatus.PENDING, "Начальный статус заказа должен быть PENDING");
        Assert.assertEquals(order.getSubtotal(), 100.0);
        Assert.assertEquals(order.getTax(), 20.0);
        Assert.assertEquals(order.getTotal(), 120.0);
    }

    /**
     * Негативный тест создания заказа с пустым списком позиций:
     * Должен выбрасывать ValidationException.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"order", "negative"})
    public void testCreateOrderWithEmptyItemsThrowsException() {
        orderService.createOrder("user-101", List.of(), address, 10.0, null);
    }

    // ==========================================
    // 2. ТЕСТЫ ОПЛАТЫ ЗАКАЗА
    // ==========================================

    /**
     * Позитивный тест успешной оплаты:
     * Проверяет проверку карты по алгоритму Луна, совпадение суммы оплаты
     * и переход статуса заказа в PAID.
     */
    @Test(groups = {"order", "payment", "smoke"})
    public void testPayOrderSuccess() {
        Order order = orderService.createOrder("user-102", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 100.0);

        Order paidOrder = orderService.getOrder(order.getId());
        Assert.assertEquals(paidOrder.getStatus(), OrderStatus.PAID, "Статус должен измениться на PAID");
    }

    /**
     * Негативный тест оплаты картой с неверной контрольной суммой:
     * Проверяет выброс PaymentFailedException при непрохождении проверки Луна.
     */
    @Test(expectedExceptions = PaymentFailedException.class, groups = {"order", "payment", "negative"})
    public void testPayOrderWithInvalidLuhnCardThrowsException() {
        Order order = orderService.createOrder("user-103", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), INVALID_LUHN_CARD, 100.0);
    }

    /**
     * Негативный тест оплаты с несовпадением суммы:
     * Проверяет отказ в оплате, если передана сумма меньше или больше суммы заказа.
     */
    @Test(expectedExceptions = PaymentFailedException.class, groups = {"order", "payment", "negative"})
    public void testPayOrderWithAmountMismatchThrowsException() {
        Order order = orderService.createOrder("user-104", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 50.0); // ожидается 100.0
    }

    // ==========================================
    // 3. ТЕСТЫ ДОСТАВКИ И ЖИЗНЕННОГО ЦИКЛА
    // ==========================================

    /**
     * Негативный тест: запрет отправки (shipOrder) неоплаченного заказа со статусом PENDING.
     * Должен выбрасывать IllegalOrderStateException.
     */
    @Test(expectedExceptions = IllegalOrderStateException.class, groups = {"order", "negative"})
    public void testCannotShipUnpaidOrder() {
        Order order = orderService.createOrder("user-105", sampleItems, address, 0.0, null);
        orderService.shipOrder(order.getId(), "TRACK-12345");
    }

    /**
     * Позитивный тест полного цикла: Оплата -> Отправка -> Доставка покупателю.
     */
    @Test(groups = {"order", "lifecycle"})
    public void testFullOrderDeliveryCycle() {
        Order order = orderService.createOrder("user-106", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 100.0);

        // Отправка
        orderService.shipOrder(order.getId(), "TRACK-99999");
        Order shippedOrder = orderService.getOrder(order.getId());
        Assert.assertEquals(shippedOrder.getStatus(), OrderStatus.SHIPPED);
        Assert.assertEquals(shippedOrder.getTrackingNumber(), "TRACK-99999");

        // Доставка
        orderService.deliverOrder(order.getId());
        Order deliveredOrder = orderService.getOrder(order.getId());
        Assert.assertEquals(deliveredOrder.getStatus(), OrderStatus.DELIVERED);
    }

    /**
     * Негативный тест: попытка перевести в доставлен (deliver) заказ, который еще не был отправлен.
     */
    @Test(expectedExceptions = IllegalOrderStateException.class, groups = {"order", "negative"})
    public void testCannotDeliverOrderNotShipped() {
        Order order = orderService.createOrder("user-107", sampleItems, address, 0.0, null);
        orderService.deliverOrder(order.getId());
    }

    // ==========================================
    // 4. ТЕСТЫ ОТМЕНЫ ЗАКАЗА (CANCEL)
    // ==========================================

    /**
     * Позитивный тест отмены неоплаченного заказа PENDING.
     */
    @Test(groups = {"order"})
    public void testCancelPendingOrderSuccess() {
        Order order = orderService.createOrder("user-108", sampleItems, address, 0.0, null);
        orderService.cancelOrder(order.getId());

        Assert.assertEquals(orderService.getOrder(order.getId()).getStatus(), OrderStatus.CANCELLED);
    }

    /**
     * Негативный тест отмены отправленного заказа:
     * Бизнес-правило запрещает отмену заказов в статусе SHIPPED или DELIVERED.
     */
    @Test(expectedExceptions = IllegalOrderStateException.class, groups = {"order", "negative"})
    public void testCannotCancelShippedOrder() {
        Order order = orderService.createOrder("user-109", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 100.0);
        orderService.shipOrder(order.getId(), "TRACK-ABC");

        // Попытка отменить уже отправленный заказ
        orderService.cancelOrder(order.getId());
    }

    // ==========================================
    // 5. ТЕСТЫ ВОЗВРАТА СРЕДСТВ (REFUND)
    // ==========================================

    /**
     * Позитивный тест возврата средств:
     * Проверяет возврат оплаченного заказа и смену статуса на REFUNDED.
     */
    @Test(groups = {"order", "finance"})
    public void testRefundPaidOrderSuccess() {
        Order order = orderService.createOrder("user-110", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 100.0);

        orderService.refundOrder(order.getId(), 100.0);
        Assert.assertEquals(orderService.getOrder(order.getId()).getStatus(), OrderStatus.REFUNDED);
    }

    /**
     * Негативный тест возврата средств:
     * Запрещен возврат по заказу, который никогда не был оплачен (PENDING).
     */
    @Test(expectedExceptions = IllegalOrderStateException.class, groups = {"order", "negative"})
    public void testCannotRefundUnpaidOrderThrowsException() {
        Order order = orderService.createOrder("user-111", sampleItems, address, 0.0, null);
        orderService.refundOrder(order.getId(), 50.0);
    }

    /**
     * Негативный тест возврата средств:
     * Сумма возврата не может превышать общую стоимость заказа.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"order", "negative"})
    public void testRefundAmountExceedingTotalThrowsException() {
        Order order = orderService.createOrder("user-112", sampleItems, address, 0.0, null);
        orderService.payOrder(order.getId(), VALID_LUHN_CARD, 100.0);

        // Попытка вернуть 150 при стоимости 100
        orderService.refundOrder(order.getId(), 150.0);
    }

    /**
     * Тест поиска заказов по пользователю и по статусу.
     */
    @Test(groups = {"order"})
    public void testGetOrdersByUserAndStatus() {
        orderService.createOrder("target-user", sampleItems, address, 0.0, null);
        orderService.createOrder("target-user", sampleItems, address, 0.0, null);
        orderService.createOrder("other-user", sampleItems, address, 0.0, null);

        List<Order> userOrders = orderService.getOrdersByUser("target-user");
        Assert.assertEquals(userOrders.size(), 2, "У целевого пользователя должно быть 2 заказа");

        List<Order> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        Assert.assertEquals(pendingOrders.size(), 3, "Все 3 созданных заказа находятся в PENDING");
    }
}
