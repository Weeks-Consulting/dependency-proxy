```shell
./mvnw spring-boot:test-run

curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_1=hello&test_param_2=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_3=hello&test_param_4=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_5=hello&test_param_6=world' > /dev/null
curl -v 'http://localhost:8080/raw/apache/dist/nifi/2.9.0/minifi-2.9.0-bin.zip.asc?test_param_7=hello&test_param_8=world' > /dev/null
```