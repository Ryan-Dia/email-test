#!/bin/sh
set -e

# (1) /maildir/new 디렉터리가 없으면 생성
mkdir -p /maildir/new

# (2) app 유저에게 소유권 넘기기
chown -R ${APP_UID}:${APP_GID} /maildir

# (3) app 유저에게 읽기/쓰기/실행 권한 부여
chmod -R u+rwx /maildir

# (3) app 유저로 애플리케이션 실행
exec su-exec ${APP_USER} java -jar /app/app.jar --spring.profiles.active=docker
