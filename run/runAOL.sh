#!/bin/bash



LIBS=../target/Synonym.jar

dir=logs

SIZES=( 10000 100000 1000000 )

for SIZE in ${SIZES[@]};
do
	./setAOL.sh $SIZE

	project=aol_$SIZE
	inputfile_one=data_store/aol/splitted/$SIZE/data.txt
	inputfile_two=data_store/aol/splitted/$SIZE/data.txt
	rulefile=data_store/wordnet/rules.noun
	outputPath=output

	./joinSI.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS sigmod13.modified.SI_Join_Modified $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS sigmod13.modified.SI_Join_Modified $inputfile_one $inputfile_two $rulefile rslt_sijoin.txt > $dir"/"logSIJoin; }
	
	./joinNaive1.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS mine.Naive1 $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive1 $inputfile_one $inputfile_two $rulefile rslt_naive.txt > $dir"/"logNaive1; }

	./joinNaive2.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile rslt_naive.txt > $dir"/"logNaive2; }

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
	done
done
