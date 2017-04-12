#!/bin/bash

outputPath=debug.txt
LIBS=../target/Synonym.jar


SIZE=10000
inputfile_one=data_store/aol/splitted/$SIZE/data.txt
inputfile_two=data_store/aol/splitted/$SIZE/data.txt

rulefile=data_store/wordnet/rules.noun

recordId=19328

./package.sh

echo PrintRecordInfo $recordId
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.algorithm.PrintRecordInfo \
	 $rulefile $inputfile_one $inputfile_two $outputPath $recordId


