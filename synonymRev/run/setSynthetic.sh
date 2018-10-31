#!/bin/bash

nTokens=$1
nRecords=$2
nRules=$3
nTokensInRule=$4
avgRecLen=$5
avgLhsLen=$6
avgRhsLen=$7
skewZ=$8
ratio=$9

echo nTokens $nTokens
echo nRecords $nRecords
echo nRules $nRules
echo nTokensInRule $nTokensInRule
echo avgRecLen $avgRecLen
echo avgLhsLen $avgLhsLen
echo avgRhsLen $avgRhsLen
echo skewZ $skewZ
echo ratio $ratio

if [[ $# -ne 9 ]];
then
	echo illegal number of parameters [$ALG]
else

seedRule=0
seedDataOne=1
seedDataTwo=2

CLASSPATH=../target/Synonym.jar

dir=data_store
if [ ! -d $dir/data ]
then
	mkdir -p $dir/data
fi

if [ ! -d $dir/rule ]
then
	mkdir -p $dir/rule
fi

ruledir=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cr $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir`

if [ -f $ruledir ]
then
	echo Rule found in $ruledir
else
	echo Generating new rules in $ruledir
	echo java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir
	java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir
fi


datadir1=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir`

if [ -f $datadir1 ]
then
	echo Data one found in $datadir1
else
	echo Generating new data in $datadir1
	echo java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir
	java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir
fi


datadir2=`java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir`

if [ -f $datadir2 ]
then
	echo Data two found in $datadir2
else
	echo Generating new data in $datadir2
	echo java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir
	java -cp $CLASSPATH snu.kdd.synonym.synonymRev.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir
fi
fi
