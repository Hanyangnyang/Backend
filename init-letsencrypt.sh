#!/bin/bash

# 💡 내 도메인 정보와 이메일 설정
domains=(api.hanyang.life admin-api.hanyang.life)
rsa_key_size=4096
data_path="./data/certbot"
email="your-email@gmail.com" # 갱신 실패 알림 등을 받을 이메일
staging=0 # 0이면 진짜 인증서 발급, 1이면 테스트 발급

if [ -d "$data_path" ]; then
  read -p "기존 인증서 데이터가 존재합니다. 덮어쓰시겠습니까? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi

if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  echo "### Recommended TLS configs 다운로드 중..."
  mkdir -p "$data_path/conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
fi

echo "### 임시(Dummy) 인증서 생성 중..."
path="/etc/letsencrypt/live/$domains"
docker compose run --entrypoint \
  "openssl req -x509 -nodes -newkey rsa:$rsa_key_size -days 1\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo "### Nginx 가동 (임시 인증서 기반)..."
docker compose up --force-recreate -d nginx

echo "### 임시 인증서 삭제..."
docker compose run --entrypoint \
  "rm -Rf /etc/letsencrypt/live/$domains && \
   rm -Rf /etc/letsencrypt/archive/$domains && \
   rm -Rf /etc/letsencrypt/renewal/$domains.conf" certbot

echo "### 진짜 Let's Encrypt 인증서 발급 요청 중..."
domain_args=""
for domain in "${domains[@]}"; do
  domain_args="$domain_args -d $domain"
done

# 이메일 옵션 구성
case "$email" in
  "") email_arg="--register-unsafely-without-email" ;;
  *) email_arg="--email $email --no-eff-email" ;;
esac

# 테스트 모드 여부
if [ $staging != "0" ]; then staging_arg="--staging"; fi

docker compose run --entrypoint \
  "certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $email_arg \
    $domain_args \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --force-renewal" certbot

echo "### Nginx 리로드 (진짜 인증서 반영)..."
docker compose exec nginx nginx -s reload
