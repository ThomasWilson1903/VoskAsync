FROM maven:sapmachine AS builder
WORKDIR /app
COPY ./pom.xml .
COPY ./src ./src
RUN mvn clean install -DskipTests

FROM eclipse-temurin:17-jdk
# Устанавливаем FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/* \

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN ffmpeg -version
CMD ["java", "-jar", "app.jar"]