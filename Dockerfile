FROM eclipse-temurin:21-jdk-alpine

ARG APP_USER=app
ARG APP_UID=1001
ARG APP_GID=1001

RUN addgroup -g ${APP_GID} ${APP_USER} \
 && adduser -u ${APP_UID} -G ${APP_USER} -s /bin/sh -D ${APP_USER}

WORKDIR /app
COPY build/libs/*.jar app.jar

#  entrypoint 복사 및 실행권한 부여 (root 상태)
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

#  ENTRYPOINT 를 먼저 등록 (root 권한으로 실행)
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

#  그 뒤에 USER 전환 (entrypoint 스크립트가 실행된 이후에 app 유저로 동작)
USER ${APP_USER}
