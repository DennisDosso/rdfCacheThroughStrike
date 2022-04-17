#!/bin/bash

# ${1]: path of the directory where the jar file is located
# ${2}: -MD path of the "master directory", where all the files of the experiments regarding the current database are stored (-MD)
# ${3}: -VP path of the values.properties file (-VP)
# -QC: query class. Used to deal with different types of query
# -S: the schema we are using to be able to use different database schemas 


echo start wikidata with cache

java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* virtuoso/CleanThingsUpWithVirtuoso -MD ${2} -VP ${3};

for i in {0..999} # number of queries we perform
do 
	for j in {0..2} # each query is run 3 times
	do
		java -cp ${1}/rdfCreditRe-virtuoso.jar:${1}/lib/* virtuoso/RunVirtuosoCache -QN $i -ET $j -MD ${2} -VP ${3} -QC 1 -S public; 
	done;
done;


echo complete the bash execution with slurm
