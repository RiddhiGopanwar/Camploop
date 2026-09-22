package com.camploop.repository;

import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySeller(Profile seller);

    List<Product> findBySellerAndStatus(Profile seller, ListingStatus status);

    List<Product> findTop8ByStatusOrderByCreatedAtDesc(ListingStatus status);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (
                    :keyword IS NULL
                    OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                    OR LOWER(p.description) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                  )
              AND (:category IS NULL OR p.category = :category)
              AND (:condition IS NULL OR p.condition = :condition)
              AND (:minPrice IS NULL OR p.sellingPrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.sellingPrice <= :maxPrice)
            """)
    List<Product> search(
            @Param("status") ListingStatus status,
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("condition") Condition condition,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
