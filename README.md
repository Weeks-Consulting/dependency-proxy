### Run Local Test Instance
```shell
./mvnw spring-boot:test-run
```

### Some Local Test Commands
```shell
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-toolkit-2.9.0-bin.zip' > /dev/null

curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_1=hello&test_param_2=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_3=hello&test_param_4=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_5=hello&test_param_6=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_7=hello&test_param_8=world' > /dev/null
```

### Test in Docker Container
```shell
# Run Container
docker run -it --rm --add-host=host.docker.internal:host-gateway debian

# Run Inside Container
sed -i 's~deb.debian.org~host.docker.internal:8080/raw/debian~g' /etc/apt/sources.list.d/debian.sources && \
time sh -c 'apt update && apt dist-upgrade -y && apt install openjdk-25-jdk -y'
```

### TODO
* Implement file checksum on successful download to allow file integrity checks
* Store file size in database - not sure on this one yet
* Implement pass through streaming so the client isn't waiting for the download to finish on the server before it an download