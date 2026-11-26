package com.tablehub.operations.port;

/**
 * Port for sending asynchronous emails via K1's Mail Helper.
 */
public interface NotificationMailPort {

    void sendStaffInvitationEmail(String toEmail, Long restaurantId, String invitedRole, String rawToken);
}
