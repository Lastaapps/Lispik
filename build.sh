#!/bin/bash

cd lispik

chmod +x gradlew
./gradlew shadowJar

cp build/libs/lispik-1.0.0.jar ../lispik.jar

