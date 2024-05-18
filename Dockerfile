FROM gradle:jdk21 as builder

COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts /home/gradle/src/
COPY --chown=gradle:gradle gradle /home/gradle/src/gradle

COPY --chown=gradle:gradle src /home/gradle/src/src

WORKDIR /home/gradle/src

RUN gradle build --no-daemon

FROM openjdk:21-slim

COPY --from=builder /home/gradle/src/build/libs/*.jar /app/server-websocket.jar

EXPOSE 3000

CMD ["java", "--enable-preview", "-jar", "/app/server-websocket.jar"]
