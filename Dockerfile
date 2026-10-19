# syntax=docker/dockerfile:1.7
# Exact BJORM source ref; never fetch an unpinned main in a production build.
FROM docker.io/library/maven:3.9-eclipse-temurin-25 AS build
ARG BJORM_REF=ada6a4b8a70819c16e70a049c95640d3a7ef31ba
WORKDIR /build
RUN git init /build/bjorm && cd /build/bjorm && \
    git remote add origin https://github.com/rfdetoni/bjorm.git && \
    git fetch --depth 1 origin "${BJORM_REF}" && git checkout --detach FETCH_HEAD
RUN --mount=type=cache,id=bjorm-demo-m2,target=/root/.m2/repository \
    cd /build/bjorm && mvn -B -ntp -pl bjorm-core,bjorm-processor,bjorm-spring-boot -am -DskipTests install
COPY pom.xml ./demo/pom.xml
COPY src ./demo/src
RUN --mount=type=cache,id=bjorm-demo-m2,target=/root/.m2/repository \
    mvn -B -ntp -f /build/demo/pom.xml -DskipTests package && \
    JAR="$(find /build/demo/target -maxdepth 1 -type f -name 'bjorm-demo-*.jar' -print -quit)" && \
    test -n "$JAR" && cp "$JAR" /build/app.jar

FROM docker.io/library/eclipse-temurin:25-jre AS production
WORKDIR /app
RUN groupadd --system --gid 10001 bjorm && \
    useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin bjorm
COPY --from=build --chown=10001:10001 /build/app.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
