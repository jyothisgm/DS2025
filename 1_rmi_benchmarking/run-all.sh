#!/bin/bash

sequenceNumberCalls=100000
reps=5
sh build.sh
rm -f out.* err.*


for n in $(seq 1 $reps)
do
  for size in $(seq 2 11)
  do
    echo running size $size
    srun -N $size run.sh $sequenceNumberCalls > out.$size 2> err.$size
  done

  # set header
  if [ $n -eq 1 ]
  then
    cat out.* | grep Clients | head -n 1 > results_ib.csv
  fi
  # gather results to file
  sh ib_gather.sh
  # cleanup
  rm -f out.* err.*
done