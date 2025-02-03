#!/bin/bash

sequenceNumberCalls=100000

rm -f out.* err.*

for size in 2 3 5 9 13 16
do
  echo running size $size
  srun -N $size run.sh $sequenceNumberCalls > out.$size 2> err.$size
done
