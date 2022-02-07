#!/bin/bash

# from terminal, to run this, do:
# cd /nfsd/gracedata/dosso/creditToRDF/target/slurm/run_queries_whole_do
# chmod 755 *.*

# then: ./run_queries_whole_db_slurm.sh

# ----- base directories (you do not need to change these) ----- #
BASEDIR=/ssd/data/dosso/creditToRDF 			# base directory where we operate
JARDIR=${BASEDIR}/target 						# where the jar is located
OUTPUTDIR=${BASEDIR}/outlog 					# where to put the logs
PATHPROP=${JARDIR}/properties/paths.properties  # properties file with the paths
VALPROP=${JARDIR}/properties/values.properties  # properties file with the values

SLURMDIR=${JARDIR}/slurm/run_queries_whole_db   # where these slurm files are located

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=1  							   # number of CPUs dedicated to this task

TIME="3:00:00"						   # Max time allowed to the task. Here 1 hour. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G								   # Allowed RAM. Formato in K, M, G e T.

JBNM=run_queries_whole_db								# name of the job
FOUT="${OUTPUTDIR}/run_queries_whole_db.txt" 			# file di standard output
FERR="${OUTPUTDIR}/err_run_queries_whole_db.txt" 		# file di standard error

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
srun ${SLURMDIR}/run_queries_on_whole_db.sh ${JARDIR} ${PATHPROP} ${VALPROP};
EOT