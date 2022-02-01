for i in {0..99} # here put as extremes 0 and the total number of queries we have to execute -1 
do 
	for j in {0..9} # we decided to run each query 10 times 
	do
		java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/wholedb/RunOnWholeDB $i $j
	done
done