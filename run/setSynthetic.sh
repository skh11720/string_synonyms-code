#!/bin/bash

nTokens=1000000
nRecords=1000000
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

CLASSPATH=

dir=tmp
mkdir $dir
echo java -cp SynonymOpt.jar generator.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0
java -cp SynonymOpt.jar generator.Generator -r $nTokensInRule $avgLhsLen $avgRhsLen $nRules 0
echo java -cp SynonymOpt.jar generator.Generator -d rule $nTokens $avgRecLen $nRecords $skewZ $ratio
java -cp SynonymOpt.jar generator.Generator -d rule $nTokens $avgRecLen $nRecords $skewZ $ratio
echo cp rule $dir"/"
cp rule $dir"/"
echo cp data $dir"/"
cp data $dir"/"
echo java -cp SynonymOpt.jar generator.Generator -d rule $nTokens $avgRecLen $nRecords $skewZ $ratio
java -cp SynonymOpt.jar generator.Generator -d rule $nTokens $avgRecLen $nRecords $skewZ $ratio
echo cp data $dir"/"
mv data $dir"/"data2
