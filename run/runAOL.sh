#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

SIZES=( 10 )
#SIZES=( 10 100 1000 10000 100000 1000000 )

RUN_Naive1=True
RUN_Naive2=True
RUN_SIJoin=True
RUN_JoinMin=True
RUN_JoinMH=True
RUN_JoinHybrid=True

for SIZE in ${SIZES[@]};
do
	./setAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/$SIZE/data.txt
	inputfile_two=data_store/aol/splitted/$SIZE/data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybrid 
done
