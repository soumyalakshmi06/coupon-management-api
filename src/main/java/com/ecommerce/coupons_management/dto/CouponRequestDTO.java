package com.ecommerce.coupons_management.dto;

import com.ecommerce.coupons_management.enums.CouponType;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class CouponRequestDTO {
  private CouponType type;
  private Details details;
  private Boolean isActive;
  private LocalDate expiryDate;

  @Data
  public static class Details {
    private Double threshold;
    private Double discount;
    private String productId;
    private List<ProductQuantity> buyProducts;
    private List<ProductQuantity> getProducts;
    private Integer repetitionLimit;
  }

  @Data
  public static class ProductQuantity {
    private Long productId;
    private Integer quantity;
  }
}
