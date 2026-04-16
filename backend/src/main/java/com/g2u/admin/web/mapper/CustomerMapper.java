package com.g2u.admin.web.mapper;

import com.g2u.admin.domain.customer.Customer;
import com.g2u.admin.domain.customer.CustomerAddress;
import com.g2u.admin.web.dto.CustomerAddressDto;
import com.g2u.admin.web.dto.CustomerDto;
import com.g2u.admin.web.dto.CustomerListDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDto toDto(Customer customer);

    @Mapping(target = "isDefault", source = "default")
    CustomerAddressDto toAddressDto(CustomerAddress address);

    CustomerListDto toListDto(Customer customer);
}
