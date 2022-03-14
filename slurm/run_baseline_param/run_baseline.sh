#!/bin/bash



# ---------- 1
echo class 1 

#first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..4} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/baseline/RunBaselineWParameters -QN $i -ET $j -MD ${2} -VP ${3} -QC 1 -S public; 
	done;
done;


# echo mixed 

# # first, clean things up (cache and database)
# java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

# for i in {0..499} # number of queries we perform
# do 
# 	for j in {0..10} # each query is run 10 times
# 	do
# 		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/baseline/RunBaselineWParameters -QN $i -ET $j -MD ${2} -VP ${3} -QC mixed -S public; 
# 	done;
# done;