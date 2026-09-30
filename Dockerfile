# ---- Build stage: biên dịch backend bằng chính mvnw của project (không cần
# cài Maven trong image) ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
# Bỏ qua test khi build image - test đã chạy riêng ở bước phát triển/CI,
# không cần chạy lại (và cũng tránh phải mang theo H2/test resource vào image).
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime stage: chỉ mang theo JRE + jar đã build, không mang theo Maven/JDK đầy đủ ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
