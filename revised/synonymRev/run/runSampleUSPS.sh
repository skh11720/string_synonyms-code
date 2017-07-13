#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

oneSide=$1

RUN_Naive=True
#RUN_Naive=False

#RUN_NaiveSP=True
RUN_NaiveSP=False

RUN_JoinMHSP=True
#RUN_JoinMHSP=False

RUN_JoinMin=True
#RUN_JoinMin=False

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

#IDS=( 8 )
#IDS=( 10 11 )
#IDS=( 1 2 3 4 )
#IDS=( 6 7 8 9 10 11 )
IDS=( 1 2 3 4 5 6 7 8 9 10 11 )
for nId in ${IDS[@]};
do
	project=usps_sample_$nId

	inputfile_one=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
	inputfile_two=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
	rulefile=/home/kddlab/wooyekim/Synonym/JiahengLu/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_NaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt  $RUN_JoinHybridThres $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide
done
