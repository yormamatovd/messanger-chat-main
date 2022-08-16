package app.chat.controller;

import app.chat.model.dto.UserDto;
import app.chat.model.dto.UserEditDto;
import app.chat.model.req.AddContactDto;
import app.chat.service.AuthService;
import app.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable(value = "id") Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/get/list")
    public ResponseEntity<?> getList() {
        return userService.getAll();
    }

    @PostMapping()
    public ResponseEntity<?> add(@RequestBody UserDto dto) {
        return userService.createUser(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable(value = "id") Long id, @RequestBody UserEditDto dto) {
        return userService.updateUser(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable(value = "id") Long id) {
        return userService.deleteUser(id);
    }

    @GetMapping("/contact")
    public ResponseEntity<?> getContacts() {
        return userService.getContacts();
    }

    @GetMapping("/personal")
    public ResponseEntity<?> getPersonals() {
        return userService.getPersonal();
    }

    @GetMapping("/group")
    public ResponseEntity<?> getGroups() {
        return userService.getGroup();
    }

    @GetMapping("/channel")
    public ResponseEntity<?> getChannels() {
        return userService.getChannel();
    }

    @GetMapping("/userBlock")
    public ResponseEntity<?> getUserBlocks() {
        return userService.getUserBlock();
    }

    // User contacts
    @GetMapping("/contact/get/{id}")
    public ResponseEntity<?> getContact(@PathVariable(value = "id") Long contactId) {
        return userService.getContact(contactId);
    }

    @PostMapping("/contact/add")
    public ResponseEntity<?> addToContact(@RequestBody AddContactDto dto) {
        return userService.addToContact(dto);
    }

    @PutMapping("/contact/edit")
    public ResponseEntity<?> editContact(@RequestParam Long contactId,
                                         @RequestParam String newName) {
        return userService.editContact(contactId, newName);
    }

    @DeleteMapping("/contact/delete")
    public ResponseEntity<?> deleteContact(@RequestParam Long contactId) {
        return userService.deleteContact(contactId);
    }
}
