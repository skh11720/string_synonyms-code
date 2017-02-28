#!/bin/bash

project=syn

inputfile_one=data_store/current_data_one/data.txt
inputfile_two=data_store/current_data_two/data.txt
rulefile=data_store/current_rule/rule.txt
outputPath=output

nTokens=1000000
nRecords=100000
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

./joinSI.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
{ time java -Xmx8G -Xms4G -cp $LIBS sigmod13.modified.SI_Join_Modified $inputfile_one $inputfile_two $rulefile rslt_sijoin.txt > $dir"/"logSIJoin; }

./joinNaive1.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile rslt_naive1.txt > $dir"/"logNaive1; }

./joinNaive2.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile rslt_naive2.txt > $dir"/"logNaive2; }

./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project

echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }

for j in {1..1..1}; do
	./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j $project

	echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinD2GramCompact$j"TopDownHashSet"; }
done

samplings=( 0.01 0.03 0.1 0.3 )

for sampling in "${samplings[@]}"; do
	./joinHybrid.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $sampling $project

	echo java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramWithOptTheta4 $inputfile_one $inputfile_two $rulefile rslt6.txt -compact -v TopDownHashSetSinglePathDS 0 -s $sampling
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramWithOptTheta4 $inputfile_one $inputfile_two $rulefile rslt6.txt -compact -v TopDownHashSetSinglePathDS 0 -s $sampling > $dir"/"logAOLHybrid2GramWithOptTheta4_$sampling; }
#	{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt5.txt -joinExpandThreshold $threshold -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T\_$threshold\_CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt6.txt -joinExpandThreshold 100 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T100CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt7.txt -joinExpandThreshold 1000 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T1000CompactTopDownHashSet; }
done

