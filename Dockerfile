FROM eclipse-temurin:21.0.2_13-jdk-alpine as build

WORKDIR /mariee

COPY ./ ./

RUN chmod +x ./mvnw

ENV PROXY_HOST= PROXY_PORT=

ENV MAVEN_OPTS="-DsocksProxyHost=$PROXY_HOST -DsocksProxyPort=$PROXY_PORT -Dmaven.repo.local=/cache/.m2"

RUN --mount=type=cache,target=/cache/.m2 ./mvnw package

FROM eclipse-temurin:21.0.2_13-jre-alpine

WORKDIR /app

COPY --from=build /mariee/target/mariee-1.0.0.jar ./mariee.jar

COPY --from=build /mariee/data/hkipc.data.json ./data/

ENV TZ=Asia/Shanghai

EXPOSE 8080

CMD java -jar -Dfile.encoding=utf-8 --enable-preview ./mariee.jar

# docker build -t mariee-api:latest .

# docker run -d --name mariee-api -p 8080:8080 mariee-api:latest
