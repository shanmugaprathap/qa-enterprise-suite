package com.enterprise.qa.core.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Fluent test data builder pattern for creating test objects.
 * Supports randomization, templates, and factory patterns.
 */
public class TestDataBuilder {

    private static final Random random = ThreadLocalRandom.current();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== User Builder ====================

    @Data
    public static class User {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String username;
        private String password;
        private LocalDate dateOfBirth;
        private Address address;
        private String role;
        private boolean active;
        private LocalDateTime createdAt;
    }

    public static class UserBuilder {
        private final User user = new User();

        public UserBuilder() {
            // Defaults
            user.setId(UUID.randomUUID().toString());
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
        }

        public UserBuilder withId(String id) { user.setId(id); return this; }
        public UserBuilder withFirstName(String firstName) { user.setFirstName(firstName); return this; }
        public UserBuilder withLastName(String lastName) { user.setLastName(lastName); return this; }
        public UserBuilder withEmail(String email) { user.setEmail(email); return this; }
        public UserBuilder withPhone(String phone) { user.setPhone(phone); return this; }
        public UserBuilder withUsername(String username) { user.setUsername(username); return this; }
        public UserBuilder withPassword(String password) { user.setPassword(password); return this; }
        public UserBuilder withDateOfBirth(LocalDate dob) { user.setDateOfBirth(dob); return this; }
        public UserBuilder withAddress(Address address) { user.setAddress(address); return this; }
        public UserBuilder withRole(String role) { user.setRole(role); return this; }
        public UserBuilder active(boolean active) { user.setActive(active); return this; }

        public UserBuilder withRandomData() {
            user.setFirstName(randomFirstName());
            user.setLastName(randomLastName());
            user.setEmail(randomEmail());
            user.setPhone(randomPhone());
            user.setUsername(randomUsername());
            user.setPassword(randomPassword());
            user.setDateOfBirth(randomDateOfBirth());
            user.setAddress(address().withRandomData().build());
            return this;
        }

        public UserBuilder asAdmin() {
            user.setRole("ADMIN");
            return this;
        }

        public UserBuilder asCustomer() {
            user.setRole("CUSTOMER");
            return this;
        }

        public User build() { return user; }

        public String buildAsJson() {
            try {
                return objectMapper.writeValueAsString(user);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize user", e);
            }
        }

        public Map<String, Object> buildAsMap() {
            return objectMapper.convertValue(user, Map.class);
        }
    }

    public static UserBuilder user() { return new UserBuilder(); }

    // ==================== Address Builder ====================

    @Data
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    public static class AddressBuilder {
        private final Address address = new Address();

        public AddressBuilder withStreet(String street) { address.setStreet(street); return this; }
        public AddressBuilder withCity(String city) { address.setCity(city); return this; }
        public AddressBuilder withState(String state) { address.setState(state); return this; }
        public AddressBuilder withZipCode(String zipCode) { address.setZipCode(zipCode); return this; }
        public AddressBuilder withCountry(String country) { address.setCountry(country); return this; }

        public AddressBuilder withRandomData() {
            address.setStreet(randomStreet());
            address.setCity(randomCity());
            address.setState(randomState());
            address.setZipCode(randomZipCode());
            address.setCountry("USA");
            return this;
        }

        public Address build() { return address; }
    }

    public static AddressBuilder address() { return new AddressBuilder(); }

    // ==================== Payment Builder ====================

    @Data
    public static class Payment {
        private String cardNumber;
        private String cardholderName;
        private String expiryMonth;
        private String expiryYear;
        private String cvv;
        private String cardType;
    }

    public static class PaymentBuilder {
        private final Payment payment = new Payment();

        public PaymentBuilder withCardNumber(String cardNumber) { payment.setCardNumber(cardNumber); return this; }
        public PaymentBuilder withCardholderName(String name) { payment.setCardholderName(name); return this; }
        public PaymentBuilder withExpiry(String month, String year) {
            payment.setExpiryMonth(month);
            payment.setExpiryYear(year);
            return this;
        }
        public PaymentBuilder withCvv(String cvv) { payment.setCvv(cvv); return this; }
        public PaymentBuilder withCardType(String type) { payment.setCardType(type); return this; }

