package com.g2u.admin.service;

import com.g2u.admin.domain.customer.Customer;
import com.g2u.admin.domain.customer.CustomerAddress;
import com.g2u.admin.domain.customer.CustomerAddressRepository;
import com.g2u.admin.domain.customer.CustomerRepository;
import com.g2u.admin.web.dto.CreateCustomerAddressRequest;
import com.g2u.admin.web.dto.CreateCustomerRequest;
import com.g2u.admin.web.dto.CustomerAddressDto;
import com.g2u.admin.web.dto.CustomerDto;
import com.g2u.admin.web.dto.CustomerListDto;
import com.g2u.admin.web.dto.UpdateCustomerRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import com.g2u.admin.web.mapper.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerMapper customerMapper;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerAddressRepository customerAddressRepository,
                           CustomerMapper customerMapper,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerMapper = customerMapper;
        this.auditService = auditService;
    }

    public CustomerDto createCustomer(UUID tenantId, UUID userId, CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .notes(request.notes())
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .addresses(new ArrayList<>())
                .build();
        customer.setTenantId(tenantId);

        customer = customerRepository.save(customer);
        log.info("Created customer '{}' (id={}) for tenant {}", customer.getFirstName(), customer.getId(), tenantId);

        auditService.log(tenantId, userId, "CREATE", "CUSTOMER", customer.getId(),
                Map.of("firstName", customer.getFirstName(), "lastName", customer.getLastName()));

        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(UUID id, UUID tenantId) {
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerListDto> getCustomers(UUID tenantId, String search, Pageable pageable) {
        Page<Customer> page;
        if (search != null && !search.isBlank()) {
            page = customerRepository.search(tenantId, search.trim(), pageable);
        } else {
            page = customerRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(customerMapper::toListDto);
    }

    public CustomerDto updateCustomer(UUID id, UUID tenantId, UUID userId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));

        if (request.firstName() != null) customer.setFirstName(request.firstName());
        if (request.lastName() != null) customer.setLastName(request.lastName());
        if (request.email() != null) customer.setEmail(request.email());
        if (request.phone() != null) customer.setPhone(request.phone());
        if (request.notes() != null) customer.setNotes(request.notes());

        customer = customerRepository.save(customer);
        log.info("Updated customer (id={}) for tenant {}", id, tenantId);

        auditService.log(tenantId, userId, "UPDATE", "CUSTOMER", customer.getId(),
                Map.of("firstName", customer.getFirstName(), "lastName", customer.getLastName()));

        return customerMapper.toDto(customer);
    }

    public void deleteCustomer(UUID id, UUID tenantId, UUID userId) {
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));

        customerRepository.delete(customer);
        log.info("Deleted customer (id={}) from tenant {}", id, tenantId);

        auditService.log(tenantId, userId, "DELETE", "CUSTOMER", id,
                Map.of("firstName", customer.getFirstName(), "lastName", customer.getLastName()));
    }

    public CustomerAddressDto addAddress(UUID customerId, UUID tenantId, CreateCustomerAddressRequest request) {
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        // If the new address is default, unset existing defaults
        if (request.isDefault()) {
            customerAddressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        customerAddressRepository.save(existing);
                    });
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .type(request.type())
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(request.isDefault())
                .build();

        address = customerAddressRepository.save(address);
        log.info("Added address (id={}) to customer {} for tenant {}", address.getId(), customerId, tenantId);

        return customerMapper.toAddressDto(address);
    }

    public CustomerAddressDto updateAddress(UUID customerId, UUID addressId, UUID tenantId,
                                            CreateCustomerAddressRequest request) {
        // Verify customer belongs to tenant
        customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        CustomerAddress address = customerAddressRepository.findById(addressId)
                .filter(a -> a.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("CustomerAddress", addressId));

        // If setting as default, unset existing defaults
        if (request.isDefault() && !address.isDefault()) {
            customerAddressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        customerAddressRepository.save(existing);
                    });
        }

        address.setType(request.type());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setDefault(request.isDefault());

        address = customerAddressRepository.save(address);
        log.info("Updated address (id={}) for customer {} tenant {}", addressId, customerId, tenantId);

        return customerMapper.toAddressDto(address);
    }

    public void deleteAddress(UUID customerId, UUID addressId, UUID tenantId) {
        customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        CustomerAddress address = customerAddressRepository.findById(addressId)
                .filter(a -> a.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("CustomerAddress", addressId));

        customerAddressRepository.delete(address);
        log.info("Deleted address (id={}) from customer {} tenant {}", addressId, customerId, tenantId);
    }

    public void incrementOrderStats(UUID customerId, UUID tenantId, BigDecimal amount) {
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        customer.setTotalOrders(customer.getTotalOrders() + 1);
        customer.setTotalSpent(customer.getTotalSpent().add(amount));
        customerRepository.save(customer);
        log.debug("Incremented order stats for customer {} (orders={}, spent={})",
                customerId, customer.getTotalOrders(), customer.getTotalSpent());
    }
}
