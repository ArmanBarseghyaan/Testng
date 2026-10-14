package student.tests;

import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.model.ProductCategory;
import am.gortsup.testing.service.ProductCatalogService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Тестовый класс для проверки ProductCatalogService.
 * Покрывает добавление товаров, валидацию полей и уникальности ID, поиск по ID и категории,
 * текстовый поиск по названию, фильтрацию по диапазону цен, активацию/деактивацию
 * и массовое применение скидок к категориям товаров.
 */
public class ProductCatalogServiceTest {

    private ProductCatalogService catalogService;

    /**
     * Перед каждым тестом инициализируется чистый экземпляр каталога.
     */
    @BeforeMethod
    public void setUp() {
        catalogService = new ProductCatalogService();
    }

    // ==========================================
    // 1. ТЕСТЫ ДОБАВЛЕНИЯ ТОВАРОВ
    // ==========================================

    /**
     * Позитивный тест добавления товара в каталог:
     * Проверяет успешное сохранение товара и корректность полей.
     */
    @Test(groups = {"catalog", "smoke"})
    public void testAddProductSuccess() {
        Product product = new Product("P-101", "Gaming Laptop", ProductCategory.ELECTRONICS, 1500.0);
        Product saved = catalogService.addProduct(product);

        Assert.assertNotNull(saved, "Сохраненный товар не должен быть null");
        Assert.assertEquals(saved.getId(), "P-101");
        Assert.assertEquals(saved.getPrice(), 1500.0);
        Assert.assertTrue(saved.isActive(), "Новый товар по умолчанию активен");
    }

    /**
     * Негативный тест: запрет добавления товара с уже существующим ID.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"catalog", "negative"})
    public void testAddProductDuplicateIdThrowsException() {
        Product p1 = new Product("P-DUP", "Mouse", ProductCategory.ELECTRONICS, 25.0);
        Product p2 = new Product("P-DUP", "Keyboard", ProductCategory.ELECTRONICS, 50.0);

        catalogService.addProduct(p1);
        catalogService.addProduct(p2); // выброс ValidationException
    }

    /**
     * Негативный тест: запрет добавления товара с отрицательной ценой.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"catalog", "negative"})
    public void testAddProductNegativePriceThrowsException() {
        Product p = new Product("P-NEG", "Monitor", ProductCategory.ELECTRONICS, -100.0);
        catalogService.addProduct(p);
    }

    /**
     * Негативный тест: проверка обязательности названия и категории.
     */
    @Test(expectedExceptions = ValidationException.class, groups = {"catalog", "negative"})
    public void testAddProductEmptyNameThrowsException() {
        Product p = new Product("P-EMPTY", "   ", ProductCategory.BOOKS, 15.0);
        catalogService.addProduct(p);
    }

    // ==========================================
    // 2. ТЕСТЫ ПОИСКА И ФИЛЬТРАЦИИ
    // ==========================================

    /**
     * Позитивный тест поиска товара по идентификатору.
     */
    @Test(groups = {"catalog"})
    public void testGetProductByIdSuccess() {
        Product product = new Product("P-FOUND", "Java Book", ProductCategory.BOOKS, 45.0);
        catalogService.addProduct(product);

        Product found = catalogService.getProductById("P-FOUND");
        Assert.assertEquals(found.getName(), "Java Book");
    }

    /**
     * Негативный тест поиска по несуществующему ID.
     */
    @Test(expectedExceptions = ResourceNotFoundException.class, groups = {"catalog", "negative"})
    public void testGetProductByIdNotFoundThrowsException() {
        catalogService.getProductById("P-NON-EXISTENT");
    }

    /**
     * Тест фильтрации товаров по категории:
     * Проверяет, что возвращаются только активные товары выбранной категории.
     */
    @Test(groups = {"catalog"})
    public void testGetProductsByCategory() {
        catalogService.addProduct(new Product("P1", "Headphones", ProductCategory.ELECTRONICS, 80.0));
        catalogService.addProduct(new Product("P2", "Smartphone", ProductCategory.ELECTRONICS, 600.0));
        catalogService.addProduct(new Product("P3", "T-Shirt", ProductCategory.CLOTHING, 25.0));

        List<Product> electronics = catalogService.getProductsByCategory(ProductCategory.ELECTRONICS);
        Assert.assertEquals(electronics.size(), 2, "Должно быть 2 товара в категории ELECTRONICS");

        List<Product> clothing = catalogService.getProductsByCategory(ProductCategory.CLOTHING);
        Assert.assertEquals(clothing.size(), 1, "Должен быть 1 товар в категории CLOTHING");
    }

