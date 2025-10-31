package com.ecommerce.coupons_management.model;

import com.ecommerce.coupons_management.enums.CouponType;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String couponCode;

  @Enumerated(EnumType.STRING)
  private CouponType type;

  private Double discount;
  private Boolean isActive;
  private LocalDate expiryDate;
  private Double threshold;

  private Long productId;

  private Integer repetitionLimit;
}
