# 1단계: Gradle을 사용하여 애플리케이션 빌드
# Stage 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
# '--no-daemon' 옵션은 CI 환경에서 빌드 안정성을 높여줍니다.
# The '--no-daemon' option improves build stability in CI environments.
RUN gradle build --no-daemon

# 2단계: 빌드된 결과물만 가져와 최종 실행 이미지 생성
# Stage 2: Create the final execution image with only the build artifacts
FROM openjdk:17-jdk-slim
WORKDIR /app

# build 스테이지의 build/libs 폴더에서 .jar 파일을 app.jar 라는 이름으로 복사해옵니다.
# Copy the .jar file from the build stage's build/libs folder and rename it to app.jar.
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 애플리케이션이 8080 포트를 사용함을 명시합니다.
# Expose port 8080 to indicate which port the application uses.
EXPOSE 8080

# 컨테이너가 시작될 때 app.jar 파일을 실행합니다.
# Specify the command to run when the container starts.
ENTRYPOINT ["java","-jar","/app/app.jar"]