#!/bin/sh
java -Dfile.encoding=UTF-8 -cp dist:dist/slf4j-api-2.0.16.jar:dist/slf4j-simple-2.0.16.jar:dist/dspa2.jar ds.pa2.MapReduce ds.pa2.$1 /var/scratch/rob/books-nl /var/scratch/$USER/intermediate /var/scratch/$USER/output

# check correctness
mode="$1"
if [ "$mode" == "WordCountCombiner" ]; then
  mode="WordCount"
fi
diff -sq /var/scratch/$USER/output/final-output.txt /var/scratch/rob/$mode-books-nl-output-correct.txt
