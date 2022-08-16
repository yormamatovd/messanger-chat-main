package app.chat.service;

import app.chat.entity.channel.ChannelUser;
import app.chat.entity.user.UserContact;
import app.chat.model.dto.UserDto;
import app.chat.model.dto.UserEditDto;
import app.chat.model.req.AddContactDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {
    ResponseEntity<?> getUser(Long id);

    ResponseEntity<?> getAll();

    ResponseEntity<?> createUser(UserDto dto);

    ResponseEntity<?> updateUser(Long id, UserEditDto dto);

    ResponseEntity<?> deleteUser(Long id);

    ResponseEntity<?> getContacts();

    ResponseEntity<?> getPersonal();

    ResponseEntity<?> getGroup();

    ResponseEntity<?> getChannel();

    ResponseEntity<?> getUserBlock();
    
    List<ChannelUser> getMyAuthorityChannel();

    ResponseEntity<?> getContact(Long contactId);

    ResponseEntity<?> addToContact(AddContactDto dto);

    ResponseEntity<?> editContact(Long contactId, String newName);

    ResponseEntity<?> deleteContact(Long contactId);
}
