package app.chat.model.resp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserContactResp {
    private Long user1Id;
    private Long user2Id;
    private String user2Name;
}
