#!/bin/bash

# Get the number of times to execute the JAR file from the command-line argument
n="$1"
case="$2"
# Check if n is set, otherwise default to 5
if [ -z "$n" ]; then
    n=10
fi

# Loop through and execute the JAR file n times
for i in `seq $n`; do
    java -jar ~/Documents/NTAMorphosis/out/artifacts/SlimNTAMorphosis_jar/NTAMorphosis.jar -csv=$2/runs/traces_$2_$i.csv -p=../src/main/resources/$2 -csvb=$2/runs/results_bisim_$i.csv -gui=false;
    sleep 10;
done

