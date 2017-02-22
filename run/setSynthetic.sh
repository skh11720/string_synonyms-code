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

seedRule=0
seedDataOne=1
seedDataTwo=2

CLASSPATH=../target/Synonym.jar

dir=data_store
if [ ! -d $dir ]
then
	mkdir $dir
fi

ruledir=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cr $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir`

if [ -d ruledir ]
then
	echo Rule found in $ruledir
else
	echo Generating new rules in $ruledir
	echo java -cp $CLASSPATH snu.kdd.synonym.data.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir
	java -cp $CLASSPATH snu.kdd.synonym.data.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0 $seedRule $dir
fi

rm $dir/current_rule
ln -s ../$ruledir $dir/current_rule 


datadir1=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir`

if [ -d $datadir1 ]
then
	echo Data one found in $datadir1
else
	echo Generating new data in $datadir1
	echo java -cp $CLASSPATH snu.kdd.synonym.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir
	java -cp $CLASSPATH snu.kdd.synonym.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataOne $dir
fi

rm $dir/current_data_one
ln -s ../$datadir1 $dir/current_data_one

datadir2=`java -cp $CLASSPATH snu.kdd.synonym.data.Generator -cd $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir`

if [ -d $datadir2 ]
then
	echo Data two found in $datadir2
else
	echo Generating new data in $datadir2
	echo java -cp $CLASSPATH snu.kdd.synonym.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir
	java -cp $CLASSPATH snu.kdd.synonym.data.Generator -d $nTokens $avgRecLen $nRecords $skewZ $ratio $seedDataTwo $dir
fi

rm $dir/current_data_two
ln -s ../$datadir2 $dir/current_data_two
