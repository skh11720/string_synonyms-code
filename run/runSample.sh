#!/bin/bash

nTokens=1000000
IDS=( 1 2 3 4 5 6 7 8 9 10 11 )
#SIZES=( 10000000 100000000 )
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

LIBS=../target/Synonym.jar

dir=logs

#RUN_Naive1=True
RUN_Naive1=False

#RUN_Naive2=True
RUN_Naive2=False

#RUN_SIJoin=True
RUN_SIJoin=False

#RUN_JoinMin=True
RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

RUN_JoinHybridOpt=True
#RUN_JoinHybridOpt=False

RUN_JoinHybridThres=True
#RUN_JoinHybridThres=False

for nId in ${IDS[@]};
do
	project=sample_$nId

	inputfile_one=/home/kddlab/wooyekim/Synonym/AOL_Sample/sample$nId
	inputfile_two=/home/kddlab/wooyekim/Synonym/AOL_Sample/sample$nId
	rulefile=data_store/current_rule/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt  $RUN_JoinHybridThres
done
