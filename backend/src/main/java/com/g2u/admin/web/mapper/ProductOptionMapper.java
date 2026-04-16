package com.g2u.admin.web.mapper;

import com.g2u.admin.domain.product.ProductOption;
import com.g2u.admin.domain.product.ProductOptionValue;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.web.dto.ProductOptionDto;
import com.g2u.admin.web.dto.ProductVariantDetailDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductOptionMapper {

    @Mapping(target = "values", source = "values")
    ProductOptionDto toOptionDto(ProductOption option);

    ProductOptionDto.OptionValueDto toOptionValueDto(ProductOptionValue value);

    List<ProductOptionDto> toOptionDtoList(List<ProductOption> options);

    default ProductVariantDetailDto toVariantDetailDto(ProductVariant variant) {
        if (variant == null) return null;

        List<ProductOptionDto.OptionValueDto> optionValueDtos = variant.getOptionValues() != null
                ? variant.getOptionValues().stream()
                    .map(this::toOptionValueDto)
                    .collect(Collectors.toList())
                : List.of();

        return new ProductVariantDetailDto(
                variant.getId(),
                variant.getName(),
                variant.getSku(),
                variant.getPrice(),
                variant.getCompareAtPrice(),
                variant.getCostPrice(),
                variant.getQuantity(),
                variant.getLowStockThreshold(),
                variant.getSortOrder(),
                variant.isActive(),
                optionValueDtos,
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }

    default List<ProductVariantDetailDto> toVariantDetailDtoList(List<ProductVariant> variants) {
        if (variants == null) return List.of();
        return variants.stream().map(this::toVariantDetailDto).collect(Collectors.toList());
    }
}
