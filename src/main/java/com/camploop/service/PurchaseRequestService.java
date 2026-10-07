package com.camploop.service;

import com.camploop.exception.ApiException;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.PurchaseRequest;
import com.camploop.model.enums.ListingStatus;
import com.camploop.model.enums.NotificationType;
import com.camploop.model.enums.RequestStatus;
import com.camploop.repository.PurchaseRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository requestRepository;
    private final ProductService productService;
    private final NotificationService notificationService;

    @Autowired
    public PurchaseRequestService(PurchaseRequestRepository requestRepository,
                                  ProductService productService,
                                  NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    /** Buyer clicks "Buy / Request" — creates (or re-opens) a request and notifies the seller. */
    @Transactional
    public PurchaseRequest create(Long productId, Profile buyer) {
        Product product = productService.getById(productId);
        Profile seller = product.getSeller();

        if (seller.getId().equals(buyer.getId())) {
            throw new ApiException("You can't request your own listing", HttpStatus.BAD_REQUEST);
        }
        if (product.getStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException("This item is no longer available", HttpStatus.CONFLICT);
        }

        Optional<PurchaseRequest> existing = requestRepository.findByProductAndBuyer(product, buyer);
        PurchaseRequest request;
        if (existing.isPresent()) {
            request = existing.get();
            switch (request.getStatus()) {
                case PENDING -> throw new ApiException("You've already sent a request for this item", HttpStatus.CONFLICT);
                case DECLINED -> throw new ApiException("The seller declined your request for this item", HttpStatus.CONFLICT);
                default -> request.setStatus(RequestStatus.PENDING); // CANCELLED / CLOSED -> re-open
            }
        } else {
            request = new PurchaseRequest();
            request.setProduct(product);
            request.setBuyer(buyer);
            request.setSeller(seller);
        }
        request = requestRepository.save(request);

        notificationService.notify(seller, NotificationType.BUY_REQUEST,
                buyer.getName() + " is interested in \"" + product.getName() + "\"",
                product, request.getId());
        return request;
    }

    @Transactional
    public PurchaseRequest cancel(Long requestId, Profile buyer) {
        PurchaseRequest request = getById(requestId);
        if (!request.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException("You can only cancel your own requests", HttpStatus.FORBIDDEN);
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException("Only pending requests can be cancelled", HttpStatus.CONFLICT);
        }
        request.setStatus(RequestStatus.CANCELLED);
        return requestRepository.save(request);
    }

    @Transactional
    public PurchaseRequest decline(Long requestId, Profile seller) {
        PurchaseRequest request = getOwnedBySeller(requestId, seller);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException("Only pending requests can be declined", HttpStatus.CONFLICT);
        }
        request.setStatus(RequestStatus.DECLINED);
        requestRepository.save(request);
        notificationService.notify(request.getBuyer(), NotificationType.REQUEST_UPDATE,
                "The seller declined your request for \"" + request.getProduct().getName() + "\"",
                request.getProduct(), request.getId());
        return request;
    }

    /** Seller marks the item sold to this buyer. Everyone else's pending requests are closed. */
    @Transactional
    public PurchaseRequest markSold(Long requestId, Profile seller) {
        PurchaseRequest request = getOwnedBySeller(requestId, seller);
        Product product = request.getProduct();

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException("Only pending requests can be marked as sold", HttpStatus.CONFLICT);
        }
        if (product.getStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException("This item is already marked " + product.getStatus().name().toLowerCase(), HttpStatus.CONFLICT);
        }

        // Tell the other interested buyers before they get closed
        List<PurchaseRequest> others = requestRepository.findByProductAndStatus(product, RequestStatus.PENDING);
        for (PurchaseRequest other : others) {
            if (!other.getId().equals(request.getId())) {
                notificationService.notify(other.getBuyer(), NotificationType.REQUEST_UPDATE,
                        "\"" + product.getName() + "\" has been sold to someone else",
                        product, other.getId());
            }
        }

        // Flips product to SOLD (drops out of the marketplace) and closes all pending requests...
        productService.markStatus(product.getId(), ListingStatus.SOLD, seller);
        // ...then this buyer's request is the one that gets the SOLD status.
        request.setStatus(RequestStatus.SOLD);
        requestRepository.save(request);

        notificationService.notify(request.getBuyer(), NotificationType.REQUEST_UPDATE,
                "\"" + product.getName() + "\" was marked as sold to you — arrange the pickup in chat",
                product, request.getId());
        return request;
    }

    public List<PurchaseRequest> received(Profile seller) {
        return requestRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<PurchaseRequest> sent(Profile buyer) {
        return requestRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public Optional<PurchaseRequest> findMine(Long productId, Profile buyer) {
        Product product = productService.getById(productId);
        return requestRepository.findByProductAndBuyer(product, buyer);
    }

    public boolean hasRequest(Product product, Profile buyer) {
        return requestRepository.existsByProductAndBuyer(product, buyer);
    }

    private PurchaseRequest getById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Request not found", HttpStatus.NOT_FOUND));
    }

    private PurchaseRequest getOwnedBySeller(Long id, Profile seller) {
        PurchaseRequest request = getById(id);
        if (!request.getSeller().getId().equals(seller.getId())) {
            throw new ApiException("Only the seller can manage this request", HttpStatus.FORBIDDEN);
        }
        return request;
    }
}
