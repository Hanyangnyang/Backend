package life.hanyang.core.playlist.domain;

public enum ReportStatus {
    PENDING,       // ⏳ 접수됨 (운영자 확인 전)
    REVIEWED,      // ✅ 검토/조치 완료 (승인/삭제 등)
    DISMISSED      // ❌ 반려/기각 (이상 없음)
}
