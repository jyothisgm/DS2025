# tail -q -n 1 $(ls out*) | sort -n -t, > results.csv
cat out.* | grep Clients | head -n 1 > results.csv
cat $(ls out* | sort -V) | grep -A1 --no-group-separator  Clients | grep -v Clients  >> results.csv
