# Project: Credit to RDF data to automatically generate a subgraph

Here a list of operations frequently done to run the experiments, step by step.

## PREPRECOESSING

### Step 1: create the database BSBM

The very first step is to create a database. Here we used BSBM. If you have the jar, the command to generate the data 
is the following (it creates a text file containing triples composing the database):
<pre>
java -cp bsbm.jar benchmark/generator/Generator -s ttl -fn dataset25M -pc 70812
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

<pre>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* query_generation/FindAndPrintQueryValues
</pre>

Facultative in-line parameters:

* arg[0]: path of the property file containing the paths
* arg[1]: path of the property file containing the parameters of the execution

properties to set:
* databaseIndexDirectory: where the database is stored
* queryBuildingValuesFile: the path of the file where to write the values

* whichQueryTypeToCreate: a string specifying the type of query we need to create
* *

### Step 5: use the query values to build the files containing the queries

<code>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* query_generation/GenerateQueries
</code>

Properties to set:




## MAIN PART

## Step 1: generate queries

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


##Step2: run queries on the whole database (no optimization used)

At this point, we first want to know how much time the database that we use (in this project rdf4j) to answer
to the generated queries. In this case, in order to limit the quantity of variations that we may have in the execution,
we first of all need to run the class via batch. The class executes only 1 query an takes the time. Via batch we execute
all the queries, each time calling the class. In this way, we should remove the influence of any memory-based
caching system. We noticed, in fact, that the execution times sometime go at 1 ms when we run everything in RAM (e.g., 
with a for loop), showing that rdf4j has some memory-based caching system that, probably, uses some has-based solution
(just speculation, here). 
Also, we execute the same query 10 times. Later, to compute the time, we remove the higher and lower values and take the average 
of the remaining values. In this way we hope to obtain a value which is representative enough. Below the code that I 
used for the batch file:

Run:
<code>
java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* batch/wholedb/ComputeOneQueryOnWholeDB
</code>

Required Batch file:
<pre>
for i in {0..9} # here put as extremes 0 and the total number of queries we have to execute -1 
do 
	for j in {0..9} # we decided to run each query 10 times 
	do
		java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/wholedb/RunOnWholeDB $i $j
	done
done
</pre>

Properties to set:

In the file
<code>paths.properties</code> 

* databaseIndexDirectory: path of the directory containing the rdf4j database.
* selectQueryFile: the path of the file where to find the queries (one per line)
* wholeDbTimesFile: path of the file where to write the results of the execution on the whole database

in-line parameters:
* 1: the id of the query (a number starting from 0, going up)
* 2: the time we execute this query. In fact, usually we wxecute the same query many times, typically 10. This is a number
going from 0 to 9 usually indicating what time this is we are executing the same query. 0 means it is the first time,
1 that it is the second, etc.
* 3: path of the path.properties file. If no path is given, the default 'properties/path.properties' is used
* 4: path of the values.properties file. As above, if no path is given, the default 'properties/values.properties' is used.
This may create some FileNotFoundException if you are not careful.


### Step 3: Experiments using the cache

With this class, it is possible to run the experiments using the program that uses the cache. It is still necessary 
to use the following bash script. 

<pre>
# bash script to execute the set of queries on the cache

# first: clean the database working as cache on disk and the rdb
java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/cache/CleanThingsUp
echo carried out cleaning operations

# now we actually run the bash command
for i in {0..99} do # here the number of queries we are doing
	for j in {0..9} do # the number of executions
		java -cp ../rdfCreditRe-1.0-SNAPSHOT.jar:../lib/* batch/cache/RunWithCache $i $j
	done
done
</pre>

The properties to set are many and the following:

From the file values.properties
* cleanTheCache: set to true if we need to clean the cache at the beginning of the process
* timeoutSelectQueries: the time allowed to perform a select query (in ms). Usually 30000
* timeoutConstructQueries: the time allowed to perform a construct query to compute provenance (in ms). Usually 30000
* timeoutUpdateRDB: maximum time allowed to update the supporting relational database
* epochLength: the number of queries composing an epoch. 
* timesOneQueryIsExecuted=10 
* namedGraphName=name 
* creditThreshold=0 
* capRequired=false 
* cap=10000 
* timeframeLenght=1 
* timeframes=1 
* timeframesRequired: set to true if the experiments needs to use the cooldown strategy
* indexes: the rdf4j indexes that we are using in our database. For example: "spoc,psoc" (provide them as csv)

From the file path.properties
* databaseIndexDirectory: directory of the database
* cacheDirectory: directory of the cache
* selectQueryFile: where to take the select queries
* constructQueryFile: where to take the construct queries
* cacheTimesFile: where to write the results for the cache
* constructTimesFile: where to write the times for the construct/provenance operation
* updateRDBTimesFile: where to write the times for the operation of update of the cache and relational db
* coolDownTimesFile: where to write the times required to operate the cooldown operation
* supportTextFile: txt file where to write the construct queries that will create the provenances



Notice that in the result file of the times of the cache we write data as follows:
* a first line of the type:
<pre># QUERYNO number_of_the_query</pre>

And then csv lines with the following metadata:
* "hit" or "miss"
* the total time required for the answer
* the time required to access the cache
* the time to access the DB (if a miss, otherwise this value is 0)
* dimension of the result set (significant if we had a hit, in which case there is the possibility that 
we do not have a complete result set)


