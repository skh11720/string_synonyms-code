#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 1000000 )
#SIZES=( 1000000 )
#SIZES=( 1500000 2000000 2500000 )
#SIZES=( 30000 )
#SIZES=( 1000000 )
SIZES=( 1000 3000 10000 30000 100000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 1000000 3000000 5000000 )
#SIZES=( 1000 3000 10000 30000 100000 )

oneSide=$1

UPLOAD=$2

#RUN_Naive=True
RUN_Naive=False

#RUN_NaiveSP=True
RUN_NaiveSP=False

RUN_JoinMHSP=True
#RUN_JoinMHSP=False

#RUN_JoinMin=True
RUN_JoinMin=False

RUN_JoinMH=True
#RUN_JoinMH=False

#RUN_JoinHybridOpt=True
RUN_JoinHybridOpt=False

#RUN_JoinHybridThres=True
RUN_JoinHybridThres=False

RUN_JoinBK=True
#RUN_JoinBK=False

RUN_JoinBKSP=True
#RUN_JoinBKSP=False

#RUN_DEBUG=True
RUN_DEBUG=False

for SIZE in ${SIZES[@]};
do
	./setAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/$SIZE/aol_$SIZE\_data.txt
	inputfile_two=data_store/aol/splitted/$SIZE/aol_$SIZE\_data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_NaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt $RUN_JoinHybridThres $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide $UPLOAD

	if [[ $UPLOAD == "True" ]];
	then
		./upload.sh
	fi
done
