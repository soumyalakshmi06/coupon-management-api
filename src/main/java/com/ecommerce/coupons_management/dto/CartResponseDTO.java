package com.ecommerce.coupons_management.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {

  private UpdatedCart updatedCart;
  private String message;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdatedCart {
    private List<ItemResponse> items;
    private double totalPrice;
    private double totalDiscount;
    private double finalPrice;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ItemResponse {
    private Long productId;
    private int quantity;
    private double price;
    private double totalDiscount;
  }
}
