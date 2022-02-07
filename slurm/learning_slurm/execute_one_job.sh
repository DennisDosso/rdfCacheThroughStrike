#!/bin/bash

# in questo script pongo le variabili che servono per far girare slurm. 
# prima le definisco come variabili bash, poi le passo al comando slurm

# dove operiamo
BASEDIR=/ssd/data/dosso/creditToRDF

# ---------- Job Parameters ---------- #
MAIL=dosso@dei.unipd.it                # mail a cui mandare eventuali messaggi
MTYP=NONE							   # NONE, BEGIN, END, FAIL, REQUEUE, ALL

PRTT=ims							   # partition. Dato che uso le risorse solo nostre, va ims (altrimenti allgroups)
NTSK=1 								   # numero di volte questo task viene ripetuto
CPTK=1  								# numero di CPU per 1 task

TIME="10:00:00"						   # tempo dedicato al task. Qui 10 ore. Altri formati: mm, mm:ss, hh:mm:ss, dd-hh
MEM=1G								   # memoria occupata. Formato in K, M, G e T.

JBNM=hello-world-dosso_${1}_${2}
FOUT="${BASEDIR}/bsbm/results/outlog/hello_world.txt" 			# file di standard output
FERR="${BASEDIR}/bsbm/results/outlog/err_hello_world.txt" 		# file di standard error


# now invoke sbatch to start the job. Set all the variables and run anover bash file
# we use wait because it is critical to have the jobs executed in the correct order
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
srun ${BASEDIR}/target/slurm/run_one_query.sh ${BASEDIR} $@;
EOT