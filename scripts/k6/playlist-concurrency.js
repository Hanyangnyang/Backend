import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const trackId = __ENV.TRACK_ID;
const songId = __ENV.SONG_ID;
const deviceId = __ENV.DEVICE_ID;

export const options = {
  scenarios: {
    concurrent_toggle: {
      executor: 'shared-iterations',
      vus: Number(__ENV.VUS || 50),
      iterations: Number(__ENV.ITERATIONS || 100),
      maxDuration: '30s',
    },
  },
};

export default function () {
  const headers = { 'Content-Type': 'application/json' };
  const response = http.post(
    `${baseUrl}/api/v1/playlist/songs/tracks/${trackId}/like`,
    JSON.stringify({ deviceId }),
    { headers },
  );

  check(response, { '좋아요 요청 성공': (res) => res.status === 200 });

  if (songId) {
    const reactionResponse = http.post(
      `${baseUrl}/api/v1/playlist/songs/${songId}/reactions`,
      JSON.stringify({ deviceId, reactionType: 'THUMBS_UP' }),
      { headers },
    );
    check(reactionResponse, { '감정표현 요청 성공': (res) => res.status === 200 });
  }
}
