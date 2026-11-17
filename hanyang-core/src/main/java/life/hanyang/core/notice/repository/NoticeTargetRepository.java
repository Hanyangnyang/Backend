package life.hanyang.core.notice.repository;

import life.hanyang.core.notice.domain.NoticeTarget;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface NoticeTargetRepository extends JpaRepository<NoticeTarget, Long> {
    // Fetch the to-one parent too, so response grouping never initializes a proxy per notice.
    @Query("select t from NoticeTarget t join fetch t.notice where t.notice.id in :ids order by t.id")
    List<NoticeTarget> findForNotices(@Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true)
    @Query("delete from NoticeTarget t where t.notice.id = :noticeId")
    int deleteForNotice(@Param("noticeId") Long noticeId);
}
