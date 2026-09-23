package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.file.FileResponse;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API upload/download/quản lý file - lõi ABE. Ai được GỌI endpoint nào chặn ở
 * SecurityConfig; ai TẢI VỀ ĐƯỢC một file cụ thể (403 hay không) do
 * FileService quyết định dựa trên accessPolicy của file so với attribute
 * (còn hiệu lực) của user gọi request.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accessPolicy") String accessPolicy,
            Authentication authentication
    ) {
        User owner = currentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.upload(file, accessPolicy, owner));
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listAll() {
        return ResponseEntity.ok(fileService.listAll());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FileResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(fileService.listMine(currentUser(authentication)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication authentication) {
        FileService.DownloadResult result = fileService.download(id, currentUser(authentication));

        String encodedFileName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = result.contentType() != null
                ? MediaType.parseMediaType(result.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(result.content());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        fileService.delete(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User đã xác thực nhưng không tìm thấy trong DB"));
    }
}
