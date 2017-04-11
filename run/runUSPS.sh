#!/bin/bash


LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 1000 )
SIZES=( 1000 3000 10000 30000 100000 300000 1000000 )

RUN_Naive1=True
#RUN_Naive1=False

#RUN_Naive2=True
RUN_Naive2=False

RUN_SIJoin=True
#RUN_SIJoin=False

RUN_JoinMin=True
#RUN_JoinMin=False

RUN_JoinMH=True
#RUN_JoinMH=False

RUN_JoinHybridOpt=True
#RUN_JoinHybridOpt=False

RUN_JoinHybridThres=True
#RUN_JoinHybridThres=False

RUN_DEBUG=True
#RUN_DEBUG=False

for SIZE in ${SIZES[@]};
do
	./setUSPS.sh $SIZE

	project=usps_$SIZE
	inputfile_one=data_store/JiahengLu/splitted/$SIZE/data.txt
	inputfile_two=data_store/JiahengLu/splitted/$SIZE/data.txt
	rulefile=data_store/JiahengLu/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt $RUN_JoinHybridThres $RUN_DEBUG
done
