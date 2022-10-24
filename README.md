# microservices
Code for practice from the book Hands-On Microservices with Spring Boot and Spring Cloud

**Step 1: Build all microservices**

`./gradlew clearn build`

**Step 2: Build docker images**

`docker-compose build`

**Step 3: Bring up all services i.e. all the docker containers**

`docker-compose up -d`

**Checking logs of all the containers using docker-compose**

`docker-compose logs -f`

**Testing the services**

`curl localhost:8081/product-composite/123 -s | jq .`

Note the port number is 8081 in the above url.
Also, jq is the command line utility to pretty print the curl output json.


**Final Step: Bringing down all containers.**

`docker-compose down`
