import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 설정 옵션
export const options = {
    vus: 100,          // 가상 유저(Virtual Users) 100명
    duration: '10s',    // 10초 동안 지속 부하

    thresholds: {
        http_req_failed: ['rate<0.01'],     // 에러율 1% 미만이어야 함
        http_req_duration: ['p(95)<50'],   // 전체 요청 중 95%는 50ms 이하여야 함 (p95 < 50ms)
    },
};

// 2. 가상 유저가 실제로 반복 수행할 동작
export default function () {
    // 실제 로컬에서 돌고 있는 API 주소로 세팅합니다.
    const url = 'http://localhost:8080/api/v1/shuttle';

    const res = http.get(url);

    // 응답 상태 코드가 200(정상)인지 검증
    check(res, {
        'is status 200': (r) => r.status === 200,
    });
}
