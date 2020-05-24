 package ua.kiev.prog.bot;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.*;


 public enum BotState {
    Start {
        private BotContext con;
        String hello = "Привет, ";
        private BotState next;
        @Override
        public void enter(BotContext context) {
            String name = context.getUser().getName() + "!";
            if(!"Привет, ".equals(hello)){name = ".";}
            ReplyKeyboardMarkup markup = requestPhone();
            try {context.getBot().execute(new SendMessage().setText(hello + name).setChatId(context.getUser()
                        .getChatId()).setReplyMarkup(markup));
            } catch (TelegramApiException e) {}
        }
        @Override
        public void handleInput(BotContext context) {
            hello = "Hажми кнопку \"Начать регистрацию\" чтоб продолжить";
            con = context;
           // context.getUser().setPhone(context.getInput());
         if(context.getUser().getPhone() != null){
             next = nextBotState(con);
         }else {next = Start;}
        }

        @Override
        public BotState nextState() {
            return next;
        }
    },

    EnterPhone {
        private BotContext con;
        @Override
        public void enter(BotContext context) {
            System.out.println(">>>>>>>>>"+BotState.values() + "  "+ BotState.byId(0)+ "  "+ BotState.byId(1)+ "  "+ BotState.byId(2));
            sendMessage(context, "Enter your phone number please:");
        }

        @Override
        public void handleInput(BotContext context) {
            context.getUser().setPhone(context.getInput());
            Set<Integer> set = new HashSet<>();
            for(int i =0 ; i<5;i++){set.add(i);}
            List<Integer> list = new ArrayList<>(set);
            System.out.println(list);
            con = context;
            System.out.println(context.getUser().getPhoneList());
        }

        @Override
        public BotState nextState() {
            return nextBotState(con);
        }
    },


    EnterEmail {
        String info = EmojiParser.parseToUnicode(":email:") + " Пожалуйста, вашу почту: ";
        String time = "";
        private BotState next1;
        @Override
        public void enter(BotContext context) {
            System.out.println(">>>>>>>>>"+BotState.values().toString());
            sendMessage(context, (info + time));
        }

        @Override
        public void handleInput(BotContext context) {
            String email = context.getInput();

            if (Utils.isValidEmailAddress(email)) {
                context.getUser().setEmail(context.getInput());
                next1 = nextBotState(context);
            } else {
                info = email;
                sendMessage(context, EmojiParser.parseToUnicode(":no_entry:") + " Вы ввели неправильный email, проверьте ввод :");
                next1 = EnterEmail;
            }
        }

        @Override
        public BotState nextState() {
            return next1;
        }
    },
    Comment{
        @Override
        public void enter(BotContext context) {
            sendMessage(context, "Если у вас есть для нас какой-то вопрос " + EmojiParser.parseToUnicode(":interrobang:") +
                    " вы можете задать" +
                    " его здесь, или отправить нам смайлик \uD83D\uDE1C");
        }
        @Override
        public void handleInput(BotContext context) {
            context.getUser().setComment(context.getInput());
        }

        @Override
        public BotState nextState() {

            return Approved;
        }
    },
    Approved(false){
        @Override
        public void enter(BotContext context) {
            sendMessage(context, "Спасибо :) Если хотите что-то поменять нажмите на нужный раздел меню. " +
            EmojiParser.parseToUnicode(":point_down:"));
        }

        @Override
        public BotState nextState() {
            return Finish;
        }
    },
    Finish{
        private BotState next;
        @Override
        public void enter(BotContext context) {
            context.getUser().setNotified(true);
            String tasck ="";
            if(context.getUser().getComment().length() < 5){tasck = EmojiParser.parseToUnicode(":kissing:") +
                    context.getUser().getComment();}
            else {
                Formatter f = new Formatter();
                f.format("Вы спросили: %S \nМы ответим вам на почту," +" или позвоним в ближайшее время!", context.getUser().getComment());
                tasck = f.toString();}
            try {
                Map<String, String> map = new HashMap<String, String>();
                map.put(EmojiParser.parseToUnicode(":thought_balloon:") + " Спросить  ", "comment");
                map.put(EmojiParser.parseToUnicode(":telephone_receiver:" + " Мой номер"), " phone");
                map.put(EmojiParser.parseToUnicode(":mailbox:") + " Мой email", " email");
                sendMessage(context, "Ваш телефон: " + context.getUser().getPhone());
                sendMessage(context, "Ваш email: " + context.getUser().getEmail());
                sendMessage(context, tasck);
                InlineKeyboardMarkup markup = sessage(context, map,
                        " Меню");
                context.getBot().execute(new SendMessage().setText("Menu " + EmojiParser.parseToUnicode(":gear:")).setChatId(context.getUser()
                        .getChatId()).setReplyMarkup(markup));
            } catch (TelegramApiException e) {

            }
        }

        @Override
        public void handleInput(BotContext context) {
            System.out.println(context.getInput());
            if(context.getInput().equals("email")){
                next = EnterEmail;
            }else if(context.getInput().equals("phone")){
                next = EnterPhone;
            } else if(context.getInput().equals("comment")){
                next = Comment;
            } else if(context.getInput().equals("zero")){
                next = Start;
            }
        }

        @Override
        public BotState nextState() {
            return next;
        }
    };

     private static BotState[] states;
    private final boolean inputNeeded;

    BotState() {
        this.inputNeeded = true;
    }

    BotState(boolean inputNeeded) {
        this.inputNeeded = inputNeeded;
    }

    public static BotState getInitialState() {
        return byId(0);
    }

    public static BotState byId(int id) {
        if (states == null) {
            states = BotState.values();
        }

        return states[id];
    }

    protected void sendMessage(BotContext context, String text) { ///////////////////////////
        SendMessage message = new SendMessage()
                .setChatId(context.getUser().getChatId())
                .setText(text);
        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public boolean isInputNeeded() {
        return inputNeeded;
    }

    public void handleInput(BotContext context) {
        // do nothing by default
    }

    public abstract void enter(BotContext context) throws TelegramApiException;
    public abstract BotState nextState();

    static BotState nextBotState(BotContext context){
        BotState nextState;
        if(context.getUser().getPhone() == null){nextState = EnterPhone;
        } else if(context.getUser().getEmail() == null){nextState = EnterEmail;
        } else if (context.getUser().getComment() == null){nextState = Comment;
        } else {nextState = Approved;}
        return nextState;
    }

    private static InlineKeyboardMarkup sessage(BotContext context, Map<String, String> buttonText,  String message) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        int num = 0;
        int count = 0;
        for (String string : buttonText.keySet()){
            InlineKeyboardButton button = new InlineKeyboardButton()
                    .setText(string)
                    .setCallbackData(buttonText.get(string));
            count++;
            if(num < 1){keyboardButtonsRow  = new ArrayList<>();}
            keyboardButtonsRow.add(button);
            num++;
            if(num == 3 || count == buttonText.size()){
                rowList.add(keyboardButtonsRow);
                num = 0;
            }

        }
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
       // return new SendMessage().setChatId(context.getUser().getChatId()).setText(message).setReplyMarkup(inlineKeyboardMarkup);
    }
     public static ReplyKeyboardMarkup requestPhone(){
         ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup().setOneTimeKeyboard(true);
         List<KeyboardRow> row = new ArrayList<>();
         KeyboardRow bord = new KeyboardRow();
         KeyboardButton kb = new KeyboardButton(EmojiParser.parseToUnicode(":white_check_mark:") + "Начать регистрацию").setRequestContact(true);
         bord.add(kb);
         row.add(bord);
         keyboardMarkup.setKeyboard(row);
         return keyboardMarkup;
     }
}
