#!/bin/bash


LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 1000000 )
#SIZES=( 158489 251188 466158 )
#SIZES=( 100000 )
#SIZES=( 63095 )
#SIZES=( 158489 )
#SIZES=( 15848 )
SIZES=( 10000 15848 25118 39810 63095 100000 )
#SIZES=( 10000 15848 25118 39810 63095 100000 158489 251188 466158 )

oneSide=$1
UPLOAD=$2


RUN_Naive=True
#RUN_Naive=False

#RUN_JoinNaiveSP=True
RUN_JoinNaiveSP=False

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

for SIZE in ${SIZES[@]};
do
	./setSPROT.sh $SIZE

	project=sprot_$SIZE
	inputfile_one=data_store/sprot/splitted/SPROT_$SIZE.txt
	inputfile_two=data_store/sprot/splitted/SPROT_$SIZE.txt
	rulefile=data_store/sprot/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_JoinNaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt $RUN_JoinHybridThres $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide $UPLOAD

	if [[ $UPLOAD == "True" ]];
	then
		./upload.sh
	fi
done
