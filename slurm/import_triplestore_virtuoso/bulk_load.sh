#!/bin/bash

#run the isql file that is present in the corresponding directory
# NB: you need to find these files in the grace version
echo here we go with the import

/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="rdf_loader_run();" &
wait 
echo import finished, performing now the commit
/ssd/virtuoso/bin/isql 1111 dba dossodba exec="checkpoint;" 

# to stop any load process:
# rdf_load_stop();
