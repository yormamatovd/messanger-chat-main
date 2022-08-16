package app.chat.repository.user;

import app.chat.entity.user.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserContactRepo extends JpaRepository<UserContact, Long> {
    List<UserContact> findAllByUser1_IdAndActiveTrue(Long user1_id);

    @Query("select u from user_contact u where u.user1.id = ?1 and u.user2.id = ?2 and u.active = true")
    Optional<UserContact> findUserContact(Long user1_id, Long user2_id);
}
