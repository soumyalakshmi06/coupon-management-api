package com.ecommerce.coupons_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bxgy_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BxGyDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long buyProductId;
  private Integer buyQuantity;

  private Long getProductId;
  private Integer getQuantity;

  private Integer repetitionLimit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;
}
