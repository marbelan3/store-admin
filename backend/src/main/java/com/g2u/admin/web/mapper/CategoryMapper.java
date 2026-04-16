package com.g2u.admin.web.mapper;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.web.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", source = "children")
    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);
}
