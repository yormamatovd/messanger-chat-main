package app.chat.controller;

import app.chat.model.req.group.GroupMemberReqDto;
import app.chat.service.GroupUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupUserController {

    private final GroupUserService service;

    @PostMapping("/addMember/")
    public ResponseEntity<?> addMembers(@Valid @RequestBody GroupMemberReqDto dto) {
        return service.addMembers(dto);
    }

    @GetMapping("members/{groupId}")
    public ResponseEntity<?> getSortedMembers(@PathVariable(value = "groupId") Long groupId) {
        return service.getSortedMembers(groupId);
    }

    @PostMapping("/join/username/{groupUsername}")
    public ResponseEntity<?> joinGroupByUsername(@PathVariable String groupUsername) {
        return service.joinByUsername(groupUsername);
    }

    @PostMapping("/join/link/{groupLink}")
    public ResponseEntity<?> joinGroupByLink(@PathVariable String groupLink) {
        return service.joinByPrivateLink(groupLink);
    }
}
