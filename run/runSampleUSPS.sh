#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

oneSide=$1

RUN_Naive1=True
#RUN_Naive1=False

#RUN_Naive2=True
RUN_Naive2=False

#RUN_SIJoin=True
RUN_SIJoin=False

#RUN_JoinMin=True
RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

#RUN_JoinHybridOpt=True
RUN_JoinHybridOpt=False

#RUN_JoinHybridThres=True
RUN_JoinHybridThres=False

#RUN_DEBUG=True
RUN_DEBUG=False

#IDS=( 11 )
#IDS=( 8 9 10 11 )
IDS=( 1 2 3 4 5 6 7 8 9 10 11 )
for nId in ${IDS[@]};
do
	project=usps_sample_$nId

	inputfile_one=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
	inputfile_two=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
	rulefile=/home/kddlab/wooyekim/Synonym/JiahengLu/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt  $RUN_JoinHybridThres $RUN_DEBUG $oneSide
done
