package com.samterminal.backend.repository;

import com.samterminal.backend.entity.Emotion;
import com.samterminal.backend.entity.FireflyAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FireflyAssetRepository extends JpaRepository<FireflyAsset, Long> {
    Optional<FireflyAsset> findByEmotion(Emotion emotion);
}
