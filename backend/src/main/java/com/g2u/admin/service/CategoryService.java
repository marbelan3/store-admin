package com.g2u.admin.service;

import com.g2u.admin.domain.category.Category;
import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.util.SlugUtils;
import com.g2u.admin.web.dto.CategoryDto;
import com.g2u.admin.web.dto.CreateCategoryRequest;
import com.g2u.admin.web.dto.UpdateCategoryRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import com.g2u.admin.web.mapper.CategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final AuditService auditService;
    private final WebhookService webhookService;

    public CategoryService(CategoryRepository categoryRepository,
                           CategoryMapper categoryMapper,
                           AuditService auditService,
                           WebhookService webhookService) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.auditService = auditService;
        this.webhookService = webhookService;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryTree(UUID tenantId) {
        List<Category> roots = categoryRepository.findByTenantIdAndParentIsNullOrderBySortOrderAsc(tenantId);
        return categoryMapper.toDtoList(roots);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(UUID tenantId) {
        List<Category> categories = categoryRepository.findByTenantIdOrderBySortOrderAsc(tenantId);
        return categoryMapper.toDtoList(categories);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(UUID tenantId, UUID categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        return categoryMapper.toDto(category);
    }

    public CategoryDto createCategory(UUID tenantId, UUID userId, CreateCategoryRequest request) {
        String baseSlug = request.slug() != null ? request.slug() : SlugUtils.generateSlug(request.name());
        String slug = SlugUtils.ensureUnique(baseSlug,
                s -> categoryRepository.existsByTenantIdAndSlug(tenantId, s));

        Category category = Category.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .active(true)
                .metaTitle(request.metaTitle())
                .metaDescription(request.metaDescription())
                .build();
        category.setTenantId(tenantId);

        if (request.parentId() != null) {
            Category parent = categoryRepository.findByTenantIdAndId(tenantId, request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.parentId()));
            category.setParent(parent);
            category.setPath(parent.getPath() != null ? parent.getPath() + "/" + slug : slug);
        } else {
            category.setPath(slug);
        }

        category = categoryRepository.save(category);
        log.info("Created category '{}' (id={}) for tenant {}", category.getName(), category.getId(), tenantId);

        auditService.log(tenantId, userId, "CREATE", "CATEGORY", category.getId(),
                Map.of("name", category.getName(), "slug", category.getSlug()));

        return categoryMapper.toDto(category);
    }

    public CategoryDto updateCategory(UUID tenantId, UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        if (request.name() != null) category.setName(request.name());
        if (request.slug() != null) category.setSlug(request.slug());
        if (request.description() != null) category.setDescription(request.description());
        if (request.imageUrl() != null) category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());
        if (request.active() != null) category.setActive(request.active());
        if (request.metaTitle() != null) category.setMetaTitle(request.metaTitle());
        if (request.metaDescription() != null) category.setMetaDescription(request.metaDescription());

        if (request.parentId() != null) {
            Category parent = categoryRepository.findByTenantIdAndId(tenantId, request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.parentId()));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Updated category '{}' (id={}) for tenant {}", category.getName(), categoryId, tenantId);

        auditService.log(tenantId, userId, "UPDATE", "CATEGORY", category.getId(),
                Map.of("name", category.getName()));

        webhookService.dispatch(tenantId, "category.updated",
                Map.of("id", category.getId(), "name", category.getName()));

        return categoryMapper.toDto(category);
    }

    public void reorderCategories(UUID tenantId, UUID userId, List<UUID> categoryIds) {
        List<Category> categories = categoryRepository.findByTenantIdAndIdIn(tenantId, categoryIds);

        for (int i = 0; i < categoryIds.size(); i++) {
            UUID cid = categoryIds.get(i);
            for (Category cat : categories) {
                if (cid.equals(cat.getId())) {
                    cat.setSortOrder(i);
                    break;
                }
            }
        }

        categoryRepository.saveAll(categories);
        log.info("Reordered {} categories for tenant {}", categoryIds.size(), tenantId);

        auditService.log(tenantId, userId, "REORDER", "CATEGORY", null,
                Map.of("count", categoryIds.size()));
    }

    public void deleteCategory(UUID tenantId, UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        categoryRepository.delete(category);
        log.info("Deleted category '{}' (id={}) from tenant {}", category.getName(), categoryId, tenantId);

        auditService.log(tenantId, userId, "DELETE", "CATEGORY", categoryId,
                Map.of("name", category.getName()));

        webhookService.dispatch(tenantId, "category.deleted",
                Map.of("id", categoryId, "name", category.getName()));
    }
}
