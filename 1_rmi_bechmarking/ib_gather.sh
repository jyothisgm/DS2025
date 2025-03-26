# tail -q -n 1 $(ls out*) | sort -n -t, > results.csv
cat logs_ib/out.* | grep Clients | head -n 1 > results_ib.csv
cat $(ls logs_ib/out* | sort -V) | grep -A1 --no-group-separator  Clients | grep -v Clients  >> results_ib.csv
