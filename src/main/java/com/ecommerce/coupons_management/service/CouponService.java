package com.ecommerce.coupons_management.service;

import com.ecommerce.coupons_management.dto.*;
import com.ecommerce.coupons_management.enums.CouponType;
import com.ecommerce.coupons_management.exception.CouponNotFoundException;
import com.ecommerce.coupons_management.model.*;
import com.ecommerce.coupons_management.repository.*;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository couponRepository;
  private final BxGyDetailRepository bxGyDetailRepository;
  private final ProductRepository productRepository;

  public Coupon addCoupon(CouponRequestDTO request) {
    Coupon coupon = new Coupon();
    CouponType type = request.getType();

    coupon.setType(type);
    coupon.setCouponCode("COUPON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
    coupon.setExpiryDate(
        request.getExpiryDate() != null ? request.getExpiryDate() : LocalDate.now().plusMonths(1));

    if (request.getDetails() != null) {
      switch (type) {
        case CART_WISE:
          coupon.setThreshold(request.getDetails().getThreshold());
          coupon.setDiscount(request.getDetails().getDiscount());
          break;

        case PRODUCT_WISE:
          if (request.getDetails().getProductId() == null) {
            throw new IllegalArgumentException("Product ID required for PRODUCT_WISE coupon");
          }
          coupon.setProductId(Long.valueOf(request.getDetails().getProductId()));
          coupon.setDiscount(request.getDetails().getDiscount());
          break;

        case BXGY:
          coupon.setRepetitionLimit(request.getDetails().getRepetitionLimit());
          break;

        default:
          throw new IllegalArgumentException("Unsupported coupon type: " + type);
      }
    }

    coupon = couponRepository.save(coupon);

    // Save BXGY details if present
    if (type == CouponType.BXGY && request.getDetails() != null) {
      var details = request.getDetails();
      if (details.getBuyProducts() != null && details.getGetProducts() != null) {
        for (var buyProduct : details.getBuyProducts()) {
          for (var getProduct : details.getGetProducts()) {
            BxGyDetail bxgy =
                BxGyDetail.builder()
                    .coupon(coupon)
                    .buyProductId(buyProduct.getProductId())
                    .buyQuantity(buyProduct.getQuantity())
                    .getProductId(getProduct.getProductId())
                    .getQuantity(getProduct.getQuantity())
                    .repetitionLimit(details.getRepetitionLimit())
                    .build();
            bxGyDetailRepository.save(bxgy);
          }
        }
      }
    }

    return coupon;
  }

  public CartResponseDTO applyCoupon(CartRequestDTO cartRequest) {
    List<CartRequestDTO.CartItem> cartItems = cartRequest.getCart().getItems();

    double totalPrice = 0.0;
    List<CartResponseDTO.ItemResponse> itemResponses = new ArrayList<>();

    for (CartRequestDTO.CartItem item : cartItems) {
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

      double itemTotal = product.getPrice() * item.getQuantity();
      totalPrice += itemTotal;

      itemResponses.add(
          new CartResponseDTO.ItemResponse(
              item.getProductId(), item.getQuantity(), product.getPrice(), 0.0));
    }

    Coupon coupon =
        couponRepository
            .findByCouponCode(cartRequest.getCouponCode())
            .orElseThrow(() -> new CouponNotFoundException("Invalid coupon code"));

    if (!coupon.getIsActive() || coupon.getExpiryDate().isBefore(LocalDate.now())) {
      throw new CouponNotFoundException("Coupon expired or inactive");
    }

    double totalDiscount = 0.0;
    String message;

    switch (coupon.getType()) {
      case CART_WISE -> {
        totalDiscount = totalPrice * (coupon.getDiscount() / 100);
        message = "Cart-wise discount applied!";
      }

      case PRODUCT_WISE -> {
        for (CartResponseDTO.ItemResponse item : itemResponses) {
          double discount =
              getProductDiscount(
                  item.getProductId(), item.getQuantity(), item.getPrice(), coupon.getId());
          item.setTotalDiscount(discount);
          totalDiscount += discount;
        }
        message = "Product-wise discount applied!";
      }

      case BXGY -> {
        List<BxGyDetail> bxgyDetails = bxGyDetailRepository.findByCouponId(coupon.getId());
        for (BxGyDetail detail : bxgyDetails) {
          int buyQuantity = 0;
          int getQuantity = 0;
          double getProductPrice = 0;

          for (CartRequestDTO.CartItem item : cartItems) {
            if (item.getProductId().equals(detail.getBuyProductId())) {
              buyQuantity = item.getQuantity();
            }
            if (item.getProductId().equals(detail.getGetProductId())) {
              getQuantity = item.getQuantity();
              Product getProduct =
                  productRepository
                      .findById(item.getProductId())
                      .orElseThrow(
                          () -> new RuntimeException("Product not found: " + item.getProductId()));
              getProductPrice = getProduct.getPrice();
            }
          }

          int applicableFreeQty = (buyQuantity / detail.getBuyQuantity()) * detail.getGetQuantity();
          double discount = applicableFreeQty * getProductPrice;
          totalDiscount += discount;

          for (CartResponseDTO.ItemResponse item : itemResponses) {
            if (item.getProductId().equals(detail.getGetProductId())) {
              item.setQuantity(item.getQuantity() + applicableFreeQty);
              item.setTotalDiscount(discount);
            }
          }
        }
        message = "Buy X Get Y discount applied!";
      }

      default -> message = "No discount applied!";
    }

    double finalPrice = totalPrice - totalDiscount;

    CartResponseDTO.UpdatedCart updatedCart =
        new CartResponseDTO.UpdatedCart(itemResponses, totalPrice, totalDiscount, finalPrice);

    return new CartResponseDTO(updatedCart, message);
  }

  public double getProductDiscount(Long productId, int quantity, double price, Long couponId) {
    Optional<Coupon> optionalCoupon = couponRepository.findById(couponId);
    if (optionalCoupon.isEmpty()) return 0;

    Coupon coupon = optionalCoupon.get();
    double discount = 0;

    if (coupon.getType() == CouponType.PRODUCT_WISE && productId.equals(coupon.getProductId())) {
      discount = (price * quantity * coupon.getDiscount()) / 100.0;
    }

    return discount;
  }

  public CartResponseDTO applyCouponToCart(Long id, CartRequestDTO cartRequest) {
    Coupon coupon =
        couponRepository
            .findById(id)
            .orElseThrow(() -> new CouponNotFoundException("Coupon not found with ID: " + id));

    cartRequest.setCouponCode(coupon.getCouponCode());
    return applyCoupon(cartRequest);
  }

  public Page<Coupon> getAllCoupons(int page, int size, String sortBy, String direction) {
    Sort sort =
        direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);
    return couponRepository.findAll(pageable);
  }

  public Coupon getCouponById(Long id) {
    return couponRepository
        .findById(id)
        .orElseThrow(() -> new CouponNotFoundException("Coupon not found with ID: " + id));
  }

  public Coupon save(Coupon coupon) {
    return couponRepository.save(coupon);
  }

  @Transactional
  public void deleteCoupon(Long id) {
    if (!couponRepository.existsById(id)) {
      throw new CouponNotFoundException("Coupon not found with ID: " + id);
    }
    bxGyDetailRepository.deleteByCouponId(id);
    couponRepository.deleteById(id);
  }

  public List<Coupon> getApplicableCoupons(CartRequestDTO cartRequest) {
    List<CartRequestDTO.CartItem> cartItems = cartRequest.getCart().getItems();

    if (cartItems == null || cartItems.isEmpty()) {
      throw new IllegalArgumentException("Cart items cannot be empty");
    }

    List<Coupon> allCoupons = couponRepository.findAll();
    LocalDate today = LocalDate.now();

    List<Coupon> applicableCoupons = new ArrayList<>();

    double totalPrice = 0.0;
    for (CartRequestDTO.CartItem item : cartItems) {
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
      totalPrice += product.getPrice() * item.getQuantity();
    }

    for (Coupon coupon : allCoupons) {
      if (!Boolean.TRUE.equals(coupon.getIsActive())) continue;
      if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(today)) continue;

      switch (coupon.getType()) {
        case CART_WISE -> {
          if (coupon.getThreshold() == null || totalPrice >= coupon.getThreshold()) {
            applicableCoupons.add(coupon);
          }
        }

        case PRODUCT_WISE -> {
          boolean productExists =
              cartItems.stream()
                  .anyMatch(item -> item.getProductId().equals(coupon.getProductId()));
          if (productExists) applicableCoupons.add(coupon);
        }

        case BXGY -> {
          List<BxGyDetail> bxgyDetails = bxGyDetailRepository.findByCouponId(coupon.getId());
          boolean applicable =
              bxgyDetails.stream()
                  .anyMatch(
                      detail ->
                          cartItems.stream()
                              .anyMatch(
                                  item ->
                                      item.getProductId().equals(detail.getBuyProductId())
                                          && item.getQuantity() >= detail.getBuyQuantity()));
          if (applicable) applicableCoupons.add(coupon);
        }

        default -> {}
      }
    }

    return applicableCoupons;
  }

  public double getApplicableCoupons(Coupon coupon, CartRequestDTO cartRequest) {
    double totalCartValue =
        cartRequest.getCart().getItems().stream()
            .mapToDouble(
                item -> {
                  Product product =
                      productRepository
                          .findById(item.getProductId())
                          .orElseThrow(
                              () ->
                                  new RuntimeException(
                                      "Product not found: " + item.getProductId()));
                  return product.getPrice() * item.getQuantity();
                })
            .sum();

    double discount = 0.0;

    switch (coupon.getType()) {
      case CART_WISE -> {
        if (coupon.getThreshold() == null || totalCartValue >= coupon.getThreshold()) {
          discount = totalCartValue * (coupon.getDiscount() / 100);
        }
      }

      case PRODUCT_WISE -> {
        for (CartRequestDTO.CartItem item : cartRequest.getCart().getItems()) {
          if (item.getProductId().equals(coupon.getProductId())) {
            Product product =
                productRepository
                    .findById(item.getProductId())
                    .orElseThrow(
                        () -> new RuntimeException("Product not found: " + item.getProductId()));
            discount += (product.getPrice() * item.getQuantity()) * (coupon.getDiscount() / 100);
          }
        }
      }

      case BXGY -> {
        List<BxGyDetail> bxgyDetails = bxGyDetailRepository.findByCouponId(coupon.getId());
        for (BxGyDetail detail : bxgyDetails) {
          int buyQty = 0;
          double getProductPrice = 0;

          for (CartRequestDTO.CartItem item : cartRequest.getCart().getItems()) {
            if (item.getProductId().equals(detail.getBuyProductId())) {
              buyQty = item.getQuantity();
            }
            if (item.getProductId().equals(detail.getGetProductId())) {
              Product getProduct =
                  productRepository
                      .findById(item.getProductId())
                      .orElseThrow(
                          () -> new RuntimeException("Product not found: " + item.getProductId()));
              getProductPrice = getProduct.getPrice();
            }
          }

          int applicableFreeQty = (buyQty / detail.getBuyQuantity()) * detail.getGetQuantity();
          discount += applicableFreeQty * getProductPrice;
        }
      }
    }

    return discount;
  }
}
