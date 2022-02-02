#!/bin/bash

# this is the script that invokes one slurm job. 
# This job runs one script, and this script does what you need


# ----- base directory ----- #
BASEDIR=/ssd/data/dosso/creditToRDF # directory where we operate

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=1  							   # number of CPUs dedicated to this task

TIME="10:00:00"						   # Max time allowed to the task. Here 10 hours. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G								   # Allowed RAM. Formato in K, M, G e T.

JBNM=hello-world-dosso_${1}_${2}								# name of the job
FOUT="${BASEDIR}/bsbm/results/outlog/hello_world.txt" 			# file di standard output
FERR="${BASEDIR}/bsbm/results/outlog/err_hello_world.txt" 		# file di standard error


# send the batch of jobs. This execution is quite peculiar, since I need a batch of only
# one job. To me it is not important to "parallelize" the execution
# but only to run one file .sh. Inside that file I'll have all that I need
sbatch \
	--mail-user=$MAIL \
	--mail-type=$MTYP \
	--ntasks=$NTSK \
	--cpus-per-task=$CPTK \
	--partition=$PRTT \
	--time=$TIME \
	--mem=$MEM \
	--job-name=$JBNM \
	--output=$FOUT \
	--error=$FERR <<EOT 
#!/bin/bash
srun ${BASEDIR}/target/slurm/hello_world.sh ${BASEDIR} $@;
EOT