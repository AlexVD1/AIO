package com.kidsanim.api.repository;

import com.kidsanim.api.domain.StyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StyleProfileRepository extends JpaRepository<StyleProfile, UUID> {
}
