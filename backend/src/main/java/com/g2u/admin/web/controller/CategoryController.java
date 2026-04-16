package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.CategoryService;
import com.g2u.admin.web.dto.CategoryDto;
import com.g2u.admin.web.dto.CreateCategoryRequest;
import com.g2u.admin.web.dto.ReorderCategoriesRequest;
import com.g2u.admin.web.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> listCategories(
            @CurrentUser UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean flat) {
        if (flat) {
            return categoryService.getAllCategories(principal.tenantId());
        }
        return categoryService.getCategoryTree(principal.tenantId());
    }

    @GetMapping("/{id}")
    public CategoryDto getCategory(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        return categoryService.getCategory(principal.tenantId(), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<CategoryDto> createCategory(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto category = categoryService.createCategory(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> reorderCategories(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody ReorderCategoriesRequest request) {
        categoryService.reorderCategories(principal.tenantId(), principal.userId(), request.categoryIds());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public CategoryDto updateCategory(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.updateCategory(principal.tenantId(), principal.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        categoryService.deleteCategory(principal.tenantId(), principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
