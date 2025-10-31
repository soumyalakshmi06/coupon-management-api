package com.ecommerce.coupons_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
  private Long productId;
  private int quantity;
}
