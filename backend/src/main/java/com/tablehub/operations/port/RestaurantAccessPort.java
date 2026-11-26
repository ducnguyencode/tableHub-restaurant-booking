package com.tablehub.operations.port;

/**
 * Port giao tiếp với module Account/Restaurant (K1 & K2) để kiểm tra quyền và trạng thái nhà hàng.
 * Khi K1 đẩy khung Security (@RequireRestaurantRole), chỉ cần implement interface này.
 */
public interface RestaurantAccessPort {

    boolean isRestaurantActive(Long restaurantId);

    boolean isUserOwner(Long restaurantId, Long userId);

    boolean isUserStaffOrOwner(Long restaurantId, Long userId);

    Long getCurrentUserId();
}
