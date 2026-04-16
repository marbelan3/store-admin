package com.g2u.admin.service;

import com.g2u.admin.domain.category.CategoryRepository;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.CategoryDto;
import com.g2u.admin.web.dto.CreateCategoryRequest;
import com.g2u.admin.web.dto.UpdateCategoryRequest;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Tenant A")
                .slug("tenant-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Tenant B")
                .slug("tenant-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void createCategory_shouldSucceed() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Electronics", null, "Electronic devices", null,
                0, null, "Electronics Meta", "Buy electronics"
        );

        CategoryDto category = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        assertNotNull(category.id());
        assertEquals("Electronics", category.name());
        assertNotNull(category.slug());
        assertFalse(category.slug().isEmpty());
        assertTrue(category.active());
        assertEquals(0, category.sortOrder());
        assertEquals("Electronics Meta", category.metaTitle());
        assertEquals("Buy electronics", category.metaDescription());
    }

    @Test
    void createCategory_shouldAutoGenerateSlugFromName() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Home & Garden", null, null, null,
                null, null, null, null
        );

        CategoryDto category = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        assertNotNull(category.slug());
        assertFalse(category.slug().isEmpty());
        // Slug should be a URL-friendly version of the name
        assertFalse(category.slug().contains(" "));
        assertFalse(category.slug().contains("&"));
    }

    @Test
    void createCategory_withExplicitSlug_shouldUseProvidedSlug() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Electronics", "custom-electronics-slug", null, null,
                null, null, null, null
        );

        CategoryDto category = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        assertEquals("custom-electronics-slug", category.slug());
    }

    @Test
    void createSubcategory_shouldSetParent() {
        CreateCategoryRequest parentRequest = new CreateCategoryRequest(
                "Electronics", null, null, null,
                null, null, null, null
        );
        CategoryDto parent = categoryService.createCategory(tenantAId, TEST_USER_ID, parentRequest);

        CreateCategoryRequest childRequest = new CreateCategoryRequest(
                "Laptops", null, "Laptop computers", null,
                null, parent.id(), null, null
        );
        CategoryDto child = categoryService.createCategory(tenantAId, TEST_USER_ID, childRequest);

        assertNotNull(child.id());
        assertEquals("Laptops", child.name());
        assertEquals(parent.id(), child.parentId());
        assertNotNull(child.path());
        assertTrue(child.path().contains(child.slug()));
    }

    @Test
    void getCategoryTree_shouldReturnTreeStructure() {
        // Create parent
        CreateCategoryRequest parentRequest = new CreateCategoryRequest(
                "Electronics", null, null, null,
                0, null, null, null
        );
        CategoryDto parent = categoryService.createCategory(tenantAId, TEST_USER_ID, parentRequest);

        // Create children
        CreateCategoryRequest child1Request = new CreateCategoryRequest(
                "Laptops", null, null, null,
                0, parent.id(), null, null
        );
        categoryService.createCategory(tenantAId, TEST_USER_ID, child1Request);

        CreateCategoryRequest child2Request = new CreateCategoryRequest(
                "Phones", null, null, null,
                1, parent.id(), null, null
        );
        categoryService.createCategory(tenantAId, TEST_USER_ID, child2Request);

        // Create another root
        CreateCategoryRequest otherRootRequest = new CreateCategoryRequest(
                "Clothing", null, null, null,
                1, null, null, null
        );
        categoryService.createCategory(tenantAId, TEST_USER_ID, otherRootRequest);

        List<CategoryDto> tree = categoryService.getCategoryTree(tenantAId);

        // Should have 2 root categories
        assertTrue(tree.size() >= 2);

        // Find the Electronics root
        CategoryDto electronicsRoot = tree.stream()
                .filter(c -> c.name().equals("Electronics"))
                .findFirst()
                .orElseThrow();

        // Electronics should have 2 children
        assertNotNull(electronicsRoot.children());
        assertEquals(2, electronicsRoot.children().size());

        // Verify children names
        List<String> childNames = electronicsRoot.children().stream()
                .map(CategoryDto::name)
                .toList();
        assertTrue(childNames.contains("Laptops"));
        assertTrue(childNames.contains("Phones"));
    }

    @Test
    void updateCategory_shouldUpdateNameAndSlug() {
        CreateCategoryRequest createRequest = new CreateCategoryRequest(
                "Old Name", null, null, null,
                null, null, null, null
        );
        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, createRequest);

        UpdateCategoryRequest updateRequest = new UpdateCategoryRequest(
                "New Name", "new-slug", null, null,
                null, null, null, null, null
        );

        CategoryDto updated = categoryService.updateCategory(tenantAId, TEST_USER_ID, created.id(), updateRequest);

        assertEquals("New Name", updated.name());
        assertEquals("new-slug", updated.slug());
    }

    @Test
    void updateCategory_shouldUpdateSeoFields() {
        CreateCategoryRequest createRequest = new CreateCategoryRequest(
                "SEO Category", null, null, null,
                null, null, null, null
        );
        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, createRequest);

        UpdateCategoryRequest updateRequest = new UpdateCategoryRequest(
                null, null, null, null,
                null, null, null, "Updated Meta Title", "Updated meta description for SEO"
        );

        CategoryDto updated = categoryService.updateCategory(tenantAId, TEST_USER_ID, created.id(), updateRequest);

        assertEquals("SEO Category", updated.name()); // name unchanged
        assertEquals("Updated Meta Title", updated.metaTitle());
        assertEquals("Updated meta description for SEO", updated.metaDescription());
    }

    @Test
    void updateCategory_shouldUpdateActiveFlag() {
        CreateCategoryRequest createRequest = new CreateCategoryRequest(
                "Active Category", null, null, null,
                null, null, null, null
        );
        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, createRequest);
        assertTrue(created.active());

        UpdateCategoryRequest deactivate = new UpdateCategoryRequest(
                null, null, null, null,
                null, false, null, null, null
        );

        CategoryDto updated = categoryService.updateCategory(tenantAId, TEST_USER_ID, created.id(), deactivate);

        assertFalse(updated.active());
    }

    @Test
    void deleteCategory_shouldRemoveCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "To Delete", null, null, null,
                null, null, null, null
        );
        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        categoryService.deleteCategory(tenantAId, TEST_USER_ID, created.id());

        assertThrows(ResourceNotFoundException.class, () ->
                categoryService.getCategory(tenantAId, created.id())
        );
    }

    @Test
    void tenantIsolation_categoryNotAccessibleByCrossTenant() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Tenant A Category", null, null, null,
                null, null, null, null
        );

        CategoryDto category = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        assertThrows(ResourceNotFoundException.class, () ->
                categoryService.getCategory(tenantBId, category.id())
        );
    }

    @Test
    void tenantIsolation_treeOnlyShowsOwnCategories() {
        categoryService.createCategory(tenantAId, TEST_USER_ID, new CreateCategoryRequest(
                "Category A1", null, null, null, null, null, null, null
        ));
        categoryService.createCategory(tenantAId, TEST_USER_ID, new CreateCategoryRequest(
                "Category A2", null, null, null, null, null, null, null
        ));
        categoryService.createCategory(tenantBId, TEST_USER_ID, new CreateCategoryRequest(
                "Category B1", null, null, null, null, null, null, null
        ));

        List<CategoryDto> tenantATree = categoryService.getCategoryTree(tenantAId);
        List<CategoryDto> tenantBTree = categoryService.getCategoryTree(tenantBId);

        long tenantACount = tenantATree.size();
        long tenantBCount = tenantBTree.size();

        // Tenant A should have at least 2, Tenant B should have at least 1
        assertTrue(tenantACount >= 2);
        assertTrue(tenantBCount >= 1);

        // Tenant A tree should not contain Tenant B categories
        List<String> tenantANames = tenantATree.stream().map(CategoryDto::name).toList();
        assertFalse(tenantANames.contains("Category B1"));

        // Tenant B tree should not contain Tenant A categories
        List<String> tenantBNames = tenantBTree.stream().map(CategoryDto::name).toList();
        assertFalse(tenantBNames.contains("Category A1"));
        assertFalse(tenantBNames.contains("Category A2"));
    }

    @Test
    void slugUniqueness_shouldGenerateUniqueSlugWithinTenant() {
        CreateCategoryRequest request1 = new CreateCategoryRequest(
                "Electronics", null, null, null,
                null, null, null, null
        );
        CreateCategoryRequest request2 = new CreateCategoryRequest(
                "Electronics", null, null, null,
                null, null, null, null
        );

        CategoryDto cat1 = categoryService.createCategory(tenantAId, TEST_USER_ID, request1);
        CategoryDto cat2 = categoryService.createCategory(tenantAId, TEST_USER_ID, request2);

        assertNotEquals(cat1.slug(), cat2.slug());
        assertNotEquals(cat1.id(), cat2.id());
    }

    @Test
    void slugUniqueness_sameSlugsAllowedAcrossTenants() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Electronics", "electronics", null, null,
                null, null, null, null
        );

        CategoryDto catA = categoryService.createCategory(tenantAId, TEST_USER_ID, request);
        CategoryDto catB = categoryService.createCategory(tenantBId, TEST_USER_ID, request);

        // Same slug is allowed for different tenants
        assertEquals("electronics", catA.slug());
        assertEquals("electronics", catB.slug());
    }

    @Test
    void getCategory_shouldReturnExistingCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Fetch Me", null, "Fetch description", "https://img.example.com/cat.png",
                5, null, "Fetch Meta", "Fetch meta desc"
        );

        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, request);
        CategoryDto fetched = categoryService.getCategory(tenantAId, created.id());

        assertEquals(created.id(), fetched.id());
        assertEquals("Fetch Me", fetched.name());
        assertEquals("Fetch description", fetched.description());
        assertEquals("https://img.example.com/cat.png", fetched.imageUrl());
        assertEquals(5, fetched.sortOrder());
        assertEquals("Fetch Meta", fetched.metaTitle());
        assertEquals("Fetch meta desc", fetched.metaDescription());
    }

    @Test
    void getCategory_notFound_shouldThrow() {
        UUID randomId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () ->
                categoryService.getCategory(tenantAId, randomId)
        );
    }

    @Test
    void deleteCategory_crossTenant_shouldThrow() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Tenant A Only", null, null, null,
                null, null, null, null
        );
        CategoryDto created = categoryService.createCategory(tenantAId, TEST_USER_ID, request);

        assertThrows(ResourceNotFoundException.class, () ->
                categoryService.deleteCategory(tenantBId, TEST_USER_ID, created.id())
        );

        // Verify it still exists for tenant A
        CategoryDto stillExists = categoryService.getCategory(tenantAId, created.id());
        assertNotNull(stillExists);
    }
}
