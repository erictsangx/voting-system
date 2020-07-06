FROM openjdk:8-jdk-alpine
EXPOSE 8080
ADD /build/libs/*.war /app.war
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.war"]
