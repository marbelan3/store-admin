package com.g2u.admin.web.mapper;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductMedia;
import com.g2u.admin.domain.product.ProductVariant;
import com.g2u.admin.domain.tag.Tag;
import com.g2u.admin.web.dto.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "status", expression = "java(product.getStatus().name())")
    ProductDto toDto(Product product);

    ProductDto.VariantDto toVariantDto(ProductVariant variant);

    @Mapping(target = "primary", source = "primary")
    ProductDto.MediaDto toMediaDto(ProductMedia media);

    ProductDto.CategoryRefDto toCategoryRefDto(Category category);

    ProductDto.TagRefDto toTagRefDto(Tag tag);
}
