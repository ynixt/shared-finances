FROM eclipse-temurin:20.0.2_9-jdk-alpine as build

WORKDIR /workspace/app

COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN ./gradlew bootJar

FROM eclipse-temurin:20.0.2_9-jdk-alpine

VOLUME /tmp

COPY --from=build /workspace/app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
