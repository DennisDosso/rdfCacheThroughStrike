#!/bin/bash

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/CleanThingsUp ${2} ${3};
echo carried out cleaning operations

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCache $i $j ${2} ${3};
	done;
done;