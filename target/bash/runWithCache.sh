# bash script to execute the set of queries on the cache

# first: clean the database working as cache on disk and the rdb
java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/cache/CleanThingsUp

# now we actually run the bash command
for i in {0..99} # here the number of queries we are doing 
do 
	for j in {0..9} # the number of executions (-1)
	do 
		java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/cache/RunWithCache $i $j
	done
done

	
