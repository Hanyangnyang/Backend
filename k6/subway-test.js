import http from 'k6/http';
import { check } from 'k6';

// 1. 테스트 설정 옵션
export const options = {
    vus: 100,          // 가상 유저 100명 동시 접속
    duration: '10s',    // 10초 동안 최대 부하 유지

    thresholds: {
        http_req_failed: ['rate<0.01'],     // 요청 실패율 1% 미만
        http_req_duration: ['p(95)<50'],   // 상위 95% 요청은 50ms 이하여야 함 (p95 < 50ms)
    },
};

// 2. 가상 유저가 반복 수행할 동작
export default function () {
    // 지하철 시간표 조회 API (한대앞역 HANDAEAP 기준 캐시 테스트)
    const url = 'http://localhost:8080/api/v1/subway/schedule?subwayStation=HANDAEAP';

    const res = http.get(url);

    // 응답 상태 코드가 200(정상)인지 검증
    check(res, {
        'is status 200': (r) => r.status === 200,
    });
}
