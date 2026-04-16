package com.g2u.admin.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, UUID> {

    List<ProductOptionValue> findByIdIn(Collection<UUID> ids);
}
