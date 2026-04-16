package com.g2u.admin.service;

import com.g2u.admin.domain.media.Media;
import com.g2u.admin.domain.media.MediaRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.domain.product.ProductStatus;
import com.g2u.admin.domain.tenant.Tenant;
import com.g2u.admin.domain.tenant.TenantRepository;
import com.g2u.admin.web.dto.MediaDto;
import com.g2u.admin.web.dto.ProductMediaDetailDto;
import com.g2u.admin.web.exception.BusinessRuleViolationException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MediaServiceIntegrationTest {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantAId;
    private UUID tenantBId;

    @BeforeEach
    void setUp() {
        Tenant tenantA = Tenant.builder()
                .name("Media Tenant A")
                .slug("media-tenant-a-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        Tenant tenantB = Tenant.builder()
                .name("Media Tenant B")
                .slug("media-tenant-b-" + UUID.randomUUID().toString().substring(0, 8))
                .active(true)
                .build();
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
    }

    @Test
    void upload_shouldPersistAndReturnDto() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.png", "image/png", new byte[]{1, 2, 3, 4});

        MediaDto dto = mediaService.upload(tenantAId, file);

        assertNotNull(dto.id());
        assertEquals("test-image.png", dto.originalName());
        assertEquals("image/png", dto.mimeType());
        assertEquals(4, dto.size());
        assertTrue(dto.url().startsWith("/api/media/"));
        assertNotNull(dto.createdAt());
    }

    @Test
    void upload_invalidType_shouldReject() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "readme.txt", "text/plain", "hello".getBytes());

        assertThrows(IllegalArgumentException.class, () ->
                mediaService.upload(tenantAId, file));
    }

    @Test
    void upload_emptyFile_shouldReject() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                mediaService.upload(tenantAId, file));
    }

    @Test
    void delete_shouldRemoveRecord() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "to-delete.png", "image/png", new byte[]{1, 2, 3});
        MediaDto dto = mediaService.upload(tenantAId, file);

        mediaService.delete(dto.id(), tenantAId);

        assertTrue(mediaRepository.findByIdAndTenantId(dto.id(), tenantAId).isEmpty());
    }

    @Test
    void delete_wrongTenant_shouldFail() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tenant-a.png", "image/png", new byte[]{1, 2, 3});
        MediaDto dto = mediaService.upload(tenantAId, file);

        assertThrows(ResourceNotFoundException.class, () ->
                mediaService.delete(dto.id(), tenantBId));
    }

    @Test
    void updateAltText_shouldPersist() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "alt-test.png", "image/png", new byte[]{1, 2, 3});
        MediaDto dto = mediaService.upload(tenantAId, file);

        MediaDto updated = mediaService.updateAltText(dto.id(), tenantAId, "A blue sky");

        assertEquals("A blue sky", updated.altText());
        assertEquals(dto.id(), updated.id());
    }

    @Test
    void tenantIsolation_mediaFromTenantANotVisibleToTenantB() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "secret.png", "image/png", new byte[]{1, 2, 3});
        MediaDto dto = mediaService.upload(tenantAId, file);

        assertThrows(ResourceNotFoundException.class, () ->
                mediaService.getMedia(dto.id(), tenantBId));
    }

    @Test
    void listMedia_shouldReturnOnlyTenantMedia() {
        mediaService.upload(tenantAId, new MockMultipartFile(
                "file", "a1.png", "image/png", new byte[]{1}));
        mediaService.upload(tenantAId, new MockMultipartFile(
                "file", "a2.png", "image/png", new byte[]{2}));
        mediaService.upload(tenantBId, new MockMultipartFile(
                "file", "b1.png", "image/png", new byte[]{3}));

        List<MediaDto> tenantAMedia = mediaService.listMedia(tenantAId);
        List<MediaDto> tenantBMedia = mediaService.listMedia(tenantBId);

        assertTrue(tenantAMedia.size() >= 2);
        assertTrue(tenantBMedia.size() >= 1);
    }

    @Test
    void addMediaToProduct_shouldLink() {
        Product product = createProduct(tenantAId, "Product With Media");
        MockMultipartFile file = new MockMultipartFile(
                "file", "product-img.png", "image/png", new byte[]{1, 2, 3});
        MediaDto media = mediaService.upload(tenantAId, file);

        ProductMediaDetailDto linked = mediaService.addMediaToProduct(
                product.getId(), media.id(), tenantAId);

        assertNotNull(linked.id());
        assertEquals(media.id(), linked.mediaId());
        assertEquals(0, linked.sortOrder());
        assertTrue(linked.primary());
    }

    @Test
    void addMediaToProduct_maxExceeded_shouldReject() {
        Product product = createProduct(tenantAId, "Product Max Media");

        // Add 20 images
        List<UUID> mediaIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "img-" + i + ".png", "image/png", new byte[]{(byte) i});
            MediaDto media = mediaService.upload(tenantAId, file);
            mediaService.addMediaToProduct(product.getId(), media.id(), tenantAId);
            mediaIds.add(media.id());
        }

        // 21st should fail
        MockMultipartFile extraFile = new MockMultipartFile(
                "file", "extra.png", "image/png", new byte[]{99});
        MediaDto extraMedia = mediaService.upload(tenantAId, extraFile);

        assertThrows(BusinessRuleViolationException.class, () ->
                mediaService.addMediaToProduct(product.getId(), extraMedia.id(), tenantAId));
    }

    @Test
    void addMediaToProduct_duplicateLink_shouldReject() {
        Product product = createProduct(tenantAId, "Product Dup Link");
        MockMultipartFile file = new MockMultipartFile(
                "file", "dup.png", "image/png", new byte[]{1});
        MediaDto media = mediaService.upload(tenantAId, file);

        mediaService.addMediaToProduct(product.getId(), media.id(), tenantAId);

        assertThrows(BusinessRuleViolationException.class, () ->
                mediaService.addMediaToProduct(product.getId(), media.id(), tenantAId));
    }

    @Test
    void removeMediaFromProduct_shouldUnlink() {
        Product product = createProduct(tenantAId, "Product Unlink");
        MockMultipartFile file = new MockMultipartFile(
                "file", "unlink.png", "image/png", new byte[]{1});
        MediaDto media = mediaService.upload(tenantAId, file);
        mediaService.addMediaToProduct(product.getId(), media.id(), tenantAId);

        mediaService.removeMediaFromProduct(product.getId(), media.id(), tenantAId);

        List<ProductMediaDetailDto> remaining = mediaService.getProductMedia(product.getId(), tenantAId);
        assertTrue(remaining.stream().noneMatch(pm -> media.id().equals(pm.mediaId())));
    }

    @Test
    void getProductMedia_shouldReturnSorted() {
        Product product = createProduct(tenantAId, "Product Sorted Media");

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "first.png", "image/png", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "second.png", "image/png", new byte[]{2});
        MediaDto media1 = mediaService.upload(tenantAId, file1);
        MediaDto media2 = mediaService.upload(tenantAId, file2);

        mediaService.addMediaToProduct(product.getId(), media1.id(), tenantAId);
        mediaService.addMediaToProduct(product.getId(), media2.id(), tenantAId);

        List<ProductMediaDetailDto> productMedia = mediaService.getProductMedia(product.getId(), tenantAId);

        assertEquals(2, productMedia.size());
        assertEquals(0, productMedia.get(0).sortOrder());
        assertEquals(1, productMedia.get(1).sortOrder());
    }

    @Test
    void reorderProductMedia_shouldUpdateSortOrder() {
        Product product = createProduct(tenantAId, "Product Reorder");

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "r1.png", "image/png", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "r2.png", "image/png", new byte[]{2});
        MediaDto media1 = mediaService.upload(tenantAId, file1);
        MediaDto media2 = mediaService.upload(tenantAId, file2);

        mediaService.addMediaToProduct(product.getId(), media1.id(), tenantAId);
        mediaService.addMediaToProduct(product.getId(), media2.id(), tenantAId);

        // Reorder: media2 first, media1 second
        mediaService.reorderProductMedia(product.getId(), tenantAId,
                List.of(media2.id(), media1.id()));

        List<ProductMediaDetailDto> reordered = mediaService.getProductMedia(product.getId(), tenantAId);
        assertEquals(media2.id(), reordered.get(0).mediaId());
        assertTrue(reordered.get(0).primary());
        assertEquals(media1.id(), reordered.get(1).mediaId());
        assertFalse(reordered.get(1).primary());
    }

    private Product createProduct(UUID tenantId, String name) {
        Product product = Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID().toString().substring(0, 6))
                .status(ProductStatus.DRAFT)
                .currency("UAH")
                .weightUnit("kg")
                .trackInventory(false)
                .quantity(0)
                .variants(new ArrayList<>())
                .media(new ArrayList<>())
                .categories(new HashSet<>())
                .tags(new HashSet<>())
                .build();
        product.setTenantId(tenantId);
        return productRepository.save(product);
    }
}
