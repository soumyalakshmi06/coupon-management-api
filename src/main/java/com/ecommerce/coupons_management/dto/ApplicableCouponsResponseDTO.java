package com.ecommerce.coupons_management.dto;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicableCouponsResponseDTO {

  private List<CouponInfo> applicable_coupons;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CouponInfo {
    private Long coupon_id;
    private String type;
    private double discount;
  }
}
