#!/bin/bash

nTokens=1000000
#SIZES=( 100 )
SIZES=( 10000 100000 )
#SIZES=( 100 1000 10000 100000 300000 1000000 1500000 2000000 3000000 10000000 )
#SIZES=( 10000000 100000000 )
#nRecords=100000
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

LIBS=../target/Synonym.jar
CLASSPATH=../target/Synonym.jar

dir=logs

#RUN_Naive1=True
RUN_Naive1=False

#RUN_Naive2=True
RUN_Naive2=False

#RUN_SIJoin=True
RUN_SIJoin=False

RUN_JoinMin=True
#RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

RUN_JoinHybridOpt=True
#RUN_JoinHybridOpt=False

RUN_JoinHybridThres=True
#RUN_JoinHybridThres=False

#RUN_DEBUG=True
RUN_DEBUG=False

seedRule=0
seedDataOne=1
seedDataTwo=2

for nRecords in ${SIZES[@]};
do
	./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

	project=syn_$nRecords

	inputfile_one=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne data_store`/data.txt
	inputfile_two=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo data_store`/data.txt
	rulefile=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cr $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule data_store`/rule.txt
	outputPath=output

	./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive1 $RUN_Naive2 $RUN_SIJoin $RUN_JoinMin $RUN_JoinMH $RUN_JoinHybridOpt  $RUN_JoinHybridThres $RUN_DEBUG
done
