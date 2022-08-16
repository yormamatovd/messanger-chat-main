package app.chat.model.req.group;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Setter
@Getter
public class GroupMemberReqDto {
    @NotNull
    private Long groupId;

    @NotNull
    private List<Long> userIdList;
}
