#!/bin/bash

inputfile_one=data_store/JiahengLu/data.txt
inputfile_two=data_store/JiahengLu/data.txt
rulefile=data_store/JiahengLu/rule.txt
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

echo H2GramNoIntvlTree logging in $dir"/"aolJoinH2GramCompactTopDownHashSet
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
	-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
	-algorithm JoinMin \
	-additional "-compact -v TopDownHashSetSinglePathDS 0" > $dir"/"aolJoinH2GramCompactTopDownHashSet

echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }


for j in {1..1..1}; do
	echo D2GramNoIntvlTree logging in $dir"/"aolJoinD2GramCompactTopDownHashSet
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm JoinMH \
		-additional "-n $j -compact -v TopDownHashSetSinglePathDS 0" > $dir"/"aolJoinD2GramCompactTopDownHashSet

	echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinD2GramCompact$j"TopDownHashSet"; }
done

#echo java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.HybridA1 $inputfile_one $inputfile_two $rulefile
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt5.txt -joinExpandThreshold 10 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T10CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt6.txt -joinExpandThreshold 100 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T100CompactTopDownHashSet; }
#{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramA1 $inputfile_one $inputfile_two $rulefile rslt7.txt -joinExpandThreshold 1000 -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logHybrid2GramA1T1000CompactTopDownHashSet; }
