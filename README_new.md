# Project: Credit to RDF data to automatically generate a subgraph

Here a list of operations frequently done to run the experiments, step by step.

## PREPROCESSING

### Step 1: create the database BSBM

The very first step is to create a database. Here we used BSBM. If you have the jar, the command to generate the data
is the following (it creates a text file containing triples composing the database):
<pre>
java -cp bsbm.jar benchmark/generator/Generator -s ttl -fn dataset100M -pc 284826
</pre>
(you need the BSBM jar for this)

For some more information go to their website: http://wbsg.informatik.uni-mannheim.de/bizer/berlinsparqlbenchmark/spec/Dataset/index.html

pc is the number of products.
* -pc 200 generates 75.000 triples. .
* -pc 300 generates around 100K triples
* -pc 666 generates 250K triples
* -pc 2785 generates around 1M triples
* -pc 70812 generates 25M triples
* -pc 284826 generates 100M triples


### Step 2: split the database in sub-parts if it is too big

This code in particular generates files that are made of around 250K lines.

<pre>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* setup/SplitDatasetFile
</pre>

properties to set (paths.properties):
* rdfFilePath: the path of big file to be split
* ttlFilesDirectory: the path of the directory where to write the triples'

NB: you should not import only part of the files generated in this way, since there are some of these files that contain
properties that are not present in other parts.


### Step 3: import the database into a triplestore on disk (using rdf4j)

<pre>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* setup/ImportDatabase
</pre>

Properties to set:

* ttlFilesDirectory: where to find the file (turtle) to import
* databaseIndexDirectory: directory where to save the on-disk Database

### Setp 4: create the values for the queries

### Step 5: use the query values to build the files containing the queries

We need to print queries of type select and construct. The first ones are needed to check the time required by the system.
The second ones are required to work with the credit distribution strategy and the creation of the cache.
This step builds two files with a list of queries. Each query in one file has its correspondent one in the other file.
The order is important, i.e., the first select query in the first file has its corresponding construct version in the first
query of the second file.

In order to work, this class needs another file that contains the information about the values being used.
That is, each query can be seen as a string with some "%s" to be filled with values. Changing these values, the query changes.
The structure of the query is the same, but the query in itself changes.


Run:
<code>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* query_generation/GenerateQueries
</code>

Properties to set:
* outputSelectQueryFile: where to save the SELECT queries
* outputConstructQueryFile: where to save the CONSTRUCT queries
* queriesToCreate: how many queries to create
* databaseIndexDirectory: directory where the on-disk database is stored
* buildingQueryValuesPath: file containing the query values to be used to generete the queries. NB: it is
  neessary that this file contains the correct values for the class of query being considered at the time of
  execution

* whichQueryTypeToCreate: a string identifying the query type we want to create. This can have one of the following values
  (or no query will be built):
    * ONE: class 1 of the BSBM queries
    * TWO: class 2 of the BSBM queries
    * FIVE
    * SIX
    * SEVEN
    * EIGHT
    * TEN
* alpha: parameter to decide the normal distribution that generates the queries. The bigger, the more concentrated around the mean (20) 


## RUNNING QUERIES

### Step 1: running queries on the whole DB (no cache)

<pre># ${1} is the path of the directory where the jar is stored
# ${2}: -MP path of the "master directory", where all the files of the experiments regarding the current database are stored 
# ${3}: -VP path of the values.properties file (-VP)

for i in {0..2} # number of queries we perform
do
for j in {0..2} # each query is run 10 times
do
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/wholedb/RunOnWholeDBwParam -QN $i -ET $j -MP ${2} -VP ${3} -QC 1;
done
done
</pre>

Observe that the parameters to set are:

${1}: the path of the directory containing the jar file

${2}: -MP path of the "master directory", where all the files of the experiments regarding the current database are stored.
This directory needs to have the following structure:
* building_query_values: directory containing the files with the values used to build our queries
* cache: the directory containing the cache on disk
* db: the directory containing the whole database on disk
* queries: directory containing the files with the queries
* results: where we save the results
* turtle: file containing the database in .ttl format 


### Step 2: running the queries using the cache

We need to use a bash script to do so:

<pre>
#!/bin/bash

# first: clean cache and supporting database
java -cp ${1}/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/lib/* batch/cache/CleanThingsUp
echo carried out cleaning operations

# now we actually run the bash command
for i in {0..99} do # here the number of queries we are doing
	for j in {0..9} do # the number of executions
		java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/cache/RunWithCache $i $j
	done
done
</pre>