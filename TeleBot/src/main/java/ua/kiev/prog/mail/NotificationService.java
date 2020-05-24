package ua.kiev.prog.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.kiev.prog.model.User;
import ua.kiev.prog.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PropertySource("classpath:telegram.properties")
public class NotificationService {
    private final static Map<Integer, String> test = new HashMap<>();
    private final UserService userService;
    private final JavaMailSender emailSender;

    @Value("${bot.email.subject}")
    private String emailSubject;
    private String title;
    @Value("${bot.email.from}")
    private String emailFrom;

    @Value("${bot.email.to}")
    private String emailTo;

    public NotificationService(UserService userService, JavaMailSender emailSender) {
        this.userService = userService;
        this.emailSender = emailSender;
    }

    @Scheduled(fixedRate = 10000)
    public void sendNewApplications() {
        List<User> users = userService.findByNotified();
        if (users.size() == 0)
            return;
        StringBuilder sb = new StringBuilder();
        for(User user : users){
            System.out.println(user.getNewUser() + " " + emailSubject);
            if(user.getNewUser()){
                title = "Изменение данных пользователя " + user.getName();
            }else {title = emailSubject;
            userService.updateNewUser(true, user.getId());}
            userService.updateUserByComment(false, user.getId());
        }


        users.forEach(user ->

            sb.append("Phone: ")
                    .append(user.getPhoneList())
                    .append("\r\n")
                    .append("Email: ")
                    .append(user.getEmail())
                    .append("\r\n\r\n")
                    .append("Comment: ")
                    .append(user.getComment())

        );

        sendEmail(sb.toString() );
    }

    private void sendEmail(String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(emailTo);
        message.setFrom(emailFrom);
        message.setSubject(title);
        message.setText(text);

        emailSender.send(message);
    }
}
