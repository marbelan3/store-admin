package com.g2u.admin.service;

import com.g2u.admin.domain.customer.CustomerRepository;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CreateCustomerAddressRequest;
import com.g2u.admin.web.dto.CreateCustomerRequest;
import com.g2u.admin.web.dto.CustomerAddressDto;
import com.g2u.admin.web.dto.CustomerDto;
import com.g2u.admin.web.dto.CustomerListDto;
import com.g2u.admin.web.dto.UpdateCustomerRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Customer Tenant A")
                .slug("cust-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Customer Tenant B")
                .slug("cust-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createCustomer_shouldPersist() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+1234567890", "VIP customer");

        CustomerDto customer = customerService.createCustomer(tenantAId, TEST_USER_ID, request);

        assertNotNull(customer.id());
        assertEquals("John", customer.firstName());
        assertEquals("Doe", customer.lastName());
        assertEquals("john@example.com", customer.email());
        assertEquals("+1234567890", customer.phone());
        assertEquals("VIP customer", customer.notes());
        assertEquals(0, customer.totalOrders());
        assertEquals(BigDecimal.ZERO, customer.totalSpent());
        assertNotNull(customer.createdAt());
        assertNotNull(customer.updatedAt());
    }

    @Test
    void getCustomers_searchByName() {
        customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Alice", "Smith", null, null, null));
        customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Bob", "Johnson", null, null, null));
        customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Charlie", "Smith", null, null, null));

        Page<CustomerListDto> results = customerService.getCustomers(tenantAId, "Smith", PageRequest.of(0, 20));

        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().allMatch(c -> c.lastName().equals("Smith")));
    }

    @Test
    void getCustomers_searchByEmail() {
        customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Test", "User", "unique-email@test.com", null, null));
        customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Other", "User", "other@test.com", null, null));

        Page<CustomerListDto> results = customerService.getCustomers(tenantAId, "unique-email", PageRequest.of(0, 20));

        assertEquals(1, results.getTotalElements());
        assertEquals("unique-email@test.com", results.getContent().get(0).email());
    }

    @Test
    void updateCustomer_shouldUpdateFields() {
        CustomerDto created = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Original", "Name", null, null, null));

        UpdateCustomerRequest update = new UpdateCustomerRequest(
                "Updated", "LastName", "new@email.com", "+9876543210", "Updated notes");

        CustomerDto updated = customerService.updateCustomer(created.id(), tenantAId, TEST_USER_ID, update);

        assertEquals("Updated", updated.firstName());
        assertEquals("LastName", updated.lastName());
        assertEquals("new@email.com", updated.email());
        assertEquals("+9876543210", updated.phone());
        assertEquals("Updated notes", updated.notes());
    }

    @Test
    void deleteCustomer_shouldRemove() {
        CustomerDto created = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("ToDelete", "User", null, null, null));

        customerService.deleteCustomer(created.id(), tenantAId, TEST_USER_ID);

        assertThrows(ResourceNotFoundException.class, () ->
                customerService.getCustomer(created.id(), tenantAId));
    }

    @Test
    void addAddress_shouldPersist() {
        CustomerDto customer = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Addr", "Test", null, null, null));

        CreateCustomerAddressRequest addressReq = new CreateCustomerAddressRequest(
                "SHIPPING", "123 Main St", "Apt 4", "Kyiv", "Kyiv Oblast",
                "01001", "Ukraine", true);

        CustomerAddressDto address = customerService.addAddress(customer.id(), tenantAId, addressReq);

        assertNotNull(address.id());
        assertEquals("SHIPPING", address.type());
        assertEquals("123 Main St", address.line1());
        assertEquals("Apt 4", address.line2());
        assertEquals("Kyiv", address.city());
        assertEquals("Kyiv Oblast", address.state());
        assertEquals("01001", address.postalCode());
        assertEquals("Ukraine", address.country());
        assertTrue(address.isDefault());
    }

    @Test
    void tenantIsolation() {
        CustomerDto customerA = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("TenantA", "Customer", null, null, null));

        // Tenant B should not see Tenant A's customer
        assertThrows(ResourceNotFoundException.class, () ->
                customerService.getCustomer(customerA.id(), tenantBId));

        // Tenant B's list should not include Tenant A's customers
        Page<CustomerListDto> tenantBCustomers = customerService.getCustomers(tenantBId, null, PageRequest.of(0, 20));
        assertEquals(0, tenantBCustomers.getTotalElements());
    }

    @Test
    void incrementOrderStats_shouldUpdateCountAndSpent() {
        CustomerDto customer = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Stats", "Test", null, null, null));

        customerService.incrementOrderStats(customer.id(), tenantAId, new BigDecimal("150.00"));
        customerService.incrementOrderStats(customer.id(), tenantAId, new BigDecimal("75.50"));

        CustomerDto updated = customerService.getCustomer(customer.id(), tenantAId);
        assertEquals(2, updated.totalOrders());
        assertEquals(new BigDecimal("225.50"), updated.totalSpent());
    }

    @Test
    void addMultipleAddresses_defaultShouldBeUnique() {
        CustomerDto customer = customerService.createCustomer(tenantAId, TEST_USER_ID,
                new CreateCustomerRequest("Multi", "Addr", null, null, null));

        CustomerAddressDto addr1 = customerService.addAddress(customer.id(), tenantAId,
                new CreateCustomerAddressRequest("SHIPPING", "Address 1", null, "City1", null, null, "Country1", true));
        assertTrue(addr1.isDefault());

        // Adding second default address should unset the first
        CustomerAddressDto addr2 = customerService.addAddress(customer.id(), tenantAId,
                new CreateCustomerAddressRequest("BILLING", "Address 2", null, "City2", null, null, "Country2", true));
        assertTrue(addr2.isDefault());

        // Verify customer now has both addresses, and only the second is default
        CustomerDto fetched = customerService.getCustomer(customer.id(), tenantAId);
        assertEquals(2, fetched.addresses().size());
        long defaultCount = fetched.addresses().stream().filter(CustomerAddressDto::isDefault).count();
        assertEquals(1, defaultCount);
    }
}
