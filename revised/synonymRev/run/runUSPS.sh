#!/bin/bash
oneSide=$1

LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 3000 )
#SIZES=( 1000 )
SIZES=( 1000 3000 10000 30000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 )
#SIZES=( 10000 30000 100000 300000 1000000 )
#SIZES=( 1000 3000 10000 30000 100000 300000 1000000 )

RUN_Naive=True
#RUN_Naive=False

RUN_NaiveSP=True
#RUN_NaiveSP=False

#RUN_SIJoin=True
RUN_SIJoin=False

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

#RUN_DEBUG=True
RUN_DEBUG=False

for SIZE in ${SIZES[@]};
do
	./setUSPS.sh $SIZE

	project=usps_$SIZE
	inputfile_one=data_store/JiahengLu/splitted/USPS_$SIZE.txt
	inputfile_two=data_store/JiahengLu/splitted/USPS_$SIZE\_tf.txt
	#inputfile_two=data_store/JiahengLu/splitted/USPS_$SIZE.txt
	rulefile=data_store/JiahengLu/USPS_rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_NaiveSP $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt $RUN_JoinHybridThres $RUN_JoinBK $RUN_DEBUG $oneSide
done
