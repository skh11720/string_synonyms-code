#!/bin/bash

project=usps

inputfile_one=data_store/JiahengLu/data.txt
inputfile_two=data_store/JiahengLu/data.txt
rulefile=data_store/JiahengLu/rule.txt
outputPath=output

LIBS=../target/Synonym.jar

dir=logs

./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project

echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }


for j in {1..1..1}; do
	./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j $project

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

