package com.tablehub.operations.port;

/**
 * Port gửi Email bất đồng bộ qua Mail Helper của K1.
 */
public interface NotificationMailPort {

    void sendStaffInvitationEmail(String toEmail, Long restaurantId, String invitedRole, String rawToken);
}
