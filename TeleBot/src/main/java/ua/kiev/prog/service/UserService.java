package ua.kiev.prog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kiev.prog.model.User;
import ua.kiev.prog.repo.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Transactional
    public void updateUserNameById(String name, long id){userRepository.updateUserByChatId(name, id);}
    @Transactional(readOnly = true)
    public User findByChatId(long id) {
        return userRepository.findByChatId(id);
    }
    @Transactional
    public void updateNewUser(boolean value, long id){userRepository.updateUserByNewUser(value, id);}
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    @Transactional
    public void updateUserByComment(boolean value, long id){ userRepository.updateUserByComment(value, id);}
    @Transactional
    public List<User> findNewUsers() {
        List<User> users = userRepository.findNewUsers();

        users.forEach((user) -> user.setNotified(true));
        userRepository.saveAll(users);

        return users;
    }
    @Transactional
    public List<User> findAll(){return userRepository.findAll();}
    public List<User> findByNotified(){return userRepository.findByNotified(true);}
    @Transactional
    public void addUser(User user) {
        user.setAdmin(userRepository.count() == 0);
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }
    @Transactional
    public void delete(long id){userRepository.deleteById(id);}
}

