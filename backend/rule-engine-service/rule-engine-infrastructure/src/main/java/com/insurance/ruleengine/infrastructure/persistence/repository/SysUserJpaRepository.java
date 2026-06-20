package com.insurance.ruleengine.infrastructure.persistence.repository;

import com.insurance.ruleengine.infrastructure.persistence.entity.SysUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysUserJpaRepository extends JpaRepository<SysUserEntity, Long> {

    Optional<SysUserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
