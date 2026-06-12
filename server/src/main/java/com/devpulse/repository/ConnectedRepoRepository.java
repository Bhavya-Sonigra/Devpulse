package com.devpulse.repository;

import com.devpulse.model.entity.ConnectedRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectedRepoRepository extends JpaRepository<ConnectedRepo, UUID> {
    Optional<ConnectedRepo> findByWebhookId(Long webhookId);
    Optional<ConnectedRepo> findByTeamIdAndRepoFullName(UUID teamId, String repoFullName);
}