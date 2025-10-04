# Используем официальный образ OpenJDK
FROM eclipse-temurin:17-jdk
# Устанавливаем FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*
# Устанавливаем рабочую директорию
WORKDIR /app
# Определяем аргумент для имени JAR-файла
ARG JAR_FILE=./target/*.jar
# Копируем файл JAR в контейнер
COPY ${JAR_FILE} app.jar
# Проверяем, что FFmpeg установился (опционально)
RUN ffmpeg -version
# Указываем команду для запуска приложения
CMD ["java", "-jar", "app.jar"]