#!/bin/bash

# ${1]: path of the directory where the jar file is located
# ${2}: -MP path of the "master directory", where all the files of the experiments regarding the current database are stored (-MP)
# ${3}: -VP path of the values.properties file (-VP)

# ----------
# we need one execution for each query class
# ----------

# ---------- 1

echo class 1 

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 1; 
	done;
done;


# ---------- 2
echo class 2

java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 2; 
	done;
done;

# ---------- 3
echo class 3 

java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 3; 
	done;
done;

# ---------- 5

echo class 5

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 5; 
	done;
done;

# ---------- 6

echo class 6

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 6; 
	done;
done;

# ---------- 7

echo class 7

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 7; 
	done;
done;

# ---------- 8

echo class 8

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 8; 
	done;
done;

# ---------- 10

echo class 10

# first, clean things up (cache and database)
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${2} -VP ${3};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MP ${2} -VP ${3} -QC 10; 
	done;
done;
