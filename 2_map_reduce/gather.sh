# cat out.* | awk 'BEGIN{IGNORECASE=1} /^node,type/ {flag=1} flag && /^[[:space:]]*node/' >> results.csv
# cat out.* | grep -A 1 '^Node,Type' | head -n 2 > results.csv
cat out.* | grep Node | head -n 1 > results.csv
cat $(ls out* | sort -V) | grep -A1 --no-group-separator  Node | grep -v Node  >> results.csv