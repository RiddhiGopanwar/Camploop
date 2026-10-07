package com.camploop.repository;

import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.PurchaseRequest;
import com.camploop.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    Optional<PurchaseRequest> findByProductAndBuyer(Product product, Profile buyer);
    List<PurchaseRequest> findBySellerOrderByCreatedAtDesc(Profile seller);
    List<PurchaseRequest> findByBuyerOrderByCreatedAtDesc(Profile buyer);
    List<PurchaseRequest> findByProductAndStatus(Product product, RequestStatus status);
    boolean existsByProductAndBuyer(Product product, Profile buyer);
}
