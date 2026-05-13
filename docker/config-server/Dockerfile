FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY library-common ./library-common
COPY eureka-server ./eureka-server
COPY config-server ./config-server
COPY config-repo ./config-repo
COPY user-service ./user-service
COPY book-service ./book-service
COPY borrow-service ./borrow-service
COPY gateway-service ./gateway-service
RUN mvn -q -pl config-server -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends git curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/config-repo /git-config
WORKDIR /git-config
RUN cd /git-config && rm -rf .git 2>/dev/null || true && git init -b main && git add -A && git config user.email "config@library.local" && git config user.name "library-config" && git commit -m "init config repo for Spring Cloud Config"
WORKDIR /app
COPY --from=build /build/config-server/target/config-server-*.jar app.jar
EXPOSE 8888
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java","-jar","/app/app.jar"]
