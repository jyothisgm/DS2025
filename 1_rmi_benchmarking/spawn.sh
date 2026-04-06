args=("$@")
num=${args[0]}
for i in $(seq 1 $num);
do  
    nohup bash -c 'java -cp dist/slf4j-api-2.0.16.jar:dist/slf4j-simple-2.0.16.jar:dist/dspa1.jar:. ds.pa1.Client' & 
done
