package com.g2u.admin.service;

import com.g2u.admin.domain.tag.Tag;
import com.g2u.admin.domain.tag.TagRepository;
import com.g2u.admin.util.SlugUtils;
import com.g2u.admin.web.dto.CreateTagRequest;
import com.g2u.admin.web.dto.TagDto;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final AuditService auditService;

    public TagService(TagRepository tagRepository, AuditService auditService) {
        this.tagRepository = tagRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TagDto> getTags(UUID tenantId) {
        return tagRepository.findByTenantId(tenantId).stream()
                .map(t -> new TagDto(t.getId(), t.getName(), t.getSlug()))
                .toList();
    }

    public TagDto createTag(UUID tenantId, UUID userId, CreateTagRequest request) {
        String baseSlug = request.slug() != null ? request.slug() : SlugUtils.generateSlug(request.name());
        String slug = SlugUtils.ensureUnique(baseSlug,
                s -> tagRepository.existsByTenantIdAndSlug(tenantId, s));

        Tag tag = Tag.builder().name(request.name()).slug(slug).build();
        tag.setTenantId(tenantId);
        tag = tagRepository.save(tag);

        auditService.log(tenantId, userId, "CREATE", "TAG", tag.getId(),
                Map.of("name", tag.getName(), "slug", tag.getSlug()));

        return new TagDto(tag.getId(), tag.getName(), tag.getSlug());
    }

    public void deleteTag(UUID tenantId, UUID userId, UUID tagId) {
        Tag tag = tagRepository.findByTenantIdAndId(tenantId, tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));
        tagRepository.delete(tag);

        auditService.log(tenantId, userId, "DELETE", "TAG", tagId,
                Map.of("name", tag.getName()));
    }
}
