package app.chat.model.resp.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPublicRespDto {
    private String firstName;
    private String lastName;
    private String username;
    private String bio;
}
