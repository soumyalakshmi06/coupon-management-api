package com.ecommerce.coupons_management.exception;

import lombok.Getter;

@Getter
public class CouponNotFoundException extends RuntimeException {

  private final String code;

  public CouponNotFoundException(String code) {
    super("Coupon not found with couponCode: " + code);
    this.code = code;
  }
}
