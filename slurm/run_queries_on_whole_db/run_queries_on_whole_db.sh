for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		# ${1} is the path of the directory where the jar is stored
		# ${2} is the path of the path.properties file
		# ${3} is the path of the values.properties file
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDB $i $j ${2} ${3};
	done
done