    /**
     * Тест поиска товаров по фрагменту названия (case-insensitive):
     * Проверяет регистронезависимый поиск подстроки среди активных товаров.
     */
    @Test(groups = {"catalog"})
    public void testSearchProductsByName() {
        catalogService.addProduct(new Product("P1", "Apple iPhone 15", ProductCategory.ELECTRONICS, 999.0));
        catalogService.addProduct(new Product("P2", "Apple MacBook Air", ProductCategory.ELECTRONICS, 1200.0));
        catalogService.addProduct(new Product("P3", "Samsung Galaxy S24", ProductCategory.ELECTRONICS, 850.0));

        List<Product> results = catalogService.searchProductsByName("apple");
        Assert.assertEquals(results.size(), 2, "Поиск по слову 'apple' должен найти 2 товара");

        List<Product> galaxyResults = catalogService.searchProductsByName("GALAXY");
        Assert.assertEquals(galaxyResults.size(), 1, "Поиск по слову 'GALAXY' должен найти 1 товар");
    }

    // ==========================================
    // 3. ТЕСТЫ ДЕАКТИВАЦИИ И ЦЕНОВОГО ДИАПАЗОНА
    // ==========================================

    /**
     * Тест деактивации и активации товара:
     * Проверяет, что деактивированный товар скрывается из поиска и категорий,
     * а после активации снова становится видимым.
     */
    @Test(groups = {"catalog"})
    public void testDeactivateAndActivateProduct() {
        Product product = new Product("P-TOGGLE", "Sneakers", ProductCategory.CLOTHING, 100.0);
        catalogService.addProduct(product);

        // Деактивация
        catalogService.deactivateProduct("P-TOGGLE");
        Assert.assertFalse(catalogService.getProductById("P-TOGGLE").isActive(), "Товар должен быть неактивен");
        Assert.assertTrue(catalogService.getProductsByCategory(ProductCategory.CLOTHING).isEmpty(),
                "Деактивированный товар не должен выдаваться в категории");

        // Повторная активация
        catalogService.activateProduct("P-TOGGLE");
        Assert.assertTrue(catalogService.getProductById("P-TOGGLE").isActive(), "Товар снова активен");
        Assert.assertEquals(catalogService.getProductsByCategory(ProductCategory.CLOTHING).size(), 1,
                "Активированный товар снова выдается в категории");
    }

    /**
     * Тест фильтрации по диапазону цен [minPrice, maxPrice]:
     * Проверяет отбор активных товаров и сортировку по возрастанию цены.
     */
    @Test(groups = {"catalog"})
    public void testGetProductsInPriceRange() {
        catalogService.addProduct(new Product("P1", "Cheap Case", ProductCategory.ELECTRONICS, 10.0));
        catalogService.addProduct(new Product("P2", "Medium Watch", ProductCategory.ELECTRONICS, 150.0));
        catalogService.addProduct(new Product("P3", "Luxury TV", ProductCategory.ELECTRONICS, 2000.0));

        List<Product> inRange = catalogService.getProductsInPriceRange(50.0, 500.0);
        Assert.assertEquals(inRange.size(), 1, "В диапазон от 50 до 500 попадает 1 товар");
        Assert.assertEquals(inRange.get(0).getName(), "Medium Watch");
    }

    /**
     * Тест массового применения скидки ко всей категории товаров:
     * Проверяет пересчет цен всех активных товаров категории на процент скидки.
     */
    @Test(groups = {"catalog", "finance"})
    public void testApplyCategoryDiscount() {
        catalogService.addProduct(new Product("P1", "Novel", ProductCategory.BOOKS, 100.0));
        catalogService.addProduct(new Product("P2", "Textbook", ProductCategory.BOOKS, 200.0));
        catalogService.addProduct(new Product("P3", "Monitor", ProductCategory.ELECTRONICS, 300.0));

        // Применяем скидку 20% ко всей категории BOOKS
        int affectedCount = catalogService.applyCategoryDiscount(ProductCategory.BOOKS, 20.0);
        Assert.assertEquals(affectedCount, 2, "Скидка должна примениться к 2 книгам");

        Assert.assertEquals(catalogService.getProductById("P1").getPrice(), 80.0, 0.01,
                "Книга за 100 со скидкой 20% стоит 80");
        Assert.assertEquals(catalogService.getProductById("P2").getPrice(), 160.0, 0.01,
                "Книга за 200 со скидкой 20% стоит 160");
        Assert.assertEquals(catalogService.getProductById("P3").getPrice(), 300.0, 0.01,
                "Цена электроники не должна была измениться");
    }
}
