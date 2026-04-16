package com.g2u.admin.domain.discount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountProductRepository extends JpaRepository<DiscountProduct, UUID> {

    List<DiscountProduct> findByDiscountId(UUID discountId);

    void deleteByDiscountId(UUID discountId);
}
