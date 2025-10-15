# 1. 베이스 이미지 선택: Eclipse Temurin JDK 17 버전을 기반으로 합니다.
FROM eclipse-temurin:17-jdk-jammy

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 먼저 빌드에 필요한 Gradle 설정 파일들만 복사합니다.
COPY ./gradlew ./settings.gradle ./build.gradle ./

# 4. Gradle을 이용해 프로젝트의 의존성을 미리 다운로드합니다. (이 과정에서 .gradle 폴더가 생성됩니다)
RUN ./gradlew dependencies

# 5. 소스 코드 전체를 이미지 안으로 복사합니다.
COPY ./src ./src

# 6. 애플리케이션을 빌드합니다. (./gradlew build)
# --no-daemon 옵션은 CI 환경에서 빌드를 더 안정적으로 만들어줍니다.
RUN ./gradlew build --no-daemon

# 7. 애플리케이션이 사용할 포트를 외부에 알립니다.
EXPOSE 8080

# 8. 컨테이너가 시작될 때 실행할 명령어를 지정합니다.
# build/libs/*.jar 패턴을 사용하여 정확한 파일 이름에 의존하지 않도록 합니다.
ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]