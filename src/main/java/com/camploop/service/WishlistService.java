package com.camploop.service;

import com.camploop.exception.ApiException;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.Wishlist;
import com.camploop.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;

    @Autowired
    public WishlistService(WishlistRepository wishlistRepository, ProductService productService) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
    }

    public Wishlist add(Long productId, Profile user) {
        Product product = productService.getById(productId);

        if (product.getSeller().getId().equals(user.getId())) {
            throw new ApiException("You can't wishlist your own listing", HttpStatus.BAD_REQUEST);
        }
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new ApiException("Already in your wishlist", HttpStatus.CONFLICT);
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        return wishlistRepository.save(wishlist);
    }

    public void remove(Long productId, Profile user) {
        Product product = productService.getById(productId);
        wishlistRepository.deleteByUserAndProduct(user, product);
    }

    public List<Wishlist> getByUser(Profile user) {
        return wishlistRepository.findByUser(user);
    }
}
