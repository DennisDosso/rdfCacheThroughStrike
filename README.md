# Project: Credit to RDF data to automatically generate a subgraph

Here a list of operations frequently done to run the experiments, step by step

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
* queryBuildingValuesFile: files where to find the values that "create" the queries
* selectQueryFile: the path of the file where to print the queries (one per line)
* databaseIndexDirectory: path of the directory containing the rdf4j database.




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
* timeoutSelectQueries: the time allowed to perform a select query (in ms). Usually 30000
* timeoutConstructQueries: the time allowed to perform a construct query to compute provenance (in ms). Usually 30000
* timeoutUpdateRDB: time allowed to update the supporting relational database
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


