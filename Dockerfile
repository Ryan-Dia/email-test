FROM eclipse-temurin:21-jdk-alpine

ARG APP_USER=app
ARG APP_UID=1001
ARG APP_GID=1001

RUN addgroup -g ${APP_GID} ${APP_USER} \
 && adduser -u ${APP_UID} -G ${APP_USER} -s /bin/sh -D ${APP_USER}

WORKDIR /app
COPY build/libs/*.jar app.jar

# entrypoint 스크립트 복사 및 실행권한 부여
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

USER ${APP_USER}

# ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=docker"]
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
