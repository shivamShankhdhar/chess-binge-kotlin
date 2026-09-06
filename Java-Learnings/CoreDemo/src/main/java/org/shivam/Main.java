package org.shivam;

import org.shivam.notifications.EmailService;
import org.shivam.notifications.NotificationService;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        NotificationService notificationService = new EmailService();
        OrderService os = new OrderService(notificationService);
        os.order();
        }
    }

