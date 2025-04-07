#!/bin/sh

rm -rf /var/scratch/$USER/intermediate /var/scratch/$USER/output
mkdir -p /var/scratch/$USER/intermediate
mkdir -p /var/scratch/$USER/output

java -Dfile.encoding=UTF-8 -cp dist:dist/slf4j-api-2.0.16.jar:dist/slf4j-simple-2.0.16.jar:dist/dspa2.jar ds.pa2.MapReduce ds.pa2.$1 /var/scratch/rob/books-nl /var/scratch/$USER/intermediate /var/scratch/$USER/output

# check correctness
diff -sq /var/scratch/$USER/output/final-output.txt /var/scratch/rob/$1-books-nl-output-correct.txt
