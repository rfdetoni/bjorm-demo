# The BJORM source is pinned to an exact upstream revision. No GitHub Packages token is needed.
FROM maven:3.9-eclipse-temurin-25 AS build
ARG BJORM_REF=119c1ead370a851f17625e24c4c700f5cf9af652
WORKDIR /build
RUN git init /build/bjorm && cd /build/bjorm && \
    git remote add origin https://github.com/rfdetoni/bjorm.git && \
    git fetch --depth 1 origin "${BJORM_REF}" && git checkout --detach FETCH_HEAD && \
    mvn -B -ntp -pl bjorm-core,bjorm-processor,bjorm-spring-boot -am -DskipTests install
COPY pom.xml ./demo/pom.xml
COPY src ./demo/src
RUN mvn -B -ntp -f /build/demo/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/demo/target/bjorm-demo-*.jar /app/app.jar
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
