#!/bin/bash

# cd /nfsd/gracedata/dosso/creditToRDF/target/slurm/run_baseline_param
# ./run_baseline_param_slurm.sh

# ----- base directories (you do not need to change these) ----- #
BASEDIR=/ssd/data/dosso/creditToRDF 			# base directory where we operate
JARDIR=${BASEDIR}/target 						# where the jar is located
OUTPUTDIR=${BASEDIR}/outlog 					# where to put the logs
PATHPROP=${JARDIR}/properties/paths.properties  # properties file with the paths
VALPROP=${JARDIR}/properties/values.properties  # properties file with the parameters of the execution
SLURMDIR=${JARDIR}/slurm/run_baseline_param # directory where these files are located 

# ----- master directory, where we write/read. YOU NEED TO CHANGE THIS ONE
MASTERDIR=${BASEDIR}/bsbm/250K					# directory where the data about a database is stored


# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=1  							   # number of CPUs dedicated to this task

TIME="24:00:00"						   # Max time allowed to the task. Here 1 hour. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G							       # Allowed RAM. Formato in K, M, G e T.

JBNM=250Kbaseline								# name of the job
FOUT="${OUTPUTDIR}/250Kbaseline.txt" 			# file di standard output
FERR="${OUTPUTDIR}/250Kbaseline_err.txt" 		# file di standard error

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
srun ${SLURMDIR}/run_baseline.sh ${JARDIR} ${MASTERDIR} ${VALPROP};
EOT
