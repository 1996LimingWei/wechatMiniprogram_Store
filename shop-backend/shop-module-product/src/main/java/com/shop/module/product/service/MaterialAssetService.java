package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.config.MaterialStorageProperties;
import com.shop.module.product.dal.dataobject.MaterialAssetDO;
import com.shop.module.product.dal.mysql.MaterialAssetMapper;
import com.shop.module.product.vo.MaterialAssetRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialAssetService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MaterialAssetMapper materialAssetMapper;
    private final MaterialFileStorageService fileStorageService;
    private final MaterialStorageProperties properties;

    @Transactional
    public MaterialAssetRespVO upload(MultipartFile file, String bizType, Long adminId) {
        validateFile(file);
        byte[] content = readBytes(file);
        String detectedType = detectImageType(content);
        ImageSize imageSize = readImageSize(content);
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        validateTypeConsistency(extension, normalizeContentType(file.getContentType()), detectedType);
        String objectKey = buildObjectKey(extension);
        String url = fileStorageService.store(content, objectKey);

        MaterialAssetDO asset = new MaterialAssetDO();
        asset.setUrl(url);
        asset.setObjectKey(objectKey);
        asset.setFileName(normalizeFileName(file.getOriginalFilename()));
        asset.setContentType(normalizeContentType(file.getContentType()));
        asset.setFileSize((long) content.length);
        asset.setWidth(imageSize.width());
        asset.setHeight(imageSize.height());
        asset.setBizType(normalizeBizType(bizType));
        asset.setReferenceCount(0);
        asset.setCreatedBy(adminId);
        materialAssetMapper.insert(asset);
        return toRespVO(asset);
    }

    public PageResult<MaterialAssetRespVO> page(PageParam pageParam, String bizType, String keyword,
                                                Long createdBy, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<MaterialAssetDO> wrapper = new LambdaQueryWrapper<MaterialAssetDO>()
                .eq(hasText(bizType), MaterialAssetDO::getBizType, bizType)
                .eq(createdBy != null, MaterialAssetDO::getCreatedBy, createdBy)
                .ge(startTime != null, MaterialAssetDO::getCreateTime, startTime)
                .le(endTime != null, MaterialAssetDO::getCreateTime, endTime)
                .and(hasText(keyword), w -> w.like(MaterialAssetDO::getFileName, keyword)
                        .or()
                        .like(MaterialAssetDO::getUrl, keyword))
                .orderByDesc(MaterialAssetDO::getCreateTime);
        PageResult<MaterialAssetDO> page = materialAssetMapper.selectPage(pageParam, wrapper);
        List<MaterialAssetRespVO> list = page.getList().stream()
                .map(this::refreshReferenceCount)
                .map(this::toRespVO)
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    public List<String> references(Long id) {
        MaterialAssetDO asset = getExistingAsset(id);
        int referenceCount = materialAssetMapper.countReferences(asset.getUrl());
        updateReferenceCount(asset, referenceCount);
        return materialAssetMapper.selectReferences(asset.getUrl());
    }

    public void validateBusinessImageUrl(String url, String fieldName, boolean required) {
        String value = url == null ? "" : url.trim();
        if (value.isEmpty()) {
            if (required) throw new ServerException(400, fieldName + "不能为空");
            return;
        }
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (value.length() > 1024 || lowerValue.startsWith("wxfile://") || lowerValue.startsWith("http://tmp/")
                || lowerValue.startsWith("file://")) {
            throw new ServerException(400, fieldName + "必须使用素材库中的正式图片");
        }
        boolean relativeMaterialUrl = value.startsWith("/uploads/material/");
        boolean publicBaseMatched = hasText(properties.getPublicBaseUrl())
                && value.startsWith(normalizeBaseUrl(properties.getPublicBaseUrl()));
        List<String> allowedPrefixes = properties.getAllowedUrlPrefixes() == null
                ? List.of() : properties.getAllowedUrlPrefixes();
        boolean configuredPrefixMatched = allowedPrefixes.stream()
                .filter(this::hasText)
                .map(this::normalizeBaseUrl)
                .anyMatch(value::startsWith);
        if (relativeMaterialUrl || publicBaseMatched || configuredPrefixMatched || existsActiveAsset(value)) {
            return;
        }
        throw new ServerException(400, fieldName + "必须来自素材库或配置白名单");
    }

    public void refreshAllReferenceCounts() {
        List<MaterialAssetDO> assets = materialAssetMapper.selectList(new LambdaQueryWrapper<MaterialAssetDO>()
                .select(MaterialAssetDO::getId, MaterialAssetDO::getUrl, MaterialAssetDO::getReferenceCount));
        for (MaterialAssetDO asset : assets) {
            int referenceCount = materialAssetMapper.countReferences(asset.getUrl());
            if (!Objects.equals(asset.getReferenceCount(), referenceCount)) {
                updateReferenceCount(asset, referenceCount);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        MaterialAssetDO asset = getExistingAsset(id);
        int referenceCount = materialAssetMapper.countReferences(asset.getUrl());
        if (referenceCount > 0) {
            updateReferenceCount(asset, referenceCount);
            throw new ServerException(409, "素材已被业务引用，不能删除");
        }
        materialAssetMapper.deleteById(id);
        fileStorageService.delete(asset.getObjectKey());
    }

    private MaterialAssetDO refreshReferenceCount(MaterialAssetDO asset) {
        int referenceCount = materialAssetMapper.countReferences(asset.getUrl());
        if (!Integer.valueOf(referenceCount).equals(asset.getReferenceCount())) {
            updateReferenceCount(asset, referenceCount);
            asset.setReferenceCount(referenceCount);
        }
        return asset;
    }

    private void updateReferenceCount(MaterialAssetDO asset, int referenceCount) {
        MaterialAssetDO update = new MaterialAssetDO();
        update.setId(asset.getId());
        update.setReferenceCount(referenceCount);
        materialAssetMapper.updateById(update);
    }

    private MaterialAssetDO getExistingAsset(Long id) {
        if (id == null) {
            throw new ServerException(400, "素材ID不能为空");
        }
        MaterialAssetDO asset = materialAssetMapper.selectById(id);
        if (asset == null) {
            throw new ServerException(404, "素材不存在");
        }
        return asset;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServerException(400, "请选择要上传的图片");
        }
        if (file.getSize() > properties.getMaxSize()) {
            throw new ServerException(400, "图片不能超过 " + properties.getMaxSize() / 1024 / 1024 + "MB");
        }
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServerException(400, "仅支持 JPG、PNG、WebP 图片");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServerException(400, "图片类型不支持");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ServerException(400, "读取上传文件失败");
        }
    }

    private String detectImageType(byte[] content) {
        if (content.length < 12) {
            throw new ServerException(400, "图片文件不完整");
        }
        boolean jpeg = content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF;
        boolean png = content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47;
        boolean webp = content[0] == 0x52 && content[1] == 0x49 && content[2] == 0x46 && content[3] == 0x46
                && content[8] == 0x57 && content[9] == 0x45 && content[10] == 0x42 && content[11] == 0x50;
        if (jpeg) return "jpeg";
        if (png) return "png";
        if (webp) return "webp";
        throw new ServerException(400, "图片文件内容不合法");
    }

    private void validateTypeConsistency(String extension, String contentType, String detectedType) {
        boolean extensionMatched = ("jpeg".equals(detectedType) && ("jpg".equals(extension) || "jpeg".equals(extension)))
                || detectedType.equals(extension);
        boolean contentTypeMatched = ("jpeg".equals(detectedType) && "image/jpeg".equals(contentType))
                || ("png".equals(detectedType) && "image/png".equals(contentType))
                || ("webp".equals(detectedType) && "image/webp".equals(contentType));
        if (!extensionMatched || !contentTypeMatched) {
            throw new ServerException(400, "图片扩展名、MIME 和文件内容不一致");
        }
    }

    private ImageSize readImageSize(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                return new ImageSize(null, null);
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return new ImageSize(null, null);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String filename = originalFilename == null ? "" : originalFilename;
        int index = filename.lastIndexOf('.');
        if (index >= 0 && index < filename.length() - 1) {
            String extension = filename.substring(index + 1).toLowerCase(Locale.ROOT);
            if ("jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension) || "webp".equals(extension)) {
                return extension;
            }
        }
        String normalizedContentType = normalizeContentType(contentType);
        if ("image/jpeg".equals(normalizedContentType)) return "jpg";
        if ("image/png".equals(normalizedContentType)) return "png";
        if ("image/webp".equals(normalizedContentType)) return "webp";
        return "";
    }

    private String buildObjectKey(String extension) {
        LocalDate today = LocalDate.now();
        return "%04d/%02d/%02d/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID(), extension);
    }

    private String normalizeFileName(String originalFilename) {
        if (!hasText(originalFilename)) {
            return "未命名图片";
        }
        return originalFilename.replace("\\", "/").substring(originalFilename.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String normalizeBizType(String bizType) {
        if (!hasText(bizType)) {
            return "common";
        }
        String normalized = bizType.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]{1,32}")) {
            throw new ServerException(400, "素材业务类型格式不正确");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
    }

    private boolean existsActiveAsset(String url) {
        Long count = materialAssetMapper.selectCount(new LambdaQueryWrapper<MaterialAssetDO>()
                .eq(MaterialAssetDO::getUrl, url));
        return count != null && count > 0;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private MaterialAssetRespVO toRespVO(MaterialAssetDO asset) {
        MaterialAssetRespVO vo = new MaterialAssetRespVO();
        vo.setId(asset.getId());
        vo.setUrl(asset.getUrl());
        vo.setObjectKey(asset.getObjectKey());
        vo.setFileName(asset.getFileName());
        vo.setContentType(asset.getContentType());
        vo.setFileSize(asset.getFileSize());
        vo.setWidth(asset.getWidth());
        vo.setHeight(asset.getHeight());
        vo.setBizType(asset.getBizType());
        vo.setReferenceCount(asset.getReferenceCount());
        vo.setCreatedBy(asset.getCreatedBy());
        vo.setCreateTime(asset.getCreateTime());
        vo.setUpdateTime(asset.getUpdateTime());
        return vo;
    }

    private record ImageSize(Integer width, Integer height) {
    }
}
