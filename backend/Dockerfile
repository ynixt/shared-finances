FROM eclipse-temurin:20.0.2_9-jdk-alpine as build

WORKDIR /workspace/app

COPY gradlew gradlew.bat /workspace/app/
COPY gradle /workspace/app/gradle
COPY build.gradle.kts settings.gradle.kts /workspace/app/

RUN apk add --no-cache bash file && \
    dos2unix gradlew && \
    chmod +x gradlew

COPY src /workspace/app/src

ENV GRADLE_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"

RUN ./gradlew bootJar

FROM eclipse-temurin:20.0.2_9-jdk-alpine

#Uncomment it if you willing to use firebase directly like is described in the doc aka README.md
#COPY src/main/resources/service-account.json /home/config/firebase/service-account.json
#ENV GOOGLE_APPLICATION_CREDENTIALS=/home/config/firebase/service-account.json

VOLUME /tmp

COPY --from=build /workspace/app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
