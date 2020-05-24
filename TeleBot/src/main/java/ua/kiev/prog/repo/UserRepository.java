package ua.kiev.prog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.kiev.prog.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.notified = false " +
            "AND u.phone IS NOT NULL AND u.email IS NOT NULL")
    List<User> findNewUsers();
    List<User> findAll();
    User findByChatId(long id);
    void deleteById(Long Id);
    List<User> findByNotified(boolean check);
    @Modifying
    @Query("UPDATE User u SET u.notified = :value WHERE u.id = :id")
    void updateUserByComment(@Param("value") boolean value, @Param("id") long id);
    @Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
    void updateUserByChatId(@Param("name") String name, @Param("id") long id);

    @Modifying
    @Query("UPDATE User u SET u.newuser = :value WHERE u.id = :id")
    void updateUserByNewUser(@Param("value") boolean value, @Param("id") long id);





}
