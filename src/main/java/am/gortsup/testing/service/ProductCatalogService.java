package am.gortsup.testing.service;

import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.model.ProductCategory;
import am.gortsup.testing.util.MathAndFinanceUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing product catalog, pricing, category classification, and product searches.
 */
public class ProductCatalogService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    /**
     * Adds a new product to the catalog.
     *
     * @param product product entity (must have non-empty name, non-null category, and price >= 0)
     * @return added product
     * @throws ValidationException if product or its fields are invalid, or ID already exists
     */
    public Product addProduct(Product product) {
        if (product == null) {
            throw new ValidationException("Product cannot be null");
        }
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            throw new ValidationException("Product ID cannot be empty");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty");
        }
        if (product.getCategory() == null) {
            throw new ValidationException("Product category cannot be null");
        }
        if (product.getPrice() < 0.0) {
            throw new ValidationException("Product price cannot be negative: " + product.getPrice());
        }
        if (products.containsKey(product.getId())) {
            throw new ValidationException("Product with ID " + product.getId() + " already exists");
        }

        products.put(product.getId(), product);
        return product;
    }

    /**
     * Finds a product by ID.
     *
     * @param id product identifier
     * @return Product
     * @throws ResourceNotFoundException if product not found
     */
    public Product getProductById(String id) {
        if (id == null) {
            throw new ResourceNotFoundException("Product ID cannot be null");
        }
        Product product = products.get(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }
        return product;
    }

    /**
     * Updates the price of an existing product.
     *
     * @param id product ID
     * @param newPrice new positive price
     * @throws ResourceNotFoundException if product not found
     * @throws ValidationException if new price is negative
     */
    public void updateProductPrice(String id, double newPrice) {
        if (newPrice < 0.0) {
            throw new ValidationException("Price cannot be negative: " + newPrice);
        }
        Product product = getProductById(id);
        product.setPrice(MathAndFinanceUtils.roundToDecimalPlaces(newPrice, 2));
    }

    /**
     * Retrieves all active products in a given category.
     *
     * @param category product category
     * @return list of active products in that category
     */
    public List<Product> getProductsByCategory(ProductCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Searches active products by partial name match (case-insensitive).
     *
     * @param query search keyword
     * @return matching products
     */
    public List<Product> searchProductsByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String lowerQuery = query.trim().toLowerCase();
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Deactivates a product, removing it from active listings.
     *
     * @param id product ID
     * @throws ResourceNotFoundException if product not found
     */
    public void deactivateProduct(String id) {
        Product product = getProductById(id);
        product.setActive(false);
    }

    /**
     * Activates a product.
     *
     * @param id product ID
     * @throws ResourceNotFoundException if product not found
     */
    public void activateProduct(String id) {
        Product product = getProductById(id);
        product.setActive(true);
    }

    /**
     * Filters active products within a price range [minPrice, maxPrice].
     *
     * @param minPrice minimum price (must be >= 0)
     * @param maxPrice maximum price (must be >= minPrice)
     * @return filtered products
     * @throws ValidationException if minPrice < 0 or minPrice > maxPrice
     */
    public List<Product> getProductsInPriceRange(double minPrice, double maxPrice) {
        if (minPrice < 0.0 || maxPrice < minPrice) {
            throw new ValidationException("Invalid price range: [" + minPrice + ", " + maxPrice + "]");
        }
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .sorted(Comparator.comparingDouble(Product::getPrice))
                .collect(Collectors.toList());
    }

    /**
     * Applies a discount percentage to all active products in a given category.
     *
     * @param category target category
     * @param discountPercentage discount between 0.0 and 100.0
     * @return count of updated products
     * @throws ValidationException if discount percentage is invalid or category is null
     */
    public int applyCategoryDiscount(ProductCategory category, double discountPercentage) {
        if (category == null) {
            throw new ValidationException("Category cannot be null");
        }
        if (discountPercentage < 0.0 || discountPercentage > 100.0) {
            throw new ValidationException("Discount percentage must be between 0.0 and 100.0");
        }

        List<Product> categoryProducts = getProductsByCategory(category);
        for (Product product : categoryProducts) {
            double newPrice = MathAndFinanceUtils.calculateDiscount(product.getPrice(), discountPercentage);
            product.setPrice(newPrice);
        }
        return categoryProducts.size();
    }

    /**
     * Returns all active products in the catalog.
     *
     * @return list of active products
     */
    public List<Product> getAllActiveProducts() {
        return products.values().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());
    }
}
