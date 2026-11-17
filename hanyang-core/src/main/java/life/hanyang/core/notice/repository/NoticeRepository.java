package life.hanyang.core.notice.repository;

import jakarta.persistence.LockModeType;
import life.hanyang.core.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.Instant;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // Each target is an OR branch; every key in a target must contain the selected scalar value.
    // Missing context keys produce SQL NULL and therefore fail the condition (never broaden visibility).
    String VISIBLE_FROM = """
            from notice n
            where n.enabled = true and n.published_at <= :now
              and (n.expires_at is null or n.expires_at > :now)
              and exists (
                select 1 from notice_target t where t.notice_id = n.id and t.page = :pageKey
                  and not exists (
                    select 1 from jsonb_each(t.conditions) c
                    where jsonb_exists(c.value, jsonb_extract_path_text(cast(:context as jsonb), c.key)) is not true
                  )
              )
            """;

    @Query(value = "select n.* " + VISIBLE_FROM + " order by n.published_at desc, n.id desc",
            countQuery = "select count(*) " + VISIBLE_FROM, nativeQuery = true)
    Page<Notice> findVisible(@Param("pageKey") String pageKey, @Param("context") String context,
                             @Param("now") Instant now, Pageable pageable);

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
