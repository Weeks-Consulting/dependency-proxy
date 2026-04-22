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
```

### Test Debian Cache in Docker Container
```shell
# Run Container
docker run -it --rm --add-host=host.docker.internal:host-gateway debian bash -c 'sed -i "s~deb.debian.org~host.docker.internal:8080/raw/debian~g" /etc/apt/sources.list.d/debian.sources && time sh -c "apt update && apt dist-upgrade -y && apt install openjdk-25-jdk -y" && exit'
```

### TODO
* ~~Implement file checksum on successful download to allow file integrity checks~~
* ~~Store file size in database - not sure on this one yet~~
* ~~Implement pass through streaming so the client isn't waiting for the download to finish on the server before it an download~~
* ~~Implement S3 Cache~~