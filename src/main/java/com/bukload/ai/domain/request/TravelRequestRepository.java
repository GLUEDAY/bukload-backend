package com.bukload.ai.domain.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelRequestRepository extends JpaRepository<TravelRequest, Long> {

    Optional<TravelRequest> findByAnchorId(String anchorId);

}

