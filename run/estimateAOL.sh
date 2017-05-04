#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 100000 )
#SIZES=( 1000000 )
#SIZES=( 1500000 2000000 2500000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 1000000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 1000000 3000000 5000000 )
SIZES=( 1000 3000 10000 30000 )

for SIZE in ${SIZES[@]};
do
	./setAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/$SIZE/data.txt
	inputfile_two=data_store/aol/splitted/$SIZE/data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./EstimateNaive.sh $inputfile_one $inputfile_two $rulefile  output logs $LIBS EST_AOL
done
