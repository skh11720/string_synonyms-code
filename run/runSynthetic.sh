#!/bin/bash

nTokens=1000000
SIZES=( 100 1000 10000 100000 )
#nRecords=100000
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

LIBS=../target/Synonym.jar

dir=logs

RUN_Naive1=True
RUN_Naive2=True
RUN_SIJoin=True
RUN_JoinMin=True
RUN_JoinMH=True
RUN_JoinHybrid=True

for nRecords in ${SIZES[@]};
do
	./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

	project=syn_$nRecords

	inputfile_one=data_store/current_data_one/data.txt
	inputfile_two=data_store/current_data_two/data.txt
	rulefile=data_store/current_rule/rule.txt
	outputPath=output


	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybrid 
done
