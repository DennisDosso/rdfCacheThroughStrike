#!/bin/bash

# ----- base directories (you do not need to change these) ----- #
BASEDIR=/ssd/data/dosso/virtuoso	 			# base directory where we operate
JARDIR=${BASEDIR}/target 						# where the jar is located
OUTPUTDIR=${BASEDIR}/outlog 					# where to put the logs

# ----- current directory ----- #
SLURMDIR=${BASEDIR}

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=20  							   # number of CPUs dedicated to this task

TIME="24:00:00"						   # Max time allowed to the task. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=64G								   # Allowed RAM. Formato in K, M, G e T.

JBNM=bulkLoad								# name of the job
FOUT="${OUTPUTDIR}/bulk_load.txt" 			# file di standard output
FERR="${OUTPUTDIR}/bulk_load_err.txt" 		# file di standard error

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
srun ${SLURMDIR}/bulk_load.sh;
EOT