        public PaymentBuilder asVisa() {
            payment.setCardNumber("4111111111111111");
            payment.setCardType("VISA");
            return this;
        }

        public PaymentBuilder asMastercard() {
            payment.setCardNumber("5500000000000004");
            payment.setCardType("MASTERCARD");
            return this;
        }

        public PaymentBuilder asAmex() {
            payment.setCardNumber("340000000000009");
            payment.setCardType("AMEX");
            return this;
        }

        public PaymentBuilder withValidExpiry() {
            LocalDate future = LocalDate.now().plusYears(2);
            payment.setExpiryMonth(String.format("%02d", future.getMonthValue()));
            payment.setExpiryYear(String.valueOf(future.getYear()));
            return this;
        }

        public PaymentBuilder withExpiredCard() {
            LocalDate past = LocalDate.now().minusMonths(1);
            payment.setExpiryMonth(String.format("%02d", past.getMonthValue()));
            payment.setExpiryYear(String.valueOf(past.getYear()));
            return this;
        }

        public PaymentBuilder withRandomData() {
            asVisa();
            payment.setCardholderName(randomFirstName() + " " + randomLastName());
            payment.setCvv(String.valueOf(100 + random.nextInt(900)));
            withValidExpiry();
            return this;
        }

        public Payment build() { return payment; }
    }

    public static PaymentBuilder payment() { return new PaymentBuilder(); }

    // ==================== Product Builder ====================

    @Data
    public static class Product {
        private String id;
        private String name;
        private String description;
        private String sku;
        private double price;
        private int quantity;
        private String category;
        private boolean inStock;
    }

    public static class ProductBuilder {
        private final Product product = new Product();

        public ProductBuilder() {
            product.setId(UUID.randomUUID().toString());
            product.setInStock(true);
            product.setQuantity(100);
        }

        public ProductBuilder withId(String id) { product.setId(id); return this; }
        public ProductBuilder withName(String name) { product.setName(name); return this; }
        public ProductBuilder withDescription(String desc) { product.setDescription(desc); return this; }
        public ProductBuilder withSku(String sku) { product.setSku(sku); return this; }
        public ProductBuilder withPrice(double price) { product.setPrice(price); return this; }
        public ProductBuilder withQuantity(int qty) { product.setQuantity(qty); return this; }
        public ProductBuilder withCategory(String category) { product.setCategory(category); return this; }
        public ProductBuilder inStock(boolean inStock) { product.setInStock(inStock); return this; }

        public ProductBuilder outOfStock() {
            product.setInStock(false);
            product.setQuantity(0);
            return this;
        }

        public ProductBuilder withRandomData() {
            product.setName("Product " + random.nextInt(10000));
            product.setDescription("Description for test product");
            product.setSku("SKU-" + random.nextInt(100000));
            product.setPrice(9.99 + random.nextDouble() * 990);
            product.setCategory(randomCategory());
            return this;
        }

        public Product build() { return product; }
    }

    public static ProductBuilder product() { return new ProductBuilder(); }

    // ==================== Order Builder ====================

    @Data
    public static class Order {
        private String id;
        private String userId;
        private List<OrderItem> items;
        private double subtotal;
        private double tax;
        private double total;
        private String status;
        private Address shippingAddress;
        private Payment paymentMethod;
        private LocalDateTime createdAt;
    }

