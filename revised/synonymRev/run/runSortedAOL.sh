#!/bin/bash

LIBS=../target/Synonym.jar

dir=logs

SIZES=( 1000 )
#SIZES=( 1000 1259 1585 1995 2512 3162 3981 5012 6310 7944 10000 12589 15849 19953 25119 31623 39811 50119 63096 79433 100000 125893 158490 199527 251190 316229 398109 501190 630961 794333 1000000 )

oneSide=$1

UPLOAD=$2

#RUN_Naive=True
RUN_Naive=False

#RUN_NaiveSP=True
RUN_NaiveSP=False

#RUN_JoinMHSP=True
RUN_JoinMHSP=False

RUN_JoinMin=True
#RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

RUN_JoinMinNaive=True
#RUN_JoinMinNaive=False

#RUN_JoinHybridThres=True
RUN_JoinHybridThres=False

#RUN_JoinBK=True
RUN_JoinBK=False

#RUN_JoinBKSP=True
RUN_JoinBKSP=False

RUN_DEBUG=True
#RUN_DEBUG=False

for SIZE in ${SIZES[@]};
do
	./setSortedAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/aol_sorted_$SIZE\_data.txt
	inputfile_two=data_store/aol/splitted/aol_sorted_$SIZE\_data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_NaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinMinNaive $RUN_JoinHybridThres $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide $UPLOAD

	if [[ $UPLOAD == "True" ]];
	then
		./upload.sh
	fi
done
