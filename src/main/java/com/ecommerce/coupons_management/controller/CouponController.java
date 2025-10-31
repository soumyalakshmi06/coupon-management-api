package com.ecommerce.coupons_management.controller;

import com.ecommerce.coupons_management.dto.*;
import com.ecommerce.coupons_management.model.Coupon;
import com.ecommerce.coupons_management.service.CouponService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

  private final CouponService couponService;

  @PostMapping
  public ResponseEntity<Map<String, Object>> createCoupon(@RequestBody CouponRequestDTO request) {
    Coupon coupon = couponService.addCoupon(request);
    Map<String, Object> response = new HashMap<>();
    response.put("coupon_id", coupon.getId());
    response.put("coupon_code", coupon.getCouponCode());
    response.put("type", coupon.getType());
    response.put("discount", coupon.getDiscount());
    response.put("threshold", coupon.getThreshold());
    response.put("expiry_date", coupon.getExpiryDate());
    response.put("is_active", coupon.getIsActive());
    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<Page<Coupon>> getAllCouponsPaginated(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "asc") String direction) {

    Page<Coupon> coupons = couponService.getAllCoupons(page, size, sortBy, direction);
    return ResponseEntity.ok(coupons);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Coupon> getCouponById(@PathVariable Long id) {
    Coupon coupon = couponService.getCouponById(id);
    return ResponseEntity.ok(coupon);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Coupon> updateCoupon(
      @PathVariable Long id, @RequestBody CouponRequestDTO request) {

    Coupon existing = couponService.getCouponById(id);
    existing.setType(request.getType());
    if (request.getDetails() != null) {
      existing.setDiscount(request.getDetails().getDiscount());
      existing.setThreshold(request.getDetails().getThreshold());
    }
    return ResponseEntity.ok(couponService.save(existing));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
    couponService.deleteCoupon(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/applicable-coupons")
  public ResponseEntity<Map<String, Object>> getApplicableCoupons(
      @RequestBody CartRequestDTO cartRequest) {
    List<Coupon> applicableCoupons = couponService.getApplicableCoupons(cartRequest);

    List<Map<String, Object>> coupons =
        applicableCoupons.stream()
            .map(
                coupon -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put("coupon_id", coupon.getId());
                  map.put("type", coupon.getType().toString().toLowerCase());
                  map.put("discount", couponService.getApplicableCoupons(coupon, cartRequest));
                  return map;
                })
            .toList();

    Map<String, Object> response = new HashMap<>();
    response.put("applicable_coupons", coupons);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/apply-coupon/{id}")
  public ResponseEntity<Map<String, Object>> applyCouponToCart(
      @PathVariable Long id, @RequestBody CartRequestDTO cartRequest) {

    CartResponseDTO responseDTO = couponService.applyCouponToCart(id, cartRequest);

    Map<String, Object> response = new HashMap<>();
    response.put("updated_cart", responseDTO.getUpdatedCart());

    return ResponseEntity.ok(response);
  }
}
