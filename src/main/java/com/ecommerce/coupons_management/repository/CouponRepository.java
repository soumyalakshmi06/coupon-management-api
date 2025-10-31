package com.ecommerce.coupons_management.repository;

import com.ecommerce.coupons_management.model.Coupon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
  Optional<Coupon> findByCouponCode(String couponCode);
}
