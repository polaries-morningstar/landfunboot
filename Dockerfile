# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 使用腾讯云 Maven 镜像，加速国内构建
COPY maven/settings.xml /root/.m2/settings.xml

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code（含 application.yml，敏感项由环境变量覆盖）
# 先复制迁移脚本，新增/修改 migration 时会失效本层缓存，确保 mvn package 重新打包
COPY src/main/resources/db ./src/main/resources/db
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port and set entry point
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
