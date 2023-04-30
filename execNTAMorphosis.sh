#!/bin/bash

JAR_PATH=/home/david/Documents/NTAMorphosis/out/artifacts/SlimNTAMorphosis_jar/NTAMorphosis.jar

if [ ! -f "$JAR_PATH" ]; then
  echo "JAR file does not exist"
  exit 1
fi

model="$1"
csv="$2"
csvb="$3"

start_jar() {
  cd ~/Documents/NTAMorphosis
  java  -jar "$JAR_PATH" -all -gui=false -model=src/main/resources/$model.xml -csv=reports/$csv -csvb=reports/$csvb
}

start_jar
