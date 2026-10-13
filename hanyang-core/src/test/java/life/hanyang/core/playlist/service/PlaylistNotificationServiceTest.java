package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.mockito.Mockito.*;

class PlaylistNotificationServiceTest {
    @Test void ownCommentAndReactionDoNotCreateNotifications() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new PlaylistNotificationService(jdbc);
        UUID owner = UUID.randomUUID();
        var song = PlaylistSong.builder().deviceId(owner).build();
        service.commentCreated(song, new PlaylistComment(UUID.randomUUID(), owner, null, "내 댓글"));
        service.firstReaction(song, owner);
        verifyNoInteractions(jdbc);
    }
}
