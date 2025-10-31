package com.ecommerce.coupons_management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.coupons_management.dto.*;
import com.ecommerce.coupons_management.enums.CouponType;
import com.ecommerce.coupons_management.model.Coupon;
import com.ecommerce.coupons_management.service.CouponService;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

  @Mock private CouponService couponService;

  @InjectMocks private CouponController couponController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  private Coupon coupon;
  private CouponRequestDTO couponRequest;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    coupon = new Coupon();
    coupon.setId(1L);
    coupon.setCouponCode("SAVE10");
    coupon.setType(CouponType.CART_WISE);
    coupon.setDiscount(10.0);
    coupon.setThreshold(100.0);
    coupon.setIsActive(true);
    coupon.setExpiryDate(LocalDate.now().plusDays(10));

    couponRequest = new CouponRequestDTO();
    couponRequest.setType(CouponType.CART_WISE);
    couponRequest.setIsActive(true);
    couponRequest.setExpiryDate(LocalDate.now().plusDays(10));

    CouponRequestDTO.Details details = new CouponRequestDTO.Details();
    details.setDiscount(10.0);
    details.setThreshold(100.0);
    couponRequest.setDetails(details);
  }

  @Test
  void testCreateCoupon() throws Exception {
    Mockito.when(couponService.addCoupon(Mockito.any())).thenReturn(coupon);

    mockMvc
        .perform(
            post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(couponRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coupon_code").value("SAVE10"));
  }

  @Test
  void testGetAllCoupons() throws Exception {
    Mockito.when(
            couponService.getAllCoupons(
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new PageImpl<>(List.of(coupon), PageRequest.of(0, 5), 1));

    mockMvc
        .perform(get("/api/coupons?page=0&size=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].couponCode").value("SAVE10"));
  }

  @Test
  void testGetCouponById() throws Exception {
    Mockito.when(couponService.getCouponById(1L)).thenReturn(coupon);

    mockMvc
        .perform(get("/api/coupons/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.couponCode").value("SAVE10"));
  }

  @Test
  void testUpdateCoupon() throws Exception {
    Mockito.when(couponService.getCouponById(1L)).thenReturn(coupon);
    Mockito.when(couponService.save(Mockito.any())).thenReturn(coupon);

    mockMvc
        .perform(
            put("/api/coupons/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(couponRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.couponCode").value("SAVE10"));
  }

  @Test
  void testDeleteCoupon() throws Exception {
    mockMvc.perform(delete("/api/coupons/1")).andExpect(status().isNoContent());
    Mockito.verify(couponService).deleteCoupon(1L);
  }

  @Test
  void testGetApplicableCoupons() throws Exception {
    CartRequestDTO cartRequest = new CartRequestDTO();

    Mockito.when(couponService.getApplicableCoupons(Mockito.any(CartRequestDTO.class)))
        .thenReturn(List.of(coupon));
    Mockito.when(
            couponService.getApplicableCoupons(
                Mockito.any(Coupon.class), Mockito.any(CartRequestDTO.class)))
        .thenReturn(10.0);

    mockMvc
        .perform(
            post("/api/coupons/applicable-coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicable_coupons[0].discount").value(10.0));
  }

  @Test
  void testApplyCouponToCart() throws Exception {
    CartRequestDTO cartRequest = new CartRequestDTO();

    CartResponseDTO.UpdatedCart updatedCart = new CartResponseDTO.UpdatedCart();
    updatedCart.setTotalPrice(100.0);
    updatedCart.setTotalDiscount(10.0);
    updatedCart.setFinalPrice(90.0);

    CartResponseDTO responseDTO = new CartResponseDTO();
    responseDTO.setUpdatedCart(updatedCart);
    responseDTO.setMessage("Coupon applied successfully");

    Mockito.when(couponService.applyCouponToCart(Mockito.eq(1L), Mockito.any(CartRequestDTO.class)))
        .thenReturn(responseDTO);

    mockMvc
        .perform(
            post("/api/coupons/apply-coupon/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated_cart.totalPrice").value(100.0))
        .andExpect(jsonPath("$.updated_cart.totalDiscount").value(10.0))
        .andExpect(jsonPath("$.updated_cart.finalPrice").value(90.0))
        .andExpect(jsonPath("$.updated_cart.items").doesNotExist())
        .andExpect(jsonPath("$.message").doesNotExist());
  }
}
