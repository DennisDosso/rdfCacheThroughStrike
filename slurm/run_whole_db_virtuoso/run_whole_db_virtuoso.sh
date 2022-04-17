#!/bin/bash

echo select queries using Virtuoso

# ${1} needs to be the path where the executable .jar is located
# ${2} 

# echo starting cleaning...
# java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* virtuoso/CleanThingsUpWithVirtuoso -MD ${2} -VP ${3};

for i in {0..999}
do
	for j in {0..2}
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* virtuoso/RunOnWholeDBwParam -QN $i -ET $j -MD ${2} -VP ${3} -QC wiki  -S alpha;
	done;
done;
