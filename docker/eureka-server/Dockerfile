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
RUN mvn -q -pl eureka-server -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/eureka-server/target/eureka-server-*.jar app.jar
EXPOSE 8761
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java","-jar","/app/app.jar"]
