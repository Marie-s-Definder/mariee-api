FROM eclipse-temurin:21.0.2_13-jdk-alpine as build

WORKDIR /mariee

COPY ./ ./

RUN chmod +x ./mvnw

# ENV PROXY_HOST= PROXY_PORT=

ENV MAVEN_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dsun.net.client.defaultReadTimeout=5000 -Dsun.net.client.defaultConnectTimeout=500 -Dmaven.repo.local=/cache/.m2"

# RUN --mount=type=cache,target=/cache/.m2 ./mvnw package

FROM eclipse-temurin:21.0.2_13-jre-alpine

WORKDIR /app

COPY --from=build /mariee/target/mariee-1.0.0.jar ./mariee.jar

ENV TZ=Asia/Shanghai

EXPOSE 8080

CMD java -jar -Dfile.encoding=utf-8 --enable-preview ./mariee.jar
