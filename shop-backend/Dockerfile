FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY shop-framework/ shop-framework/
COPY shop-module-product/ shop-module-product/
COPY shop-module-member/ shop-module-member/
COPY shop-module-trade/ shop-module-trade/
COPY shop-module-system/ shop-module-system/
COPY shop-server/ shop-server/
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/shop-server/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
