    # 1단계: Gradle을 사용하여 애플리케이션 빌드
    FROM gradle:8.4-jdk17-alpine AS build
    WORKDIR /home/gradle/src
    COPY --chown=gradle:gradle . .
    # '--no-daemon' 옵션으로 CI 환경에서 더 안정적으로 빌드합니다.
    RUN gradle build --no-daemon

    # 2단계: 빌드된 결과물만 가져와 최종 이미지 생성
    FROM openjdk:17-jdk-slim
    WORKDIR /app

    # build 스테이지의 build/libs 폴더에서 .jar 파일을 app.jar 라는 이름으로 복사해옵니다.
    COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

    # 애플리케이션이 8080 포트를 사용함을 명시합니다.
    EXPOSE 8080

    # 컨테이너가 시작될 때 app.jar 파일을 실행합니다.
    ENTRYPOINT ["java","-jar","/app/app.jar"]