## Dependency Proxy
When building and maintaining applications, containers, and servers it's common to need to download files from the internet. The dependency proxy sit's on your local network and caches those requests to persistent storage so subsequent requests don't have to redownload the file. It can be used for Linux, Python, Maven, and NodeJS packages as well as a virtually anything that responds to an HTTP get request. Think of it like GitLab's Dependency Proxy or Sonatype Nexus's Raw Repositories. Cached files are either stored locally in a Docker Volume or on any S3 compatible bucket. File path's can be excluded for things that shouldn't be cached and cached items are automatically deleted after a user specified amount of time.

### Run Example in Docker Compose - See `.env.sample` for a full list of configuration options.
```shell
# Run using local disk storage
docker compose -f ./docker-compose.yml -f ./docker-compose-local.yml up

# Run using local s3 storage
docker compose -f ./docker-compose.yml -f ./docker-compose-s3.yml up

# Cleanup Test Run - THIS WILL DELETE YOUR DATA
docker compose down --volumes --remove-orphans
```

### TODO
* ~~Implement file checksum on successful download to allow file integrity checks~~
* ~~Store file size in database~~
* ~~Implement pass through streaming so the client isn't waiting for the download to finish on the server before it an download~~
* ~~Implement S3 Cache~~
* Implement improved cache exclusions
    * Add a ttl for cache exclusions so they still can benefit from caching but for a much shorter duration
    * Add regular expression based exclusions - needed for NPM
* Implement regular expression based cache exclusions - Needed for NPM
* Implement file integrity checks
    * Verify files the database thinks are cached actually are
    * Verify files exist before attempting to download
    * Cleanup files that exist in storage but aren't in the database
    * Cleanup files that exist in the database but aren't in storage
* Add admin rest endpoints
    * Get lists of what is cached
    * Mark file as not cached and optionally remove from storage

### Run Local Test Instance
```shell

# Run Test Instance using local storage
./mvnw clean spring-boot:test-run -Dspring-boot.run.profiles=test,local

# Run Test Instance using s3 storage
./mvnw clean spring-boot:test-run -Dspring-boot.run.profiles=test,s3
```

### Local Test Commands
```shell
# 7mb file - ee16b346867fd028abd4e50b06d0d348
time curl -v 'http://localhost:8080/proxy/raw/apache/nifi/2.9.0/minifi-toolkit-2.9.0-bin.zip' | md5sum

# 200mb file - 5fcf2bcce742799a23db9e45cdddbec3
time curl -v 'http://localhost:8080/proxy/raw/apache/nifi/2.9.0/minifi-2.9.0-bin.zip' | md5sum

# 800mb file - bdd1d4dc244a54ce6a46e4cb406beae3
time curl -v 'http://localhost:8080/proxy/raw/apache/nifi/2.9.0/nifi-2.9.0-bin.zip' | md5sum

# Repo that doesn't exist
time curl -v 'http://localhost:8080/proxy/raw/fake_repo/unknown_file.zip'

# File that doesn't exist
time curl -v 'http://localhost:8080/proxy/raw/apache/unknown_file.zip'

# NPM Package with Complicated URL - 4645a1bd80161e333bf6442f11916a14
time curl -v 'http://localhost:8080/proxy/raw/npm/@isaacs%2ffs-minipass' | md5sum

# Test something from the NPM Registry - Using `--no-audit` as the proxy doesn't support post requests
npm config set registry=http://localhost:8080/proxy/raw/npm
npx --verbose --no-audit hello-world-npm

# Debian Packages
docker run -it --rm --add-host=host.docker.internal:host-gateway debian bash -c '
    sed -i "s~deb.debian.org~host.docker.internal:8080/proxy/raw/debian~g" /etc/apt/sources.list.d/debian.sources && \
    time sh -c "apt update && apt dist-upgrade -y && apt install openjdk-25-jdk -y" && \
    exit'

# Rocky Packages
docker run -it --rm --add-host=host.docker.internal:host-gateway rockylinux:9-minimal bash -c '
    sed -i "s~http://dl.rockylinux.org~http://host.docker.internal:8080/proxy/raw/rocky~g" /etc/yum.repos.d/rocky* && \
    sed -i "s~^mirrorlist~#mirrorlist~g" /etc/yum.repos.d/rocky* && \
    sed -i "s~^#baseurl~baseurl~g" /etc/yum.repos.d/rocky* && \
    time sh -c "microdnf  update -y && microdnf install -y java-25-openjdk-devel" && \
    exit'
```

### Build
```shell
# Java Image
./mvnw clean spring-boot:build-image -DskipTests | tee spring-boot-build.log

# Native Image
./mvnw clean spring-boot:build-image -Pnative  -DskipTests | tee spring-boot-build.log
```
