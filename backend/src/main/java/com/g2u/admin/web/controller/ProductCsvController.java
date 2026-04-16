package com.g2u.admin.web.controller;

import com.g2u.admin.infrastructure.security.CurrentUser;
import com.g2u.admin.infrastructure.security.UserPrincipal;
import com.g2u.admin.service.ProductCsvService;
import com.g2u.admin.web.dto.ImportResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
public class ProductCsvController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCsvController.class);

    private final ProductCsvService productCsvService;

    public ProductCsvController(ProductCsvService productCsvService) {
        this.productCsvService = productCsvService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts(@CurrentUser UserPrincipal principal) {
        LOG.debug("REST request to export products as CSV for tenant: {}", principal.tenantId());
        String csv = productCsvService.exportProducts(principal.tenantId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> importProducts(
            @CurrentUser UserPrincipal principal,
            @RequestParam("file") MultipartFile file) throws IOException {
        LOG.debug("REST request to import products from CSV for tenant: {}", principal.tenantId());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportResultDto(0, 0, java.util.List.of(
                            new ImportResultDto.ImportError(0, "File is empty"))));
        }

        ImportResultDto result = productCsvService.importProducts(principal.tenantId(), file);
        return ResponseEntity.ok(result);
    }
}
