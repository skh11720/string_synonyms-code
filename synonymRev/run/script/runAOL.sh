#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 10000 )
#SIZES=( 10000 15848 25118 39810 63095 100000 158489 251188 398107 630957 1000000 )
SIZES=( 4619326 )
#SIZES=( 1000000 )

oneSide=$1

UPLOAD=$2

#RUN_Naive=True
RUN_Naive=False

#RUN_NaiveSP=True
RUN_NaiveSP=False

#RUN_JoinMHSP=True
RUN_JoinMHSP=False

#RUN_JoinMin=True
RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

#RUN_JoinMinNaive=True
RUN_JoinMinNaive=False

#RUN_JoinHybridThres=True
RUN_JoinHybridThres=False

#RUN_JoinMHNaive=True
RUN_JoinMHNaive=False

#RUN_JoinMHNaiveThres=True
RUN_JoinMHNaiveThres=False

RUN_JoinHybridAll=True
#RUN_JoinHybridAll=False

#RUN_JoinBK=True
RUN_JoinBK=False

#RUN_JoinBKSP=True
RUN_JoinBKSP=False

RUN_DEBUG=True
#RUN_DEBUG=False

for SIZE in ${SIZES[@]};
do
	./setAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/aol_$SIZE\_data.txt
	inputfile_two=data_store/aol/splitted/aol_$SIZE\_data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_NaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinMinNaive $RUN_JoinHybridThres $RUN_JoinMHNaive $RUN_JoinMHNaiveThres $RUN_JoinHybridAll $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide $UPLOAD

	if [[ $UPLOAD == "True" ]];
	then
		./upload.sh
	fi
done
