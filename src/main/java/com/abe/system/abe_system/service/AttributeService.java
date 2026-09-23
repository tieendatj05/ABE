package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.attribute.AttributeRequest;
import com.abe.system.abe_system.dto.attribute.AttributeResponse;
import com.abe.system.abe_system.dto.attribute.UserAttributeResponse;
import com.abe.system.abe_system.entity.Attribute;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.entity.UserAttribute;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.AttributeRepository;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserAttributeRepository;
import com.abe.system.abe_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiệp vụ quản lý "kho" Attribute và gán/thu hồi attribute cho user.
 *
 * Mô hình ABE phi tập trung hóa: bên cạnh ADMIN toàn cục (quyền không giới
 * hạn, tương thích ngược với hệ thống cũ), mỗi phòng ban có thể có một
 * DEPT_ADMIN - một KGC thu nhỏ chỉ được:
 *   - Tạo/xoá attribute do CHÍNH phòng ban mình phát hành (issuerDepartment).
 *   - Gán/thu hồi CÁC attribute đó cho user THUỘC CÙNG phòng ban.
 * SecurityConfig chỉ chặn được theo role (ADMIN/DEPT_ADMIN), không biết attribute
 * hay user cụ thể có "thuộc về" phòng ban nào - nên toàn bộ kiểm tra phạm vi
 * (scoping) theo phòng ban đều nằm ở tầng service này.
 */
@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final UserAttributeRepository userAttributeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public AttributeResponse create(AttributeRequest request, String callerUsername) {
        User caller = requireUser(callerUsername);
        if (attributeRepository.existsByAttributeName(request.getAttributeName())) {
            throw new DuplicateResourceException("Attribute đã tồn tại: " + request.getAttributeName());
        }

        Department issuerDepartment = resolveIssuerDepartmentForCreate(caller, request.getDepartmentId());

        Attribute attribute = Attribute.builder()
                .attributeName(request.getAttributeName())
                .description(request.getDescription())
                .issuerDepartment(issuerDepartment)
                .build();
        return AttributeResponse.from(attributeRepository.save(attribute));
    }

    public List<AttributeResponse> listAll(String callerUsername) {
        User caller = requireUser(callerUsername);
        List<Attribute> attributes = caller.getRole() == Role.ADMIN
                ? attributeRepository.findAll()
                : attributeRepository.findByIssuerDepartmentIsNullOrIssuerDepartmentId(requireOwnDepartment(caller).getId());
        return attributes.stream().map(AttributeResponse::from).toList();
    }

    @Transactional
    public void delete(Long attributeId, String callerUsername) {
        User caller = requireUser(callerUsername);
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy attribute id=" + attributeId));
        requireCanManageAttribute(caller, attribute);
        attributeRepository.delete(attribute);
    }

    @Transactional
    public UserAttributeResponse assign(Long userId, Long attributeId, String callerUsername) {
        User caller = requireUser(callerUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy attribute id=" + attributeId));

        requireCanManageAttribute(caller, attribute);
        requireCanManageTargetUser(caller, user);

        UserAttribute userAttribute = userAttributeRepository.findByUserAndAttribute(user, attribute)
                .map(existing -> {
                    // User từng có attribute này rồi bị revoke -> gán lại bằng cách bỏ cờ revoked,
                    // thay vì tạo bản ghi mới (sẽ vi phạm UNIQUE constraint(user_id, attribute_id)).
                    existing.setRevoked(false);
                    return existing;
                })
                .orElseGet(() -> UserAttribute.builder()
                        .user(user)
                        .attribute(attribute)
                        .build());
        // Ghi nhận ai thực hiện lần cấp/tái cấp gần nhất - kể cả khi tái cấp lại
        // 1 bản ghi cũ, vì đó vẫn là một hành động cấp quyền mới cần truy vết.
        userAttribute.setIssuedBy(caller);

        return UserAttributeResponse.from(userAttributeRepository.save(userAttribute));
    }

    @Transactional
    public void revoke(Long userAttributeId, String callerUsername) {
        User caller = requireUser(callerUsername);
        UserAttribute userAttribute = userAttributeRepository.findById(userAttributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user_attribute id=" + userAttributeId));

        requireCanManageAttribute(caller, userAttribute.getAttribute());
        requireCanManageTargetUser(caller, userAttribute.getUser());

        userAttribute.setRevoked(true);
        userAttributeRepository.save(userAttribute);
    }

    public List<UserAttributeResponse> listByUser(Long userId, String callerUsername) {
        User caller = requireUser(callerUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));

        requireCanManageTargetUser(caller, user);

        return userAttributeRepository.findByUser(user).stream()
                .map(UserAttributeResponse::from)
                .toList();
    }

    // Dùng cho GET /api/attributes/me - user tự xem attribute của chính mình
    // (khác listByUser ở trên vốn chỉ ADMIN/DEPT_ADMIN mới gọi được).
    public List<UserAttributeResponse> listMine(String username) {
        User user = requireUser(username);
        return userAttributeRepository.findByUser(user).stream()
                .map(UserAttributeResponse::from)
                .toList();
    }

    // --- Kiểm tra phạm vi phòng ban (chỉ áp dụng cho DEPT_ADMIN; ADMIN luôn full quyền) ---

    private Department resolveIssuerDepartmentForCreate(User caller, Long requestedDepartmentId) {
        if (caller.getRole() == Role.ADMIN) {
            return requestedDepartmentId == null ? null : findDepartment(requestedDepartmentId);
        }
        if (caller.getRole() == Role.DEPT_ADMIN) {
            Department own = requireOwnDepartment(caller);
            // Không tin id phòng ban do client gửi lên cho mục đích cấp quyền - luôn ép
            // về đúng phòng ban của caller, chỉ dùng để phát hiện yêu cầu bất thường.
            if (requestedDepartmentId != null && !requestedDepartmentId.equals(own.getId())) {
                throw new AccessDeniedException("DEPT_ADMIN chỉ được tạo attribute cho phòng ban của chính mình");
            }
            return own;
        }
        throw new AccessDeniedException("Bạn không có quyền tạo attribute");
    }

    private void requireCanManageAttribute(User caller, Attribute attribute) {
        if (caller.getRole() == Role.ADMIN) {
            return;
        }
        if (caller.getRole() == Role.DEPT_ADMIN) {
            Department own = requireOwnDepartment(caller);
            if (attribute.getIssuerDepartment() != null && attribute.getIssuerDepartment().getId().equals(own.getId())) {
                return;
            }
            throw new AccessDeniedException("Bạn chỉ được thao tác attribute do phòng ban của mình phát hành");
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác attribute");
    }

    private void requireCanManageTargetUser(User caller, User targetUser) {
        if (caller.getRole() == Role.ADMIN) {
            return;
        }
        if (caller.getRole() == Role.DEPT_ADMIN) {
            Department own = requireOwnDepartment(caller);
            if (targetUser.getDepartment() != null && targetUser.getDepartment().getId().equals(own.getId())) {
                return;
            }
            throw new AccessDeniedException("Bạn chỉ được thao tác với user thuộc phòng ban của mình");
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác với user này");
    }

    private Department requireOwnDepartment(User caller) {
        if (caller.getDepartment() == null) {
            throw new AccessDeniedException("Tài khoản DEPT_ADMIN chưa được gán khoa/phòng ban");
        }
        return caller.getDepartment();
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban id=" + departmentId));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));
    }
}
