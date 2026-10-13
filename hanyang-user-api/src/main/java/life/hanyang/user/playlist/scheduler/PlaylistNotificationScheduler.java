package life.hanyang.user.playlist.scheduler;

import life.hanyang.core.playlist.service.PlaylistNotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistNotificationScheduler {
    private final PlaylistNotificationDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000, scheduler = "playlistNotificationTaskScheduler")
    public void dispatch() {
        for (int i = 0; i < 20 && dispatcher.dispatchNext(); i++) {
            // 각 알림은 별도 트랜잭션에서 처리한다. 다른 인스턴스는 잠긴 행을 건너뛴다.
        }
    }
}
