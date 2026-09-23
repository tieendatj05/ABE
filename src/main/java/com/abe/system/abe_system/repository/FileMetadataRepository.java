package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.FileMetadata;
import com.abe.system.abe_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository cho FileMetadata.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    // Dùng cho màn hình "File của tôi" của Data Owner.
    List<FileMetadata> findByOwner(User owner);

    List<FileMetadata> findByOwnerId(Long ownerId);
}
