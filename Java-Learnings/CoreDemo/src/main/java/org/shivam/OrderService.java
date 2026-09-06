package org.shivam;

import org.shivam.notifications.EmailService;
import org.shivam.notifications.NotificationService;
import org.shivam.notifications.SmsService;

public class OrderService {
    NotificationService notificationService;


//    CONSTRUCTOR DI
    public OrderService(NotificationService notificationService){
        this.notificationService= notificationService;
    }

//    SETTER DI
    public void setNotification(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    public void order() {
        IO.println("order placed");
        notificationService.sendNotification();
    }
}
