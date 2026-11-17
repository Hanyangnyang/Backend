package life.hanyang.core.notice.repository;

import jakarta.persistence.LockModeType;
import life.hanyang.core.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notice n where n.id = :id")
    Optional<Notice> findForUpdate(@Param("id") Long id);

    @Query("""
            select n from Notice n
            where (cast(:enabled as boolean) is null or n.enabled = :enabled)
              and (cast(:pageKey as string) is null or exists (
                select t.id from NoticeTarget t where t.notice = n and t.page = :pageKey))
            order by n.publishedAt desc, n.id desc
            """)
    Page<Notice> findForAdmin(@Param("pageKey") String pageKey, @Param("enabled") Boolean enabled, Pageable pageable);

    // A bulk parent delete delegates child cleanup to the ON DELETE CASCADE FK.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notice n where n.id = :id")
    int deleteNotice(@Param("id") Long id);
}
