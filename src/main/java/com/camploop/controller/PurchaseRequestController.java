package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.dto.PurchaseRequestResponse;
import com.camploop.model.Profile;
import com.camploop.service.ProfileService;
import com.camploop.service.PurchaseRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
public class PurchaseRequestController {

    private final PurchaseRequestService requestService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public PurchaseRequestController(PurchaseRequestService requestService, ProfileService profileService,
                                     AuthContext authContext) {
        this.requestService = requestService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    /** Buyer: "Buy / Request" on a listing. */
    @PostMapping("/{productId}")
    public ResponseEntity<PurchaseRequestResponse> create(@PathVariable Long productId, HttpServletRequest http) {
        Profile buyer = me(http);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PurchaseRequestResponse(requestService.create(productId, buyer)));
    }

    /** Seller inbox: requests on my listings. */
    @GetMapping("/received")
    public ResponseEntity<List<PurchaseRequestResponse>> received(HttpServletRequest http) {
        return ResponseEntity.ok(toResponses(requestService.received(me(http))));
    }

    /** Buyer: requests I've sent. */
    @GetMapping("/sent")
    public ResponseEntity<List<PurchaseRequestResponse>> sent(HttpServletRequest http) {
        return ResponseEntity.ok(toResponses(requestService.sent(me(http))));
    }

    /** Buyer: my request (if any) for one product — lets the product page show "Request sent". */
    @GetMapping("/product/{productId}/mine")
    public ResponseEntity<PurchaseRequestResponse> mineForProduct(@PathVariable Long productId, HttpServletRequest http) {
        return requestService.findMine(productId, me(http))
                .map(r -> ResponseEntity.ok(new PurchaseRequestResponse(r)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/mark-sold")
    public ResponseEntity<PurchaseRequestResponse> markSold(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(new PurchaseRequestResponse(requestService.markSold(id, me(http))));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<PurchaseRequestResponse> decline(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(new PurchaseRequestResponse(requestService.decline(id, me(http))));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseRequestResponse> cancel(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(new PurchaseRequestResponse(requestService.cancel(id, me(http))));
    }

    private Profile me(HttpServletRequest http) {
        return profileService.getOrCreate(authContext.require(http), null);
    }

    private List<PurchaseRequestResponse> toResponses(List<com.camploop.model.PurchaseRequest> list) {
        return list.stream().map(PurchaseRequestResponse::new).collect(Collectors.toList());
    }
}
