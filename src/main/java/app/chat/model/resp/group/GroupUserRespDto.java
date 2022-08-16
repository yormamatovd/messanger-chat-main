package app.chat.model.resp.group;

import app.chat.enums.ChatRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupUserRespDto {

    private String firstName;

    private String lastName;

    private ChatRoleEnum role;

    private Timestamp joined;
}
