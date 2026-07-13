#!/bin/bash
module load java/jdk-11

sequenceNumberCalls=10000
reps=10
sh build.sh
rm -f out.* err.*
rm results.csv

for n in $(seq 1 $reps)
do
  echo === $n ===
  for size in $(seq 2 11)
  do
    echo running size $size
    srun -N $size run_java.sh $sequenceNumberCalls > out.$size 2> err.$size &
  done
  wait

  # set header
  if [ $n -eq 1 ]
  then
    cat out.* | grep Clients | head -n 1 > results.csv
  fi
  # gather results to file
  sh gather.sh
  # cleanup
  rm out.* err.*

done

sed -i -e 's/Array/Vector/g' -e 's/Complex/Hashmap/g'  results.csv 
sed -i -e 's/Array/Vector/g' -e 's/Complex/Hashmap/g'  results_ib.csv 

mv results.csv results_java.csv
mv results_ib.csv results_java_ib.csv

cp results*.csv ../../rrmi
