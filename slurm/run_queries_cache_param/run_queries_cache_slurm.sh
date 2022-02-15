#!/bin/bash
# NB: put #!/bin/bash also in the other .sh file!!!!!!!!!!!!

# from terminal, to run this, do:
# cd /nfsd/gracedata/dosso/creditToRDF/target/slurm/run_queries_cache
# chmod 755 *.*

# ----- base directories (you do not need to change these) ----- #
BASEDIR=/ssd/data/dosso/creditToRDF 			# base directory where we operate
JARDIR=${BASEDIR}/target 						# where the jar is located
OUTPUTDIR=${BASEDIR}/outlog 					# where to put the logs
PATHPROP=${JARDIR}/properties/paths.properties  # properties file with the paths
VALPROP=${JARDIR}/properties/values.properties  # properties file with the values
MASTERDIR=${BASEDIR}/bsbm/250K					# directory where the data about a database is stored

# ----- current directory ----- #
SLURMDIR=${JARDIR}/slurm/run_queries_cache_param

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail to which send info (put NONE when you debug)
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # Number of times this one task is repeated
CPTK=1  							   # number of CPUs dedicated to this task

TIME="3:00:00"						   # Max time allowed to the task. Here 1 hour. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G								   # Allowed RAM. Formato in K, M, G e T.

JBNM=cacheRDF								# name of the job
FOUT="${OUTPUTDIR}/run_queries_cache.txt" 			# file di standard output
FERR="${OUTPUTDIR}/err_run_queries_cache.txt" 		# file di standard error

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
srun ${SLURMDIR}/run_queries_cache.sh ${JARDIR} ${MASTERDIR} ${VALPROP};
EOT
