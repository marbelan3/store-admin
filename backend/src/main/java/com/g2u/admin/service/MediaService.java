package com.g2u.admin.service;

import com.g2u.admin.domain.media.Media;
import com.g2u.admin.domain.media.MediaRepository;
import com.g2u.admin.domain.product.Product;
import com.g2u.admin.domain.product.ProductMedia;
import com.g2u.admin.domain.product.ProductMediaRepository;
import com.g2u.admin.domain.product.ProductRepository;
import com.g2u.admin.service.storage.FileStorageService;
import com.g2u.admin.web.dto.MediaDto;
import com.g2u.admin.web.dto.ProductMediaDetailDto;
import com.g2u.admin.web.exception.BusinessRuleViolationException;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private static final int MAX_IMAGES_PER_PRODUCT = 20;

    private final MediaRepository mediaRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public MediaService(MediaRepository mediaRepository,
                        ProductMediaRepository productMediaRepository,
                        ProductRepository productRepository,
                        FileStorageService fileStorageService) {
        this.mediaRepository = mediaRepository;
        this.productMediaRepository = productMediaRepository;
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload a file and persist its metadata in the media table.
     * File validation (type, size) is handled by FileStorageService.
     */
    public MediaDto upload(UUID tenantId, MultipartFile file) {
        String storedPath;
        try {
            storedPath = fileStorageService.store(file, tenantId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        String url = "/api/media/" + storedPath;
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        // Extract the generated filename from the stored path (tenantId/filename)
        String filename = storedPath.substring(storedPath.indexOf('/') + 1);

        Media media = Media.builder()
                .filename(filename)
                .originalName(originalName)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .url(url)
                .build();
        media.setTenantId(tenantId);

        media = mediaRepository.save(media);
        log.info("Uploaded media '{}' (id={}) for tenant {}", originalName, media.getId(), tenantId);

        return toDto(media);
    }

    /**
     * Delete a media record and its physical file.
     */
    public void delete(UUID mediaId, UUID tenantId) {
        Media media = mediaRepository.findByIdAndTenantId(mediaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        // Delete physical file
        String storedPath = tenantId + "/" + media.getFilename();
        try {
            fileStorageService.delete(storedPath);
        } catch (Exception e) {
            log.warn("Failed to delete file from disk: {}", storedPath, e);
        }

        mediaRepository.delete(media);
        log.info("Deleted media '{}' (id={}) for tenant {}", media.getOriginalName(), mediaId, tenantId);
    }

    /**
     * Update alt text for a media record.
     */
    public MediaDto updateAltText(UUID mediaId, UUID tenantId, String altText) {
        Media media = mediaRepository.findByIdAndTenantId(mediaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        media.setAltText(altText);
        media = mediaRepository.save(media);
        log.info("Updated alt text for media id={} tenant={}", mediaId, tenantId);

        return toDto(media);
    }

    @Transactional(readOnly = true)
    public MediaDto getMedia(UUID mediaId, UUID tenantId) {
        Media media = mediaRepository.findByIdAndTenantId(mediaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));
        return toDto(media);
    }

    @Transactional(readOnly = true)
    public List<MediaDto> listMedia(UUID tenantId) {
        return mediaRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    // --- Product-Media linking ---

    @Transactional(readOnly = true)
    public List<ProductMediaDetailDto> getProductMedia(UUID productId, UUID tenantId) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        return productMediaRepository.findByProductIdAndTenantId(productId, tenantId).stream()
                .map(this::toProductMediaDetailDto)
                .toList();
    }

    public ProductMediaDetailDto addMediaToProduct(UUID productId, UUID mediaId, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Media media = mediaRepository.findByIdAndTenantId(mediaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        long currentCount = productMediaRepository.countByProductId(productId);
        if (currentCount >= MAX_IMAGES_PER_PRODUCT) {
            throw new BusinessRuleViolationException(
                    "Product cannot have more than " + MAX_IMAGES_PER_PRODUCT + " images");
        }

        if (productMediaRepository.findByProductIdAndMediaId(productId, mediaId).isPresent()) {
            throw new BusinessRuleViolationException("Media is already linked to this product");
        }

        ProductMedia productMedia = ProductMedia.builder()
                .product(product)
                .url(media.getUrl())
                .altText(media.getAltText())
                .mediaType("image")
                .sortOrder((int) currentCount)
                .primary(currentCount == 0)
                .mediaId(mediaId)
                .build();
        productMedia.setTenantId(tenantId);

        productMedia = productMediaRepository.save(productMedia);
        log.info("Linked media {} to product {} for tenant {}", mediaId, productId, tenantId);

        return toProductMediaDetailDto(productMedia);
    }

    public void removeMediaFromProduct(UUID productId, UUID mediaId, UUID tenantId) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        ProductMedia pm = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductMedia",
                        "productId=" + productId + ", mediaId=" + mediaId));

        productMediaRepository.delete(pm);
        log.info("Unlinked media {} from product {} for tenant {}", mediaId, productId, tenantId);
    }

    public void reorderProductMedia(UUID productId, UUID tenantId, List<UUID> mediaIds) {
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        List<ProductMedia> existingMedia = productMediaRepository.findByProductIdAndTenantId(productId, tenantId);

        for (int i = 0; i < mediaIds.size(); i++) {
            UUID mid = mediaIds.get(i);
            for (ProductMedia pm : existingMedia) {
                if (mid.equals(pm.getMediaId())) {
                    pm.setSortOrder(i);
                    pm.setPrimary(i == 0);
                    break;
                }
            }
        }

        productMediaRepository.saveAll(existingMedia);
        log.info("Reordered {} media items for product {} tenant {}", mediaIds.size(), productId, tenantId);
    }

    // --- Private helpers ---

    private MediaDto toDto(Media media) {
        return new MediaDto(
                media.getId(),
                media.getFilename(),
                media.getOriginalName(),
                media.getMimeType(),
                media.getSize(),
                media.getUrl(),
                media.getAltText(),
                media.getCreatedAt()
        );
    }

    private ProductMediaDetailDto toProductMediaDetailDto(ProductMedia pm) {
        return new ProductMediaDetailDto(
                pm.getId(),
                pm.getMediaId(),
                pm.getUrl(),
                pm.getAltText(),
                pm.getMediaType(),
                pm.getSortOrder(),
                pm.isPrimary(),
                pm.getCreatedAt()
        );
    }
}
