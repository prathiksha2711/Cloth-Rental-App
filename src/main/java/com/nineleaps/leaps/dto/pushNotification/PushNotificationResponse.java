package com.nineleaps.leaps.dto.pushNotification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PushNotificationResponse {
    private int status;
    private String message;
}

