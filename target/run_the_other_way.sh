for i in {0..5} # number of queries
do
	java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/printutil/PrintOneLineBatch $i
	for j in {0..9} # number of times each query
	do
		java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/wholedb/ComputeOneQueryOnWholeDB $i
	done
done