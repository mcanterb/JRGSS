#!/bin/bash
cd Player-Core
mvn clean install
cd ../Player-Desktop
mvn clean install
cd ..

mkdir -p target
cp Player-Desktop/target/Player-Desktop-1.0-SNAPSHOT.jar ./target/


