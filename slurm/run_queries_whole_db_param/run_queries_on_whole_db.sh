#!/bin/bash

# ${1} is the path of the directory where the jar is stored
# ${2}: -MP path of the "master directory", where all the files of the experiments regarding the current database are stored (-MP)
# ${3}: -VP path of the values.properties file (-VP)


# one for each query class

# ----------1 

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 1;
	done
done


# ---------- 2

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 2;
	done
done


# ---------- 3

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 3;
	done
done


# ---------- 5

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 5;
	done
done


# ---------- 6

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 6;
	done
done


# ---------- 7

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 7;
	done
done


# ---------- 8

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 8;
	done
done


# ---------- 10

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 10;
	done
done

