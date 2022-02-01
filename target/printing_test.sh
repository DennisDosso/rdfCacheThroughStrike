for i in {0..9}
do
	java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/printutil/PrintOneLineBatch $i
done