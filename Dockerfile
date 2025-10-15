# 1. 베이스 이미지 선택: Eclipse Temurin JDK 17 버전을 기반으로 합니다.
FROM eclipse-temurin:17-jdk-jammy

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드에 필요한 Gradle 관련 파일 및 폴더를 모두 복사합니다.
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle

# 4. Docker 컨테이너 안에서 gradlew를 실행할 수 있도록 실행 권한을 부여합니다.
RUN chmod +x ./gradlew

# 5. Gradle을 이용해 프로젝트의 의존성을 미리 다운로드합니다.
RUN ./gradlew dependencies

# 6. 소스 코드 전체를 이미지 안으로 복사합니다.
COPY ./src ./src

# 7. 애플리케이션을 빌드합니다.
RUN ./gradlew build --no-daemon

# 8. 애플리케이션이 사용할 포트를 외부에 알립니다.
EXPOSE 8080

# 9. 컨테이너가 시작될 때 실행할 명령어를 지정합니다.
ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]