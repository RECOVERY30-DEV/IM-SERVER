#!/bin/bash
# recovery-server와 같은 EC2에 IM-SERVER를 단일 컨테이너로 배포한다.
# 주의: 블루그린이 아니다 — recovery-server(8080/8081, nginx 무중단 전환)와 달리
# 이 스크립트는 컨테이너를 그 자리에서 교체하므로 재시작 중 짧은 다운타임이 있고,
# 헬스체크 실패 시에도 "이미 내려간 컨테이너"를 자동으로 되살리지 않는다.
# 실패하면 `docker compose logs app`으로 원인을 보고 수동으로 이전 TAG로 재배포할 것.
set -e

APP_DIR="/home/ec2-user/im-app"
cd "$APP_DIR"

PORT=8090

echo "1. pull latest image"
docker compose pull app

echo "2. restart container"
docker compose up -d app

echo "3. health check (port $PORT)"
READY=false
for i in $(seq 1 20); do
  sleep 3
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${PORT}/actuator/health" || echo "000")
  echo "   attempt $i/20: HTTP $STATUS"
  if [ "$STATUS" = "200" ]; then
    READY=true
    break
  fi
done

if [ "$READY" != "true" ]; then
  echo "### health check FAILED — docker compose logs app 으로 원인 확인 후 수동 롤백 필요 ###"
  exit 1
fi

echo "### deploy complete: http://<EC2 IP>:${PORT} ###"
