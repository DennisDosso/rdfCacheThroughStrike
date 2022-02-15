echo class 8 

JARDIR=/Users/dennisdosso/eclipse-workspace/rdfCreditRe/target
MAINDIR=/Users/dennisdosso/Documents/databases/BSBM/revival/250K
VALPROP=/Users/dennisdosso/eclipse-workspace/rdfCreditRe/properties/values.properties


java -cp ${JARDIR}/rdfCreditRe-1.0-SNAPSHOT.jar:${JARDIR}/lib/* batch/cache/cleaning/CleanThingsUpWithParameters -MD ${MAINDIR} -VP ${VALPROP};

for i in {0..199} # number of queries we perform
do 
	for j in {0..9} # each query is run 10 times
	do
		java -cp ${JARDIR}/rdfCreditRe-1.0-SNAPSHOT.jar:${JARDIR}/lib/* batch/cache/RunWithCacheWParameters -QN $i -ET $j -MD ${MAINDIR} -VP ${VALPROP} -QC 1; 
	done;
done;