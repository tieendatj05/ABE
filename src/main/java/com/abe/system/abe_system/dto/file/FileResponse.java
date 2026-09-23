package com.abe.system.abe_system.dto.file;

import com.abe.system.abe_system.entity.FileMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FileResponse {

    private Long id;
    private String fileName;
    private String ownerUsername;
    private String accessPolicy;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createdAt;

    public static FileResponse from(FileMetadata metadata) {
        return new FileResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getOwner().getUsername(),
                metadata.getAccessPolicy(),
                metadata.getFileSize(),
                metadata.getContentType(),
                metadata.getCreatedAt()
        );
    }
}
