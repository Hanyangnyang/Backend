package life.hanyang.core.notice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "notice_target")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeTarget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Notice notice;
    @Column(nullable = false, length = 50)
    private String page;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, List<String>> conditions;

    public NoticeTarget(Notice notice, String page, Map<String, List<String>> conditions) {
        this.notice = notice;
        this.page = page;
        this.conditions = conditions.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }
}
