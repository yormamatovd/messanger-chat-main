package app.chat.service.impl;

import app.chat.entity.channel.Channel;
import app.chat.entity.channel.ChannelUser;
import app.chat.entity.group.Group;
import app.chat.entity.group.GroupUser;
import app.chat.entity.personal.Personal;
import app.chat.entity.settings.Notification;
import app.chat.entity.settings.PrivacySecurity;
import app.chat.entity.settings.Settings;
import app.chat.entity.settings.TwoStepVerification;
import app.chat.entity.user.User;
import app.chat.entity.user.UserBlock;
import app.chat.entity.user.UserContact;
import app.chat.entity.user.Username;
import app.chat.enums.Activity;
import app.chat.enums.ErrorEnum;
import app.chat.enums.Lang;
import app.chat.helpers.UserSession;
import app.chat.helpers.Utils;
import app.chat.mapper.MapstructMapper;
import app.chat.model.ApiResponse;
import app.chat.model.dto.UserDto;
import app.chat.model.dto.UserEditDto;
import app.chat.model.req.AddContactDto;
import app.chat.model.resp.ChannelRespDto;
import app.chat.model.resp.group.GroupRespDto;
import app.chat.repository.channel.ChannelUserRepo;
import app.chat.repository.group.GroupUserRepo;
import app.chat.repository.personal.PersonalRepo;
import app.chat.repository.sittings.*;
import app.chat.repository.user.UserContactRepo;
import app.chat.repository.user.UserRepo;
import app.chat.service.ErrorService;
import app.chat.service.UserService;
import app.chat.service.UsernameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static app.chat.helpers.Utils.isEmpty;
import static app.chat.helpers.Utils.isValidEmailAddress;
import static app.chat.model.ApiResponse.response;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final GroupUserRepo groupUserRepo;
    private final UserContactRepo userContactRepo;
    private final UsernameService usernameService;
    private final MapstructMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final SettingsRepo settingsRepo;
    private final NotificationRepo notificationRepo;
    private final PrivacySecurityRepo privacySecurityRepo;
    private final LanguageRepo languageRepo;
    private final UserSession session;
    private final TwoStepVerificationRepo twoStepRepo;
    private final ErrorService error;
    private final ChannelUserRepo channelUserRepo;
    private final PersonalRepo personalRepo;


    @Override
    public ResponseEntity<?> getUser(Long id) {
        Optional<User> optionalUser = userRepo.findById(id);
        if (optionalUser.isEmpty()) {
            return response(error.message(10, Lang.RU, id), HttpStatus.NOT_FOUND);
        }
        User user = optionalUser.get();
        return response(mapper.toUserDto(user));
    }

    @Override
    public ResponseEntity<?> getAll() {
        return response(mapper.toUserDto(userRepo.findAll()));
    }

    @Override
    public ResponseEntity<?> createUser(UserDto dto) {
        if (userRepo.existsByEmail(dto.getEmail())) {
            return response(error.message(11, Lang.RU, dto.getEmail()), HttpStatus.BAD_REQUEST);
        }

        if (!Utils.isEmpty(dto.getUsername())) {
            if (usernameService.checkExist(dto.getUsername())) {
                return response(error.message(51, Lang.RU, dto.getUsername()), HttpStatus.BAD_REQUEST);
            }
        }

        if (!isValidEmailAddress(dto.getEmail())) {
            return response(error.message(12, Lang.RU, dto.getEmail()), HttpStatus.BAD_REQUEST);
        }
        User user = new User();

        if (!Utils.isEmpty(dto.getUsername())) {
            Username username = usernameService.create(dto.getUsername());
            user.setUserName(username);
        }
        user.setBio(dto.getBio());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActivity(Activity.OFFLINE);
        Settings settings = new Settings();
        settings.setNotification(notificationRepo.save(new Notification()));
        TwoStepVerification twoStepVerification = twoStepRepo.save(new TwoStepVerification(dto.getEmail(), "", false));
        PrivacySecurity privacySecurity = new PrivacySecurity();
        privacySecurity.setTwoStepVerification(twoStepVerification);
        settings.setPrivacySecurity(privacySecurityRepo.save(privacySecurity));
        settings.setLanguage(languageRepo.getById(1L));
        user.setSettings(settingsRepo.save(settings));
        user = userRepo.save(user);
        return response(mapper.toUserDto(user), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<?> updateUser(Long id, UserEditDto dto) {
        Optional<User> optionalUser = userRepo.findActiveById(id);
        if (optionalUser.isEmpty()) {
            return response(error.message(10, Lang.RU, id), HttpStatus.NOT_FOUND);
        }
        if (userRepo.existsByEmailAndIdNot(dto.getEmail(), id)) {
            return response(error.message(11, Lang.RU, dto.getEmail()), HttpStatus.BAD_REQUEST);
        }

        User user = optionalUser.get();
        user.setBio(dto.getBio());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPassword(dto.getPassword());
        user = userRepo.save(user);
        return response(user);
    }

    @Override
    public ResponseEntity<?> deleteUser(Long id) {
        Optional<User> optionalUser = userRepo.findActiveById(id);
        if (optionalUser.isEmpty()) {
            return response(error.message(10, Lang.RU, id), HttpStatus.NOT_FOUND);
        }
        User user = optionalUser.get();
        user.setLastName(null);
        user.setFirstName("Deleted account");
        user.setActive(false);
        usernameService.delete(user.getUserName().getId());
        user.setEmail(user.getEmail() + "/!deleted!/");
        userRepo.save(user);
        return response(new ApiResponse<>());
    }

    @Override
    public ResponseEntity<?> getContacts() {
        Long userId = session.getUserId();
        if (!Utils.isEmpty(userId)) {
            List<UserContact> contacts = userContactRepo.findAllByUser1_IdAndActiveTrue(userId);
            return response(mapper.toUserContactResp(contacts), (long) contacts.size());
        }
        return null;
    }

    @Override
    public ResponseEntity<?> getPersonal() {
        Long userId = session.getUserId();
        if (!Utils.isEmpty(userId)) {
            // todo fix Fazliddin
            List<Personal> personals = personalRepo.getPersonal(userId);
            return response(mapper.toPersonalDto(personals));
        }
        return null;
    }

    @Override
    public ResponseEntity<?> getGroup() {
        Long userId = session.getUserId();
        if (!Utils.isEmpty(userId)) {
            List<GroupUser> followGroups = groupUserRepo.findAllByUser_Id(userId);
            List<Group> groups = new ArrayList<>();
            followGroups.forEach(groupUser -> groups.add(groupUser.getGroup()));

            //to response dto
            List<GroupRespDto> groupRespDtoList = mapper.toGroupRespDto(groups);

            //set total member counts
            groupRespDtoList.forEach(groupRespDto -> groupRespDto.setMemberCount((long) groupUserRepo.findAllByGroup_IdAndActiveTrue(groupRespDto.getId()).size()));

            return response(groupRespDtoList, (long) groupRespDtoList.size(), HttpStatus.OK);
        }
        return null;
    }

    @Override
    public ResponseEntity<?> getChannel() {
        Long userId = session.getUserId();
        if (!Utils.isEmpty(userId)) {
            List<ChannelUser> channelUserList = channelUserRepo.findAllByUser_Id(userId);
            List<Channel> channels = new ArrayList<>();
            channelUserList.forEach(channelUser -> channels.add(channelUser.getChannel()));
            List<ChannelRespDto> channelRespDtoList = mapper.toChannelDto(channels);
            return response(channelRespDtoList, (long) channelRespDtoList.size());
        }
        return null;
    }

    @Override
    public ResponseEntity<?> getUserBlock() {
        Long userId = session.getUserId();
        if (!Utils.isEmpty(userId)) {
            List<UserBlock> blockList = userRepo.getBlocks(userId);
            return response(blockList, (long) blockList.size());
        }
        return null;
    }

    /**
     * todo activeUser admin yoki owner bo`lgan kanallarimni olib beradi
     *
     * @return ChannelUser list
     */
    @Override
    public List<ChannelUser> getMyAuthorityChannel() {
        List<ChannelUser> myChannels = new ArrayList<>();
        Long activeUserId = session.getUserId();

        List<ChannelUser> channelUserList = channelUserRepo.findAllByUser_Id(activeUserId);

        //todo activeUser admin yoki owner bo`lgan kanallarni myChannels ga qo`shish qismi
        for (ChannelUser channelUser : channelUserList) {
            Channel channel = channelUser.getChannel();
            if (channelUserRepo.isAdmin(activeUserId, channel.getId())) {
                Optional<ChannelUser> authChannelOptional = channelUserRepo.findActiveChannelUser(activeUserId, channel.getId());
                authChannelOptional.ifPresent(myChannels::add);
            }
        }

        return myChannels;
    }

    @Override
    public ResponseEntity<?> addToContact(AddContactDto dto) {
        User user = session.getUser();
        if (isEmpty(user)) {
            return response(error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        Optional<User> optionalUser = userRepo.findByEmailAndActiveTrue(dto.getEmail());
        if (optionalUser.isEmpty()) {
            return response(error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU, dto.getEmail()));
        }
        User addingUser = optionalUser.get();
        if (Objects.equals(addingUser.getId(), user.getId())) {
            return response(error.message(ErrorEnum.USER_CONFLICT.getCode(), Lang.RU), HttpStatus.CONFLICT);
        }
        Optional<UserContact> optionalUserContact = userContactRepo.findUserContact(user.getId(), addingUser.getId());
        if (optionalUserContact.isPresent()) {
            return editContact(addingUser.getId(), dto.getName());
        }
        UserContact userContact = new UserContact();
        userContact.setUser1(user);
        userContact.setUser2(addingUser);
        userContact.setUser2Name(dto.getName());
        return response(mapper.toUserContactResp(userContactRepo.save(userContact)));
    }

    @Override
    public ResponseEntity<?> editContact(Long contactId, String newName) {
        Long userId = session.getUserId();
        if (isEmpty(userId)) {
            return response(error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        Optional<UserContact> optionalUserContact = userContactRepo.findUserContact(userId, contactId);
        if (optionalUserContact.isEmpty()) {
            return response(error.message(ErrorEnum.USER_CONTACT_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        UserContact userContact = optionalUserContact.get();
        userContact.setUser2Name(newName);
        return response(mapper.toUserContactResp(userContactRepo.save(userContact)));
    }

    @Override
    public ResponseEntity<?> deleteContact(Long contactId) {
        Long userId = session.getUserId();
        if (isEmpty(userId)) {
            return response(error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        Optional<UserContact> optionalUserContact = userContactRepo.findUserContact(userId, contactId);
        if (optionalUserContact.isEmpty()) {
            return response(error.message(ErrorEnum.USER_CONTACT_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        UserContact userContact = optionalUserContact.get();
        userContact.setActive(false);
        userContactRepo.save(userContact);
        return response("Contact deleted!");
    }

    @Override
    public ResponseEntity<?> getContact(Long contactId) {
        Long userId = session.getUserId();
        if (isEmpty(userId)) {
            return response(error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        Optional<UserContact> optionalUserContact = userContactRepo.findUserContact(userId, contactId);
        if (optionalUserContact.isEmpty()) {
            return response(error.message(ErrorEnum.USER_CONTACT_NOT_FOUND.getCode(), Lang.RU), HttpStatus.NOT_FOUND);
        }
        return response(mapper.toUserContactResp(optionalUserContact.get()));
    }


}
