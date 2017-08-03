#!/bin/bash

nTokens=1000000
SIZES=( 10000 )
#SIZES=( 10000 15848 25118 39810 63095 100000 158489 251188 398107 630957 1000000 )

RULES=( 10000 )
#RULES=( 10000 30000 100000 300000 1000000 )
#nRules=100000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

oneSide=$1
UPLOAD=$2

LIBS=../target/Synonym.jar
CLASSPATH=../target/Synonym.jar

dir=logs

#RUN_Naive=True
RUN_Naive=False

#RUN_JoinNaiveSP=True
RUN_JoinNaiveSP=False

#RUN_JoinMHSP=True
RUN_JoinMHSP=False

RUN_JoinMin=True
#RUN_JoinMin=False

#RUN_JoinMH=True
RUN_JoinMH=False

#RUN_JoinMinNaive=True
RUN_JoinMinNaive=False

RUN_JoinMinNaive_Thres=True
#RUN_JoinMinNaive_Thres=False

#RUN_JoinBK=True
RUN_JoinBK=False

#RUN_JoinBKSP=True
RUN_JoinBKSP=False

#RUN_DEBUG=True
RUN_DEBUG=False

seedRule=0
seedDataOne=1
seedDataTwo=2

for nRecords in ${SIZES[@]};
do
	for nRules in ${RULES[@]};
	do
		./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

		project=syn_$nRecords

		inputfile_one=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne data_store`
		inputfile_two=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo data_store`
		rulefile=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cr $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule data_store`
		outputPath=output

		./runAlgorithms.sh $project $inputfile_one $inputfile_two $rulefile $outputPath $dir $RUN_Naive $RUN_JoinNaiveSP $RUN_JoinMHSP $RUN_JoinMin $RUN_JoinMH $RUN_JoinMinNaive  $RUN_JoinMinNaive_Thres $RUN_JoinBK $RUN_JoinBKSP $RUN_DEBUG $oneSide $UPLOAD

		if [[ $UPLOAD == "True" ]];
		then
			./upload.sh
		fi
	done
done
