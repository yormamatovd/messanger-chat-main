package app.chat.service.impl;

import app.chat.entity.channel.ChannelUser;
import app.chat.repository.channel.ChannelUserRepo;
import app.chat.service.ChannelUserService;
import app.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChannelUserServiceImpl implements ChannelUserService {

    private final UserService userService;
    private final ChannelUserRepo channelUserRepo;

    /**
     * todo active user admin yoki owner bo`lgan kanallarda obunachilar orasida id si follower id ga teng bo`lganini olib beradi
     * @param followerId qidirilayotgan followrni ID si
     * @return ChannelUser
     */
    @Override
    public ChannelUser findMyAuthorityChannelFollower(Long followerId) {
        //todo actiUser ni admin yoki owner bo`lgan kanallari listini olib beradi
        List<ChannelUser> myAuthorityChannel = userService.getMyAuthorityChannel();

        for (ChannelUser channelUser : myAuthorityChannel) {
            List<ChannelUser> followers = channelUserRepo.findAllByChannel_IdAndActiveTrue(channelUser.getChannel().getId());
            for (ChannelUser follower : followers) {
                if (follower.getUser().isActive()) {
                    if (follower.getUser().getId().equals(followerId)) {
                        return follower;
                    }
                }
            }
        }
        return null;
    }
}
