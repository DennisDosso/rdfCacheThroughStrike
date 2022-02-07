#!/bin/bash

for i in {0..5}; 
do
	for j in {0..4}; 
	do
		echo run: ${i} ${j}
		./execute_one_job.sh ${i} ${j}
	done;
done;