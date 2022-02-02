#!/bin/bash

# this is a bash file supposed to be called by SLURM
# in this example I just run one HelloWorld

for i in {0..5}; 
do
	for j in {0..4}; 
	do
		java -cp ${1}/target/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/target/lib/* test/printing/HelloWorld ${i} ${j};
	done;
done;