    @Data
    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
    }

    public static class OrderBuilder {
        private final Order order = new Order();
        private final List<OrderItem> items = new ArrayList<>();

        public OrderBuilder() {
            order.setId(UUID.randomUUID().toString());
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
        }

        public OrderBuilder withUserId(String userId) { order.setUserId(userId); return this; }
        public OrderBuilder withStatus(String status) { order.setStatus(status); return this; }
        public OrderBuilder withShippingAddress(Address address) { order.setShippingAddress(address); return this; }
        public OrderBuilder withPayment(Payment payment) { order.setPaymentMethod(payment); return this; }

        public OrderBuilder addItem(String productId, String name, int quantity, double price) {
            OrderItem item = new OrderItem();
            item.setProductId(productId);
            item.setProductName(name);
            item.setQuantity(quantity);
            item.setUnitPrice(price);
            item.setTotalPrice(quantity * price);
            items.add(item);
            return this;
        }

        public OrderBuilder addRandomItems(int count) {
            for (int i = 0; i < count; i++) {
                addItem(
                        UUID.randomUUID().toString(),
                        "Product " + (i + 1),
                        1 + random.nextInt(5),
                        9.99 + random.nextDouble() * 100
                );
            }
            return this;
        }

        public Order build() {
            order.setItems(items);
            double subtotal = items.stream().mapToDouble(OrderItem::getTotalPrice).sum();
            order.setSubtotal(subtotal);
            order.setTax(subtotal * 0.08); // 8% tax
            order.setTotal(subtotal + order.getTax());
            return order;
        }
    }

    public static OrderBuilder order() { return new OrderBuilder(); }

    // ==================== Factory Methods ====================

    /**
     * Creates a list of random users.
     */
    public static List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(user().withRandomData().build());
        }
        return users;
    }

    /**
     * Creates a list of random products.
     */
    public static List<Product> createProducts(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(product().withRandomData().build());
        }
        return products;
    }

    /**
     * Creates test data from a supplier.
     */
    public static <T> List<T> createList(int count, Supplier<T> supplier) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(supplier.get());
        }
        return list;
    }

    // ==================== Random Data Generators ====================

    private static final String[] FIRST_NAMES = {
            "James", "John", "Robert", "Michael", "William", "David", "Richard",
            "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
            "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson"
    };

    private static final String[] CITIES = {
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
            "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville"
    };

    private static final String[] STATES = {
            "CA", "TX", "FL", "NY", "PA", "IL", "OH", "GA", "NC", "MI", "NJ", "VA"
    };

    private static final String[] CATEGORIES = {
            "Electronics", "Clothing", "Home & Garden", "Sports", "Books",
            "Toys", "Automotive", "Health & Beauty", "Food & Beverages"
    };

    public static String randomFirstName() { return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]; }
    public static String randomLastName() { return LAST_NAMES[random.nextInt(LAST_NAMES.length)]; }
    public static String randomCity() { return CITIES[random.nextInt(CITIES.length)]; }
    public static String randomState() { return STATES[random.nextInt(STATES.length)]; }
    public static String randomCategory() { return CATEGORIES[random.nextInt(CATEGORIES.length)]; }

    public static String randomEmail() {
        return randomFirstName().toLowerCase() + "." +
               randomLastName().toLowerCase() + random.nextInt(1000) + "@example.com";
    }

    public static String randomPhone() {
        return String.format("+1-%03d-%03d-%04d",
                200 + random.nextInt(800),
                200 + random.nextInt(800),
                random.nextInt(10000));
    }

    public static String randomUsername() {
        return randomFirstName().toLowerCase() + random.nextInt(10000);
    }

    public static String randomPassword() {
        return "Test" + random.nextInt(10000) + "!";
    }

    public static String randomStreet() {
        return (100 + random.nextInt(9900)) + " " + randomLastName() + " Street";
    }

    public static String randomZipCode() {
        return String.format("%05d", 10000 + random.nextInt(90000));
    }

    public static LocalDate randomDateOfBirth() {
        return LocalDate.now().minusYears(18 + random.nextInt(60));
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static int randomInt(int min, int max) {
        return min + random.nextInt(max - min);
    }

    public static double randomDouble(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public static <T> T randomFrom(T[] array) {
        return array[random.nextInt(array.length)];
    }

    public static <T> T randomFrom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
