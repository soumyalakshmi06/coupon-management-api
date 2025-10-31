package com.ecommerce.coupons_management.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartRequestDTO {
  private Cart cart;
  private String couponCode;

  @Data
  public static class Cart {
    private List<CartItem> items;
  }

  @Data
  public static class CartItem {
    private Long productId;
    private int quantity;
    private double price;
  }
}
