#!/bin/bash

JAR_PATH=/home/david/Documents/NTAMorphosis/out/artifacts/SlimNTAMorphosis_jar/NTAMorphosis.jar

if [ ! -f "$JAR_PATH" ]; then
  echo "JAR file does not exist"
  exit 1
fi

start_jar() {
  cd ~/Documents/NTAMorphosis
  java  -jar "$JAR_PATH" -all -gui=false -model=src/main/resources/Collision-avoidance.xml -csv=reports/collision/traces_collision_product.csv -csvb=reports/collision/results_bisim_product.csv
}

start_jar
