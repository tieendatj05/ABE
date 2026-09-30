package com.abe.system.abe_system.service;

import com.abe.system.abe_system.crypto.AesFileCipher;
import com.abe.system.abe_system.crypto.PolicyKeyDistributor;
import com.abe.system.abe_system.crypto.PolicyKeyDistributor.LeafShare;
import com.abe.system.abe_system.crypto.PolicyNode;
import com.abe.system.abe_system.crypto.PolicyParser;
import com.abe.system.abe_system.dto.file.FileResponse;
import com.abe.system.abe_system.entity.FileMetadata;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.ContentIntegrityException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.FileMetadataRepository;
import com.abe.system.abe_system.repository.UserAttributeRepository;
import com.abe.system.abe_system.storage.ObjectStorageService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ lõi ABE: mã hoá file bằng AES-256 lúc upload, chia khoá AES theo
 * accessPolicy bằng Shamir Secret Sharing ({@link PolicyKeyDistributor}), và
 * ghép lại khoá để giải mã lúc download nếu attribute (còn hiệu lực) của
 * người tải thoả policy.
 *
 * Nội dung file KHÔNG bao giờ đi qua DB - chỉ lưu ciphertext trên object
 * storage (MinIO cho dev/prod, ổ đĩa local cho test - xem package
 * {@code storage}), DB chỉ giữ metadata + khoá AES đã được chia sẻ.
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserAttributeRepository userAttributeRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final ObjectStorageService objectStorageService;
    private final ContentIntegrityService contentIntegrityService;

    public record DownloadResult(String fileName, String contentType, byte[] content) {
    }

    @Transactional
    public FileResponse upload(MultipartFile file, String accessPolicy, User owner) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống");
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File upload thiếu tên gốc");
        }

        // Parse trước để fail-fast nếu policy sai cú pháp, trước khi ghi gì lên storage/DB.
        PolicyNode policyTree = PolicyParser.parse(accessPolicy);

        byte[] plaintext;
        try {
            plaintext = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được nội dung file upload", e);
        }

        byte[] aesKey = AesFileCipher.generateKey();
        byte[] ciphertext = AesFileCipher.encrypt(plaintext, aesKey);

        List<LeafShare> leafShares = PolicyKeyDistributor.distribute(policyTree, AesFileCipher.keyToSecret(aesKey));
        String encryptedAesKeyJson = serializeShares(leafShares);

        String storageKey = UUID.randomUUID() + ".enc";
        objectStorageService.store(storageKey, ciphertext, "application/octet-stream");

        // Hash file GỐC (plaintext, trước khi mã hoá) - dùng để phát hiện can
        // thiệp lúc download (xem ContentIntegrityService.verify).
        String contentHash = contentIntegrityService.sha256Hex(plaintext);

        FileMetadata metadata = FileMetadata.builder()
                .fileName(originalFileName)
                .storageKey(storageKey)
                .accessPolicy(accessPolicy)
                .encryptedAesKey(encryptedAesKeyJson)
                .owner(owner)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .contentHash(contentHash)
                .build();

        FileMetadata saved = fileMetadataRepository.save(metadata);
        auditLogService.recordUpload(saved, owner);
        return FileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listAll() {
        return fileMetadataRepository.findAll().stream().map(FileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listMine(User owner) {
        return fileMetadataRepository.findByOwnerId(owner.getId()).stream().map(FileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DownloadResult download(Long fileId, User requester) {
        FileMetadata metadata = getOrThrow(fileId);

        Set<String> activeAttributes = userAttributeRepository.findByUserAndRevokedFalse(requester).stream()
                .map(ua -> ua.getAttribute().getAttributeName())
                .collect(Collectors.toSet());

        PolicyNode policyTree = PolicyParser.parse(metadata.getAccessPolicy());
        List<LeafShare> leafShares = deserializeShares(metadata.getEncryptedAesKey());

        Optional<BigInteger> secret = PolicyKeyDistributor.reconstruct(policyTree, activeAttributes, leafShares);
        if (secret.isEmpty()) {
            auditLogService.recordDownloadDenied(metadata, requester, activeAttributes);
            throw new AccessDeniedException("Bạn không có đủ thuộc tính thoả access policy của file này");
        }
        BigInteger keySecret = secret.get();

        byte[] aesKey = AesFileCipher.secretToKey(keySecret);
        byte[] ciphertext = objectStorageService.retrieve(metadata.getStorageKey());
        byte[] plaintext = AesFileCipher.decrypt(ciphertext, aesKey);

        // Giải mã kỹ thuật thành công (đủ quyền + GCM tag hợp lệ) KHÔNG có nghĩa
        // nội dung chắc chắn đúng nguyên bản - xác nhận thêm bằng SHA-256 độc lập.
        try {
            contentIntegrityService.verify(metadata.getContentHash(), plaintext);
        } catch (ContentIntegrityException ex) {
            auditLogService.recordIntegrityViolation(metadata, requester);
            throw ex;
        }

        auditLogService.recordDownloadSuccess(metadata, requester);
        return new DownloadResult(metadata.getFileName(), metadata.getContentType(), plaintext);
    }

    @Transactional
    public void delete(Long fileId, User requester) {
        FileMetadata metadata = getOrThrow(fileId);
        boolean isOwner = metadata.getOwner().getId().equals(requester.getId());
        if (!isOwner && requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ chủ file hoặc ADMIN mới được xoá file này");
        }
        objectStorageService.delete(metadata.getStorageKey());
        String fileName = metadata.getFileName();
        String accessPolicy = metadata.getAccessPolicy();
        User owner = metadata.getOwner();
        fileMetadataRepository.delete(metadata);
        auditLogService.recordDelete(fileId, fileName, accessPolicy, owner, requester);
    }

    private FileMetadata getOrThrow(Long fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file id=" + fileId));
    }

    // Lưu List<LeafShare> dưới dạng JSON thủ công qua Map<String,String> (share
    // ghi thành chuỗi thập phân của BigInteger) thay vì để Jackson tự suy luận
    // constructor của record - tránh phụ thuộc vào cờ compile "-parameters".
    private String serializeShares(List<LeafShare> shares) {
        List<Map<String, String>> raw = shares.stream()
                .map(s -> Map.of("attributeName", s.attributeName(), "share", s.value().toString()))
                .toList();
        return objectMapper.writeValueAsString(raw);
    }

    private List<LeafShare> deserializeShares(String json) {
        List<Map<String, String>> raw = objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {
        });
        return raw.stream()
                .map(m -> new LeafShare(m.get("attributeName"), new BigInteger(m.get("share"))))
                .toList();
    }
}
