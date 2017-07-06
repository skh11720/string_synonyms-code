#!/bin/bash

outputPath=output
LIBS=../target/Synonym.jar

hybrid=False

# USPS
rulefile=$1
inputfile_one=$2

#./package.sh

echo $rulefile
echo $inputfile_one

time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.algorithm.misc.TransformDataSet\
	 $rulefile $inputfile_one 


