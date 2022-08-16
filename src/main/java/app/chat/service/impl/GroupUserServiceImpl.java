package app.chat.service.impl;

import app.chat.entity.group.Group;
import app.chat.entity.group.GroupBlockList;
import app.chat.entity.group.GroupUser;
import app.chat.entity.group.GroupUserPermission;
import app.chat.entity.personal.Personal;
import app.chat.entity.user.User;
import app.chat.enums.ChatPermissions;
import app.chat.enums.ChatRoleEnum;
import app.chat.enums.ErrorEnum;
import app.chat.enums.Lang;
import app.chat.helpers.UserSession;
import app.chat.helpers.Utils;
import app.chat.mapper.MapstructMapper;
import app.chat.model.req.group.GroupMemberReqDto;
import app.chat.model.resp.group.GroupMemberAddRespDto;
import app.chat.model.resp.group.GroupUserRespDto;
import app.chat.repository.ChatRoleRepo;
import app.chat.repository.GroupUserPermissionRepo;
import app.chat.repository.group.GroupBlockListRepo;
import app.chat.repository.group.GroupRepo;
import app.chat.repository.group.GroupUserRepo;
import app.chat.repository.personal.PersonalRepo;
import app.chat.repository.user.UserRepo;
import app.chat.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static app.chat.model.ApiResponse.response;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupUserServiceImpl implements GroupUserService {

    private final GroupUserPermissionRepo groupUserPermissionRepo;
    private final UserContactService userContactService;
    private final ChannelUserService channelUserService;
    private final GroupUserRepo groupMemberRepo;
    private final PersonalRepo personalRepo;
    private final GroupBlockListRepo groupBlockListRepo;
    private final GroupUserRepo groupUserRepo;
    private final ChatRoleRepo chatRoleRepo;
    private final UserSession session;
    private final GroupRepo groupRepo;
    private final ErrorService error;
    private final UserRepo userRepo;
    private final GroupService groupService;
    private final MapstructMapper mapper;

    @Autowired
    PersonalService personalService;


    /**
     * @param dto Data Transfer Object
     * @return todo addMember - guruxga odam qo`shish
     * todo - gurux mavjudligini tekshiradi
     * todo - qo`shayotgan odamingizni qayerdan topib qushayotganez tekshiriladi
     * to
     */
    @Override
    public ResponseEntity<?> addMembers(GroupMemberReqDto dto) {
        User activeUser = session.getUser();

        //todo check existing group
        Optional<Group> groupOptional = groupRepo.findById(dto.getGroupId());
        if (groupOptional.isEmpty()) {
            return response(error.message(ErrorEnum.GROUP_NOT_FOUND.getCode(), Lang.RU, dto.getGroupId()));
        }
        Group group = groupOptional.get();
        //todo check am I a member of this group
        Optional<GroupUser> memberOptional = groupMemberRepo.findByUser_IdAndGroup_IdAndActiveTrue(activeUser.getId(), dto.getGroupId());
        if (memberOptional.isEmpty()) {
            return response(error.message(ErrorEnum.YOU_NOT_MEMBER_IN_GROUP.getCode(), Lang.RU, dto.getGroupId()), HttpStatus.BAD_REQUEST);
        }
        //todo check permission for add member
        List<String> permissions = groupUserPermissionRepo.getPermissions(activeUser.getId(), group.getId());
        if (!permissions.contains(ChatPermissions.ADD_MEMBER.name())) {
            return response(error.message(ErrorEnum.NO_PERMISSION.getCode(), Lang.RU, dto.getGroupId()));
        }

        //todo check joining member not delete account
        List<GroupMemberAddRespDto> newMembers = new ArrayList<>();

        dto.getUserIdList().forEach(memberId -> {

            Optional<User> userOptional = userRepo.findById(memberId);
            if (userOptional.isEmpty()) {
                newMembers.add(new GroupMemberAddRespDto(memberId, false, error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU, memberId)));
            } else {
                User newMember = userOptional.get();
                //todo find my group mate for add user validation
                boolean itsMyFriend = !Utils.isEmpty(findMyGroupMate(memberId));

                //todo find in my contacts
//                if (!itsMyFriend){
                if (!Utils.isEmpty(userContactService.findInMyContacts(memberId))) {
                    itsMyFriend = true;
                }
//                }
                //todo men admin yoki owner bo`lgan kanallardan tekshirish
//                if (itsMyFriend){
                if (!Utils.isEmpty(channelUserService.findMyAuthorityChannelFollower(memberId))) {
                    itsMyFriend = true;
                }
//                }
                //todo find in my personals
//                if (!itsMyFriend) {
                for (Personal personal : personalRepo.getPersonal(activeUser.getId())) {
                    if (personal.getUser1().getId().equals(memberId) || personal.getUser2().getId().equals(memberId)) {
                        itsMyFriend = true;
                        break;
                    }
                }
//                }

                if (!newMember.isActive()) {
                    newMembers.add(new GroupMemberAddRespDto(memberId, false, error.message(ErrorEnum.USER_NOT_FOUND.getCode(), Lang.RU, memberId)));
                } else if (!itsMyFriend) {
                    newMembers.add(new GroupMemberAddRespDto(memberId, false, error.message(ErrorEnum.CHAT_NOT_FOUND.getCode(), Lang.RU, memberId)));
                } else {
                    //todo already exist in group
                    if (groupUserRepo.existsByUserAndGroupAndActiveTrue(newMember, group)) {
                        newMembers.add(new GroupMemberAddRespDto(memberId, false, error.message(ErrorEnum.MEMBER_ALREADY_EXIST.getCode(), Lang.RU, memberId)));
                    } else if (false/**todo check the privacy policy of joining members**/) {
                        //todo userni gurux yoki kanalga qo`shish ximoyasini tikshirish qismi
                        //todo user settings service kutilmoqda
                    } else {
                        newMembers.add(new GroupMemberAddRespDto(memberId, true, ""));
                    }
                }
            }
        });

        newMembers.forEach(newMember -> {
            if (newMember.getAdded()) {
                GroupUser groupUser = new GroupUser();
                groupUser.setGroup(group);
                groupUser.setUser(userRepo.getById(newMember.getUserId()));
                groupUser.setIsMute(false);
                groupUser.setIsPinned(false);
                groupUser.setChatRole(chatRoleRepo.findByRole(ChatRoleEnum.MEMBER).get());
                attachPermissions(groupUserRepo.save(groupUser), ChatRoleEnum.MEMBER);
                groupUserRepo.save(groupUser);
            }
        });

        return response(newMembers, (long) newMembers.size(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getSortedMembers(Long groupId) {

        //todo check existing group
        Optional<Group> optionalGroup = groupRepo.findById(groupId);
        if (optionalGroup.isEmpty()) {
            return response(error.message(30, Lang.RU, groupId), HttpStatus.NOT_FOUND);
        }

        List<GroupUser> groupMembers = groupUserRepo.findAllByGroup_IdAndActiveTrue(groupId);//, Sort.by("user.firstName").descending()

        List<GroupUserRespDto> groupMemberRespDto = mapper.toGroupUserDto(groupMembers);
        return response(groupMemberRespDto, (long) groupMemberRespDto.size(), HttpStatus.OK);
    }

    @Override
    public GroupUser findMyGroupMate(Long searchedFriendId) {

        //todo get i`m follow groups
        List<GroupUser> groupUserList = groupUserRepo.findAllByUser_Id(session.getUserId());

        for (GroupUser groupUser : groupUserList) {
            Group group = groupUser.getGroup();
            if (group.isActive()) {
                //todo get i`m follow group members
                List<GroupUser> members = groupUserRepo.findByGroup_IdAndActiveTrue(group.getId());

                //todo for each group members
                for (GroupUser member : members) {
                    if (member.getUser().isActive()) {
                        if (member.getUser().getId().equals(searchedFriendId)) {
                            return member;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ResponseEntity<?> joinByPrivateLink(String groupLink) {
        User activeUser = session.getUser();

        //todo check existing group
        Optional<Group> groupOptional = groupRepo.findByLink(groupLink);
        if (groupOptional.isEmpty()) {
            return response(error.message(ErrorEnum.GROUP_NOT_FOUND.getCode(), Lang.RU, groupLink));
        }
        Group group = groupOptional.get();
        //todo check is blocked user
        boolean isBlockedUser = groupBlockListRepo.existsByUser_IdAndGroup_IdAndActiveTrue(activeUser.getId(), group.getId());
        if (isBlockedUser) {
            groupBlockListRepo.save(new GroupBlockList(activeUser, group));
            return response(true, HttpStatus.OK);
        } else {
            boolean alreadyJoin = groupUserRepo.existsByUserAndGroupAndActiveTrue(activeUser, group);
            if (!alreadyJoin) {

                GroupUser groupUser = new GroupUser();
                groupUser.setUser(activeUser);
                groupUser.setGroup(group);
                groupUserRepo.save(groupUser);

                //todo set permission
                attachPermissions(groupUser, ChatRoleEnum.MEMBER);

            }
            return response(true, HttpStatus.OK);

        }

    }

    @Override
    public ResponseEntity<?> joinByUsername(String groupUsername) {
        User activeUser = session.getUser();

        //todo check existing group
        Optional<Group> groupOptional = groupRepo.findByUsername_Name(groupUsername);
        if (groupOptional.isEmpty()) {
            return response(error.message(ErrorEnum.GROUP_NOT_FOUND.getCode(), Lang.RU, groupUsername),HttpStatus.NOT_FOUND);
        }
        Group group = groupOptional.get();
        //todo check is blocked user
        boolean isBlockedUser = groupBlockListRepo.existsByUser_IdAndGroup_IdAndActiveTrue(activeUser.getId(), group.getId());
        if (isBlockedUser) {
            groupBlockListRepo.save(new GroupBlockList(activeUser, group));
            return response(true, HttpStatus.OK);
        } else {
            boolean alreadyJoin = groupUserRepo.existsByUserAndGroupAndActiveTrue(activeUser, group);
            if (!alreadyJoin) {

                GroupUser groupUser = new GroupUser();
                groupUser.setUser(activeUser);
                groupUser.setGroup(group);
                groupUserRepo.save(groupUser);

                //todo set permission
                attachPermissions(groupUser, ChatRoleEnum.MEMBER);

            }
            return response(true, HttpStatus.OK);

        }
    }

    public void attachPermissions(GroupUser groupUser, ChatRoleEnum role) {
        List<GroupUserPermission> permissions = new ArrayList<>();
        Set<ChatPermissions> permission = ChatPermissions.getPermission(role);
        permission.forEach(chatPermission -> permissions.add(new GroupUserPermission(groupUser, chatPermission)));
        groupUserPermissionRepo.saveAll(permissions);
    }
}
