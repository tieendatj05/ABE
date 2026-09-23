package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho Department (khoa/phòng ban).
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);

    boolean existsByCode(String code);
}
