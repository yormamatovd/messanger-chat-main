package app.chat.model.req;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddContactDto {
    private String email;
    private String name;
}
