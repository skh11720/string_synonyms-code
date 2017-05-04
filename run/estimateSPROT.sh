#!/bin/bash


LIBS=../target/Synonym.jar

dir=logs

SIZES=( 1000000 )
#SIZES=( 1000 3000 10000 30000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 1000000 )

for SIZE in ${SIZES[@]};
do
	./setSPROT.sh $SIZE

	project=sprot_$SIZE
	inputfile_one=data_store/sprot/splitted/SPROT_$SIZE.txt
	inputfile_two=data_store/sprot/splitted/SPROT_$SIZE.txt
	rulefile=data_store/sprot/rule.txt
	outputPath=output

	./EstimateNaive.sh $inputfile_one $inputfile_two $rulefile  output logs $LIBS EST_SPROT
	./EstimateJoinMin.sh $inputfile_one $inputfile_two $rulefile  output logs $LIBS 2 EST_AOL
done
