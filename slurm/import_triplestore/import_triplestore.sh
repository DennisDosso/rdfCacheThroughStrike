#!/bin/bash

# invokes the code the import a triplestore
# needs to be called from another bash command to pass 
# the path of the directory with the jar and the property file with
# the required paths

java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* setup/ImportDatabase ${2}