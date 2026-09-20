package com.camploop.repository;

import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(Profile user);
    Optional<Wishlist> findByUserAndProduct(Profile user, Product product);
    boolean existsByUserAndProduct(Profile user, Product product);
    void deleteByUserAndProduct(Profile user, Product product);
}
