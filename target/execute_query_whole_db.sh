for i in {0..9}
do
	java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/printutil/PrintOneLineBatch $i
	for j in {0..5}
	do
		java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/wholedb/ComputeOneQueryOnWholeDB $j
	done
done