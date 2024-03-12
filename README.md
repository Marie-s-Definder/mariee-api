# Marie-E API
> a backend api service using [SpringBoot](https://spring.io/projects/spring-boot)

## Learning Cure

> See [HELP.md](./HELP.md)

## Dev Environment

### Software
* [OpenJDK JDK 21](https://jdk.java.net/21) `21.0.2`
* [IntelliJ IDEA](https://www.jetbrains.com/idea)

### Hardware
* HkVision IPC with `basic/digest` auth enabled

## Dev Procedure

1. *IDEA will setup the project for you*
1. Build Artifacts and Run
    ```
    $ docker build -t mariee-api:latest .
    $ docker run -d --name mariee-api -p 8080:8080 mariee-api:latest
    ```
    > you may want to set a proxy when fetching deps from maven central

## TODO
1. Introduce `hcnetsdk` along with `ISAPI` for controlling HkVision IPC
1. Setup DB connection with JPA now they are in a json file
1. `Droid` control functionality with TCP Socket connection
