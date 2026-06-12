package com.devpulse.repository;

import com.devpulse.model.entity.SlackConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlackConnectionRepository extends JpaRepository<SlackConnection, UUID> {
    Optional<SlackConnection> findByTeamId(UUID teamId);
}