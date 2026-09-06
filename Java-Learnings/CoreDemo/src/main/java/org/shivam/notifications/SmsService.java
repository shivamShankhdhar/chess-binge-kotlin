package org.shivam.notifications;

public class SmsService  implements NotificationService{
    public void sendNotification(){
        IO.println("sms notification sent");
    }
}
