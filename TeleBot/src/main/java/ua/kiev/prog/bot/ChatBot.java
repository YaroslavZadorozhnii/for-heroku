package ua.kiev.prog.bot;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.kiev.prog.model.User;
import ua.kiev.prog.service.UserService;

import java.io.InputStream;
import java.util.List;

@Component
@PropertySource("classpath:telegram.properties")
public class ChatBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LogManager.getLogger(ChatBot.class);
    private static final String DELETE = "delete";
    private static final String BROADCAST = "broadcast ";
    private static final String LIST_USERS = "users";

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    private final UserService userService;
    private static BotState stateNow;
    public ChatBot(UserService userService) {
        this.userService = userService;
    }
    static String command;
    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        BotContext context;
        BotState state;
    Gson gson = new Gson();
        try {
            String i = gson.toJson(update.getMessage());
            System.out.println(i);
        }catch (Exception e){
        }
        try {
            String i = gson.toJson(update.getCallbackQuery().getData());
            System.out.println(i);
        }catch (Exception e){}

            Object[] fromJson = postUpdate(update);
            final String text = (String) fromJson[0];////update.getMessage().getText();
            final long chatId = (long) fromJson[1];///update.getMessage().getChatId();
            User user = userService.findByChatId(chatId);

            if((boolean)fromJson[2]){
                user.setPhone(text);
            }
            if (checkIfAdminCommand(user, text))
                return;

            if (user == null) {
                state = BotState.getInitialState();

                user = new User(chatId, state.ordinal(), (String) fromJson[3]);
                user.setNewUser(false);
                userService.addUser(user);

                context = BotContext.of(this, user, text);
                try {

                    state.enter(context);
                } catch (TelegramApiException e) {

                }

                LOGGER.info("New user registered: " + chatId);
            } else {
                context = BotContext.of(this, user, text);
                state = BotState.byId(user.getStateId());

                LOGGER.info("Update received for user in state: " + state);
            }
            try {
                user.setPhone(update.getMessage().getContact().getPhoneNumber());
            }catch (Exception e){System.out.println("Error");}
            state.handleInput(context);
           // System.out.println("My test : " + command + "\n " + text);
        do {
                if(user.getComment() != null &&
                        user.getPhone() != null &&
                        user.getEmail() != null){

                        System.out.println(command + " " + user.isMarhaller());


                    user = getCommand(command, user, text);
                    if(user.isMarhaller()){
                        state = stateNow;
                        user.setMarhaller(false);
                    } else {state = BotState.Finish;}
                }
                else {state = state.nextState();}
                try {
                    state.enter(context);
                } catch (TelegramApiException e) {

                }
            } while (!state.isInputNeeded());

            user.setStateId(state.ordinal());
            userService.updateUser(user);
            //context.getUser().setComment(text);
        }

    private boolean checkIfAdminCommand(User user, String text) {
        if (user == null || !user.getAdmin())
            return false;

        if (text.startsWith(BROADCAST)) {
            LOGGER.info("Admin command received: " + BROADCAST);

            text = text.substring(BROADCAST.length());
            broadcast(text);

            return true;
        } else if (text.equals(LIST_USERS)) {
            LOGGER.info("Admin command received: " + LIST_USERS);

            listUsers(user);
            return true;
        } else if (text.indexOf(DELETE) != -1){
            LOGGER.info("Admin command received: " + DELETE);
            String result = text.replace(DELETE + " ","");
            delete(Long.parseLong(result));
            listUsers(user);
            return true;
        } else if(text.indexOf("addUser") != -1){
            String result = text.replace("addUser ","");
            String[] pice = result.split(",");
            User user1 = new User((long)12121, 12);
            user1.setEmail(pice[0]);
            user1.setPhone(pice[1]);
            userService.addUser(user1);
            return true;
        }

        return false;
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(long chatId) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test.png");

        SendPhoto message = new SendPhoto()
                .setChatId(chatId)
                .setPhoto("test", is);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void listUsers(User admin) {
        StringBuilder sb = new StringBuilder("All users list:\r\n");
        List<User> users = userService.findAllUsers();

        users.forEach(user ->
            sb.append(user.getId())
                    .append(' ')
                    .append(user.getPhone())
                    .append(' ')
                    .append(user.getEmail())
                    .append("\r\n")
        );

        sendPhoto(admin.getChatId());
        sendMessage(admin.getChatId(), sb.toString());
    }

    private void broadcast(String text) {
        List<User> users = userService.findAllUsers();
        users.forEach(user -> sendMessage(user.getChatId(), text));
    }

    private void delete (long id){
        userService.delete(id);
    }
    ///////////////////////////////////////////////////////////////////

   static User getCommand(String command, User user, String text){
       try {command = command.trim();
       }catch (Exception r){}
        if("comment".equals(command)){
            user.setMarhaller(true);
            stateNow = BotState.Comment;
            user.setComment(text);
        } else if("phone".equals(command)){
            user.setMarhaller(true);
            stateNow = BotState.EnterPhone;
            user.setPhone(text);
        } else if ("email".equals(command)){
            user.setMarhaller(true);
            stateNow = BotState.EnterEmail;
            user.setEmail(text);
        }
        return user;
   }
   private Object[] postUpdate(Update update){
       Gson gson = new Gson();
       String i = "";
        long id = 0;
        String name ="";
       boolean fix = false;
       try {
           i = gson.toJson(update.getMessage().getContact().getPhoneNumber().replace("\"", ""));
           id =Long.parseLong(gson.toJson(update.getMessage().getChat().getId()));
           name = gson.toJson(update.getMessage().getChat().getFirstName());
           fix = true;
       }catch (Exception e){}
       try {
           i = gson.toJson(update.getMessage().getText());
           id = Long.parseLong(gson.toJson(update.getMessage().getChat().getId()));
           name = gson.toJson(update.getMessage().getChat().getFirstName());
           System.out.println(i);
       }catch (Exception e1){}
       try {
           i = gson.toJson(update.getCallbackQuery().getData());
           id = update.getCallbackQuery().getFrom().getId();
       }catch (Exception ek){}
       if(update.hasCallbackQuery()){
       command = i.replace("\"", "");
      User user = userService.findByChatId(id);
           if("comment".equals(command)){
               user.setNewUser(true);
           } else if("phone".equals(command)){
               user.setNewUser(true);
           } else if ("email".equals(command)){
               user.setNewUser(true);
           }
       }else {command = null;}
       return new Object[]{i.replace("\"", "").trim(), id, fix, name.replace("\"", "")};
   }

}
