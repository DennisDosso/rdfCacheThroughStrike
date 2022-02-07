#!/bin/bash

# to begin with
# cd /nfsd/gracedata/dosso/creditToRDF/target/slurm/import_triplestore
# chmod 755 *.*




# this is the script that invokes one slurm job. 
# This job runs one script, and this script does what you need


# ----- base directories ----- #
BASEDIR=/ssd/data/dosso/creditToRDF 			# base directory where we operate
JARDIR=${BASEDIR}/target 						# where the jar is located
OUTPUTDIR=${BASEDIR}/outlog 					# where to put the logs
SLURMDIR=${JARDIR}/slurm/import_triplestore     # where these slurm files are located
PATHPROP=${JARDIR}/properties/paths.properties  # properties file with the paths
VALPROP=${JARDIR}/properties/values.properties  # properties file with the values

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=1  							   # number of CPUs dedicated to this task

TIME="1:00:00"						   # Max time allowed to the task. Here 1 hour. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G								   # Allowed RAM. Formato in K, M, G e T.

JBNM=hello-world-dosso_${1}_${2}								# name of the job
FOUT="${OUTPUTDIR}/import_triplestore.txt" 			# file di standard output
FERR="${OUTPUTDIR}/err_import_triplestore.txt" 		# file di standard error


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
srun ${SLURMDIR}/import_triplestore.sh ${JARDIR} ${PATHPROP} $@;
EOT