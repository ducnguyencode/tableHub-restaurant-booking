package com.tablehub.operations.port;

/**
 * Port for communicating with the Account/Restaurant module (K1 & K2)
 * to verify restaurant status and member permissions.
 */
public interface RestaurantAccessPort {

    boolean isRestaurantActive(Long restaurantId);

    boolean isUserOwner(Long restaurantId, Long userId);

    boolean isUserStaffOrOwner(Long restaurantId, Long userId);

    Long getCurrentUserId();
}
