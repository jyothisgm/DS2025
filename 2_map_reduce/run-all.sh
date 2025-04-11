#!/bin/bash

# for size in 2 4 8 12 16
for size in 8
do
  rm -rf /var/scratch/$USER/intermediate /var/scratch/$USER/output
  mkdir -p /var/scratch/$USER/intermediate
  mkdir -p /var/scratch/$USER/output

  srun -N $size -t 15 ./run.sh WordCount > out.WordCount.$size 2> err.WordCount.$size
done

# for size in 2 4 8 12 16
# do
#   rm -rf /var/scratch/$USER/intermediate /var/scratch/$USER/output
#   mkdir -p /var/scratch/$USER/intermediate
#   mkdir -p /var/scratch/$USER/output

#   srun -N $size -t 15 ./run.sh InvertedIndex > out.InvertedIndex.$size 2> err.InvertedIndex.$size

# done
