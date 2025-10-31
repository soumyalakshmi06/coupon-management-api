package com.ecommerce.coupons_management.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ecommerce.coupons_management.dto.*;
import com.ecommerce.coupons_management.enums.CouponType;
import com.ecommerce.coupons_management.exception.CouponNotFoundException;
import com.ecommerce.coupons_management.model.*;
import com.ecommerce.coupons_management.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

  @Mock private CouponRepository couponRepository;
  @Mock private BxGyDetailRepository bxGyDetailRepository;
  @Mock private ProductRepository productRepository;
  @InjectMocks private CouponService couponService;

  private Coupon coupon;
  private Product product;

  @BeforeEach
  void setUp() {
    coupon =
        Coupon.builder()
            .id(1L)
            .couponCode("COUPON-TEST")
            .type(CouponType.CART_WISE)
            .discount(10.0)
            .isActive(true)
            .expiryDate(LocalDate.now().plusDays(10))
            .threshold(100.0)
            .build();

    product = Product.builder().id(1L).name("Test Product").price(100.0).build();
  }

  @Test
  void testAddCartWiseCouponSuccess() {
    CouponRequestDTO request = new CouponRequestDTO();
    request.setType(CouponType.CART_WISE);
    CouponRequestDTO.Details details = new CouponRequestDTO.Details();
    details.setThreshold(200.0);
    details.setDiscount(10.0);
    request.setDetails(details);

    when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

    Coupon saved = couponService.addCoupon(request);

    assertThat(saved).isNotNull();
    assertThat(saved.getType()).isEqualTo(CouponType.CART_WISE);
    verify(couponRepository).save(any(Coupon.class));
  }

  @Test
  void testAddProductWiseCouponSuccess() {
    CouponRequestDTO request = new CouponRequestDTO();
    request.setType(CouponType.PRODUCT_WISE);
    CouponRequestDTO.Details details = new CouponRequestDTO.Details();
    details.setProductId("1");
    details.setDiscount(15.0);
    request.setDetails(details);

    when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

    Coupon saved = couponService.addCoupon(request);
    assertThat(saved.getProductId()).isEqualTo(1L);
    assertThat(saved.getDiscount()).isEqualTo(15.0);
  }

  @Test
  void testAddProductWiseCouponMissingProductId() {
    CouponRequestDTO request = new CouponRequestDTO();
    request.setType(CouponType.PRODUCT_WISE);
    request.setDetails(new CouponRequestDTO.Details());
    assertThrows(IllegalArgumentException.class, () -> couponService.addCoupon(request));
  }

  @Test
  void testAddBxGyCouponSuccess() {
    CouponRequestDTO request = new CouponRequestDTO();
    request.setType(CouponType.BXGY);
    CouponRequestDTO.Details details = new CouponRequestDTO.Details();
    details.setRepetitionLimit(2);

    CouponRequestDTO.ProductQuantity buy = new CouponRequestDTO.ProductQuantity();
    buy.setProductId(1L);
    buy.setQuantity(2);
    CouponRequestDTO.ProductQuantity get = new CouponRequestDTO.ProductQuantity();
    get.setProductId(2L);
    get.setQuantity(1);

    details.setBuyProducts(List.of(buy));
    details.setGetProducts(List.of(get));
    request.setDetails(details);

    when(couponRepository.save(any()))
        .thenAnswer(
            i -> {
              Coupon c = i.getArgument(0);
              c.setId(10L);
              return c;
            });

    couponService.addCoupon(request);

    verify(bxGyDetailRepository, times(1)).save(any(BxGyDetail.class));
  }

  private CartRequestDTO prepareCartRequest(String code) {
    CartRequestDTO.CartItem item = new CartRequestDTO.CartItem();
    item.setProductId(1L);
    item.setQuantity(2);
    CartRequestDTO.Cart cart = new CartRequestDTO.Cart();
    cart.setItems(List.of(item));

    CartRequestDTO request = new CartRequestDTO();
    request.setCouponCode(code);
    request.setCart(cart);
    return request;
  }

  @Test
  void testApplyCartWiseCouponSuccess() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(couponRepository.findByCouponCode("COUPON-TEST")).thenReturn(Optional.of(coupon));

    CartResponseDTO response = couponService.applyCoupon(prepareCartRequest("COUPON-TEST"));
    assertThat(response.getUpdatedCart().getTotalDiscount()).isPositive();
    assertThat(response.getMessage()).contains("Cart-wise");
  }

  @Test
  void testApplyProductWiseCouponSuccess() {
    Coupon coupon =
        Coupon.builder()
            .id(1L)
            .couponCode("COUPON-TEST")
            .type(CouponType.CART_WISE)
            .discount(10.0)
            .isActive(true)
            .expiryDate(LocalDate.now().plusDays(10))
            .threshold(50.0)
            .build();

    Product product = Product.builder().id(1L).name("Test Product").price(100.0).build();

    when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
    when(couponRepository.findByCouponCode("COUPON-TEST")).thenReturn(Optional.of(coupon));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    CartRequestDTO.CartItem item = new CartRequestDTO.CartItem();
    item.setProductId(1L);
    item.setQuantity(2);

    CartRequestDTO.Cart cart = new CartRequestDTO.Cart();
    cart.setItems(List.of(item));

    CartRequestDTO request = new CartRequestDTO();
    request.setCart(cart);

    CartResponseDTO response = couponService.applyCouponToCart(1L, request);

    assertThat(response).isNotNull();
    assertThat(response.getUpdatedCart()).isNotNull();
    assertThat(response.getUpdatedCart().getTotalDiscount()).isPositive();
    assertThat(response.getUpdatedCart().getFinalPrice())
        .isLessThan(response.getUpdatedCart().getTotalPrice());
  }

  @Test
  void testApplyBxGyCouponSuccess() {
    coupon.setType(CouponType.BXGY);
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
    when(couponRepository.findByCouponCode(anyString())).thenReturn(Optional.of(coupon));

    BxGyDetail bxgy =
        BxGyDetail.builder()
            .id(1L)
            .buyProductId(1L)
            .buyQuantity(2)
            .getProductId(1L)
            .getQuantity(1)
            .coupon(coupon)
            .build();

    when(bxGyDetailRepository.findByCouponId(anyLong())).thenReturn(List.of(bxgy));

    CartResponseDTO response = couponService.applyCoupon(prepareCartRequest("COUPON-BXGY"));
    assertThat(response.getMessage()).contains("Buy X Get Y");
  }

  @Test
  void testApplyCouponExpiredOrInactive() {
    coupon.setIsActive(false);
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
    when(couponRepository.findByCouponCode(anyString())).thenReturn(Optional.of(coupon));

    assertThrows(
        CouponNotFoundException.class,
        () -> couponService.applyCoupon(prepareCartRequest("COUPON-TEST")));
  }

  @Test
  void testApplyCouponInvalidCode() {
    when(couponRepository.findByCouponCode(anyString())).thenReturn(Optional.empty());
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
    assertThrows(
        CouponNotFoundException.class,
        () -> couponService.applyCoupon(prepareCartRequest("INVALID")));
  }

  @Test
  void testGetProductDiscountProductWise() {
    coupon.setType(CouponType.PRODUCT_WISE);
    coupon.setProductId(1L);
    when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

    double discount = couponService.getProductDiscount(1L, 2, 100.0, 1L);
    assertThat(discount).isEqualTo(20.0);
  }

  @Test
  void testGetProductDiscountInvalidCoupon() {
    when(couponRepository.findById(1L)).thenReturn(Optional.empty());
    assertThat(couponService.getProductDiscount(1L, 2, 100.0, 1L)).isZero();
  }

  @Test
  void testApplyCouponToCart() {

    Coupon coupon =
        Coupon.builder()
            .id(1L)
            .couponCode("SAVE10")
            .type(CouponType.CART_WISE)
            .discount(10.0)
            .threshold(100.0)
            .isActive(true)
            .expiryDate(LocalDate.now().plusDays(5))
            .build();

    Product product = Product.builder().id(1L).name("Product A").price(120.0).build();

    when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
    when(couponRepository.findByCouponCode("SAVE10")).thenReturn(Optional.of(coupon));
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));

    CartRequestDTO request = prepareCartRequest(null);

    CartResponseDTO response = couponService.applyCouponToCart(1L, request);

    assertThat(response.getUpdatedCart().getTotalDiscount()).isPositive();
    assertThat(response.getUpdatedCart().getFinalPrice())
        .isLessThan(response.getUpdatedCart().getTotalPrice());
  }

  @Test
  void testGetAllCoupons() {
    Page<Coupon> page = new PageImpl<>(List.of(coupon));
    when(couponRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Coupon> result = couponService.getAllCoupons(0, 10, "id", "asc");
    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  void testGetCouponByIdFound() {
    when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
    assertThat(couponService.getCouponById(1L)).isEqualTo(coupon);
  }

  @Test
  void testGetCouponByIdNotFound() {
    when(couponRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(CouponNotFoundException.class, () -> couponService.getCouponById(1L));
  }

  @Test
  void testDeleteCouponSuccess() {
    when(couponRepository.existsById(1L)).thenReturn(true);
    doNothing().when(bxGyDetailRepository).deleteByCouponId(1L);
    couponService.deleteCoupon(1L);
    verify(couponRepository).deleteById(1L);
  }

  @Test
  void testDeleteCouponNotFound() {
    when(couponRepository.existsById(1L)).thenReturn(false);
    assertThrows(CouponNotFoundException.class, () -> couponService.deleteCoupon(1L));
  }

  @Test
  void testGetApplicableCoupons() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(couponRepository.findAll()).thenReturn(List.of(coupon));

    CartRequestDTO req = prepareCartRequest(null);
    List<Coupon> applicable = couponService.getApplicableCoupons(req);
    assertThat(applicable).isNotEmpty();
  }

  @Test
  void testGetApplicableCouponsCartWiseThresholdNotMet() {
    coupon.setThreshold(5000.0);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(couponRepository.findAll()).thenReturn(List.of(coupon));

    List<Coupon> applicable = couponService.getApplicableCoupons(prepareCartRequest(null));
    assertThat(applicable).isEmpty();
  }

  @Test
  void testGetApplicableCoupons_BXGYLogic() {
    coupon.setType(CouponType.BXGY);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(couponRepository.findAll()).thenReturn(List.of(coupon));

    BxGyDetail bxgy =
        BxGyDetail.builder()
            .buyProductId(1L)
            .buyQuantity(2)
            .getProductId(2L)
            .getQuantity(1)
            .coupon(coupon)
            .build();
    when(bxGyDetailRepository.findByCouponId(anyLong())).thenReturn(List.of(bxgy));

    List<Coupon> applicable = couponService.getApplicableCoupons(prepareCartRequest(null));
    assertThat(applicable).isNotEmpty();
  }

  @Test
  void testGetApplicableCouponsForSpecificCoupon() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    double discount = couponService.getApplicableCoupons(coupon, prepareCartRequest(null));
    assertThat(discount).isGreaterThan(0);
  }

  @Test
  void testGetApplicableCouponsForSpecificCoupon_BXGY() {
    coupon.setType(CouponType.BXGY);
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
    when(bxGyDetailRepository.findByCouponId(anyLong()))
        .thenReturn(
            List.of(
                BxGyDetail.builder()
                    .buyProductId(1L)
                    .buyQuantity(2)
                    .getProductId(1L)
                    .getQuantity(1)
                    .coupon(coupon)
                    .build()));

    double discount = couponService.getApplicableCoupons(coupon, prepareCartRequest(null));
    assertThat(discount).isGreaterThan(0);
  }
}
