#!/bin/sh

export COMPOSE_FILE_PATH="${PWD}/target/classes/docker/docker-compose.yml"

if [ -z "${M2_HOME}" ]; then
  export MVN_EXEC="mvn"
else
  export MVN_EXEC="${M2_HOME}/bin/mvn"
fi

start() {
    docker volume create graphql-acs-volume
    docker volume create graphql-db-volume
    docker volume create graphql-ass-volume
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d
}

down() {
    if [ -f "$COMPOSE_FILE_PATH" ]; then
        docker-compose -f "$COMPOSE_FILE_PATH" down
    fi
}

purge() {
    docker volume rm -f graphql-acs-volume
    docker volume rm -f graphql-db-volume
    docker volume rm -f graphql-ass-volume
}

build() {
    $MVN_EXEC clean package
}

tail() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs -f
}

tail_all() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs --tail="all"
}

test() {
    $MVN_EXEC clean package verify -Dcode-coverage
}

case "$1" in
  build_start)
    down
    build
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  test)
    down
    test
    xdg-open target/site/jacoco-it/index.html &
    ;;
  *)
    echo "Usage: $0 {build_start|start|stop|purge|tail|test}"
esac