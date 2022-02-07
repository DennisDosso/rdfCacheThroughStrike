#!/bin/bash

java -cp ${1}/target/rdfCreditRe-1.0-SNAPSHOT.jar:${1}/target/lib/* test/printing/HelloWorld ${2} ${3};