package org.likelionhsu.hackathon.preference.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository
        extends JpaRepository<PreferenceProfile, Long> {

    Optional<PreferenceProfile> findByUser_Id(Long userId);
}