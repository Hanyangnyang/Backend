import http from 'k6/http';
import { check } from 'k6';

// 1. 테스트 설정 옵션
export const options = {
    vus: 100,          // 가상 유저(Virtual Users) 100명 동시 접속
    duration: '10s',    // 10초 동안 최대 부하 유지

    thresholds: {
        http_req_failed: ['rate<0.01'],     // 요청 실패율 1% 미만
        http_req_duration: ['p(95)<50'],   // 상위 95% 요청은 50ms 이하여야 함 (p95 < 50ms)
    },
};

// 2. 가상 유저가 반복 수행할 동작
export default function () {
    // 로컬에 띄운 유저 API 학식 조회 주소 (필터 생략 시 D-7 ~ D+7 기본 캐시 작동)
    const url = 'http://localhost:8080/api/v1/menu';

    const res = http.get(url);

    // 응답 상태 코드가 200(정상)인지 검증
    check(res, {
        'is status 200': (r) => r.status === 200,
    });
}
