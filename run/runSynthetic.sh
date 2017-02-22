#!/bin/bash

inputfile_one=data_store/current_data_one/data.txt
inputfile_two=data_store/current_data_two/data.txt
rulefile=data_store/current_rule/rule.txt

nTokens=1000000
nRecords=1000000
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

LIBS=../target/Synonym.jar

dir=logs

echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }
