### Run Local Test Instance
```shell

# Run Test Instance using local storage
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test,local

# Run Test Instance using s3 storage
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test,s3
```

### Some Local Test Commands
```shell
# 7mb file
time curl -v 'http://localhost:8080/raw/apache/nifi/2.9.0/minifi-toolkit-2.9.0-bin.zip' | md5sum

# 200mb file
time curl -v 'http://localhost:8080/raw/apache/nifi/2.9.0/minifi-2.9.0-bin.zip' | md5sum

# 800mb file
time curl -v 'http://localhost:8080/raw/apache/nifi/2.9.0/nifi-2.9.0-bin.zip' | md5sum

# Repo that doesn't exist
time curl -v 'http://localhost:8080/raw/fake_repo/unknown_file.zip' | md5sum

# File that doesn't exist
time curl -v 'http://localhost:8080/raw/apache/unknown_file.zip' | md5sum
```

### Test Debian Cache in Docker Container
```shell
# Run Container
docker run -it --rm --add-host=host.docker.internal:host-gateway debian bash -c 'sed -i "s~deb.debian.org~host.docker.internal:8080/raw/debian~g" /etc/apt/sources.list.d/debian.sources && time sh -c "apt update && apt dist-upgrade -y && apt install openjdk-25-jdk -y" && exit'
```

### Build
```shell
# Java Image
./mvnw clean spring-boot:build-image -DskipTests | tee spring-boot-build.log

# Native Image
./mvnw clean spring-boot:build-image -Pnative  -DskipTests | tee spring-boot-build.log
```

### Run in Docker Compose
```shell
# Run using local disk storage
docker compose down --volumes && docker compose -f ./docker-compose.yml -f ./docker-compose-local.yml up

# Run using local s3 storage
docker compose down --volumes && docker compose -f ./docker-compose.yml -f ./docker-compose-s3.yml up
```

### TODO
* ~~Implement file checksum on successful download to allow file integrity checks~~
* ~~Store file size in database~~
* ~~Implement pass through streaming so the client isn't waiting for the download to finish on the server before it an download~~
* ~~Implement S3 Cache~~
* Implement file integrity checks
    * Verify files the database thinks are cached actually are
    * Verify files exist before attempting to download
    * Cleanup files that exist in storage but aren't in the database
    * Cleanup files that exist in the database but aren't in storage
* Add admin rest endpoints
    * Get lists of what is cached
    * Mark file as not cached and optionally remove from storage