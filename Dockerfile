# 작성자 : 최산하
# 1단계: Gradle을 사용하여 애플리케이션 빌드
# Stage 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .

# 테스트는 GitHub Actions에서 이미 수행했으므로 빌드만 실행
# Tests are already run in GitHub Actions, so we skip them during Docker build
RUN gradle build --no-daemon -x test

# 2단계: 빌드된 결과물만 가져와 최종 실행 이미지 생성
# Stage 2: Create the final execution image with only the build artifacts
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정 - Asia/Seoul로 고정
# Set timezone to Asia/Seoul
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

ENV TZ=Asia/Seoul

# build 스테이지의 build/libs 폴더에서 .jar 파일을 app.jar 라는 이름으로 복사해옵니다.
# Copy the .jar file from the build stage's build/libs folder and rename it to app.jar.
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 애플리케이션이 8081 포트를 사용함을 명시합니다.
# Expose port 8081 to indicate which port the application uses.
EXPOSE 8081

# 컨테이너가 시작될 때 app.jar 파일을 실행합니다.
# Specify the command to run when the container starts.
ENTRYPOINT ["java","-jar","/app/app.jar"]