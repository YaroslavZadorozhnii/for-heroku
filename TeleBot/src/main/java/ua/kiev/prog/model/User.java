package ua.kiev.prog.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.List;

@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Long chatId;
    private Integer stateId;
    private String phone;
    @Column(length = 5000)
    private static String phoneList;
    private String email;
    private String comment;
    private Boolean admin;
    private Boolean notified = false;
    private Boolean newuser;
    private boolean marhaller;

    public boolean isMarhaller() {
        return marhaller;
    }

    public void setMarhaller(boolean marhaller) {
        this.marhaller = marhaller;
    }

    public Boolean getNewUser() {
        return newuser;
    }

    public void setNewUser(Boolean newUser) {
        this.newuser = newUser;
    }

    public String getPhoneList() {
        return phoneList;
    }

    public User() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User(Long chatId, Integer state) {
        this.chatId = chatId;
        this.stateId = state;
    }

    public User(Long chatId, Integer stateId, Boolean admin) {
        this.chatId = chatId;
        this.stateId = stateId;
        this.admin = admin;
    }
    public User(Long chatId, Integer stateId, String name) {
        this.chatId = chatId;
        this.stateId = stateId;
        this.name = name;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        if(phoneList == null && phone != null){phoneList = "[ date:{" +  new Date()  + "} phone:{" +  phone + "}]; \n";}
        else if(!"phone".equals(phone) && phone != null){phoneList += "[ date:{" +  new Date()  + "} phone:{" +  phone + "}]; \n";}
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean getNotified() {
        return notified;
    }

    public void setNotified(Boolean notified) {
        this.notified = notified;
    }
}
