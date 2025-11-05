FROM openjdk:17

WORKDIR /app

COPY ./build/libs/*.jar app.jar

EXPOSE 80

ENTRYPOINT ["java","-jar","/app/app.jar"]