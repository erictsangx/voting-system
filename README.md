### Prerequisites
<pre>
//Redis
docker run -p 6379:6379 --name some-redis -d redis:6-alpine redis-server --appendonly yes

//Mongodb (change /tmp/dump if necessary)
docker run -v /tmp/dump:/dump -p 27017:27017 --name some-mongo -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=pass -d mongo:4.2.8-bionic

//example data must be imported for admin users and indexing (votingSystem.tar)
//test users: (admin,adminPass) (editor,editorPass)
docker exec -it some-mongo bash
mongorestore --uri="mongodb://mongoadmin:pass@localhost:27017/?authSource=admin" -d votingSystem /dump/votingSystem

</pre>

### Run from sources
<pre>
./gradlew bootRun
</pre>

### Run from docker
<pre>
//change [application.properties] in order to connect the databases:
spring.redis.host={local-ip}
spring.data.mongodb.host={local-ip}

./gradlew build
docker build -t voting-system:v1 .
docker run -p 8080:8080 --rm voting-system:v1
</pre>

### API host
http://localhost:8080

### Swagger
http://localhost:8080/swagger-ui.html


### Testing
<pre>
./gradlew test
</pre>

