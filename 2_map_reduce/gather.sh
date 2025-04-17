cat out.WordCount.* | grep Node | head -n 1 > WordCount.csv
cat $(ls out.WordCount.* | sort -V) | grep -A1 --no-group-separator  Node | grep -v Node  >> WordCount.csv

cat out.WordCountCombiner.* | grep Node | head -n 1 > WordCountCombiner.csv
cat $(ls out.WordCountCombiner.* | sort -V) | grep -A1 --no-group-separator  Node | grep -v Node  >> WordCountCombiner.csv

cat out.InvertedIndex.* | grep Node | head -n 1 > InvertedIndex.csv
cat $(ls out.InvertedIndex.* | sort -V) | grep -A1 --no-group-separator  Node | grep -v Node  >> InvertedIndex.csv
