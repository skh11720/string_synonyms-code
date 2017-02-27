#!/bin/bash

inputfile_one=data_store/aol/1M.txt
inputfile_two=data_store/aol/1M.txt
rulefile=data_store/wordnet/rules.noun
outputPath=output

#nTokens=1000000
#nRecords=500000
#nRules=200000
#nTokensInRule=30000
#avgRecLen=5
#avgLhsLen=2
#avgRhsLen=2
#skewZ=1
#ratio=0

#./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

LIBS=../target/Synonym.jar

dir=logs

./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS

echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }


for j in {1..1..1}; do
	./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j

	echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinD2GramCompact$j"TopDownHashSet"; }
done

#echo java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.HybridA1 $inputfile_one $inputfile_two $rulefile
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt5.txt -joinExpandThreshold 10 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T10CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt6.txt -joinExpandThreshold 100 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T100CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt7.txt -joinExpandThreshold 1000 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T1000CompactTopDownHashSet; }
