package com.ecommerce.coupons_management.repository;

import com.ecommerce.coupons_management.model.BxGyDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BxGyDetailRepository extends JpaRepository<BxGyDetail, Long> {
  void deleteByCouponId(Long couponId);

  List<BxGyDetail> findByCouponId(Long id);
}
