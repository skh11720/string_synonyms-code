project=$1
inputfile_one=$2
inputfile_two=$3
rulefile=$4
outputPath=$5
dir=$6
RUN_Naive1=$7
RUN_Naive2=$8
RUN_SIJoin=$9
RUN_JoinMin=${10}
RUN_JoinMH=${11}
RUN_JoinHybrid=${12}

LIBS=../target/Synonym.jar

echo "------------[run algorithms]-----------"
echo project $project
echo inputfile_one $inputfile_one
echo inputfile_two $inputfile_two
echo rulefile $rulefile
echo outputPath $outputPath
echo dir $dir
echo RUN_Naive1 $RUN_Naive1
echo RUN_Naive2 $RUN_Naive2
echo RUN_SIJoin $RUN_SIJoin
echo RUN_JoinMin $RUN_JoinMin
echo RUN_JoinMH $RUN_JoinMH
echo RUN_JoinHybrid $RUN_JoinHybrid
echo "--------------------------------------"

if [ ! -d 'logs' ];
then
	mkdir logs
fi

if [ ! -d 'json' ];
then
	mkdir json
fi

if [ ! -d 'result' ];
then
	mkdir result
fi
	
#JoinNaive1
if [[ $RUN_Naive1 == "True" ]];
then
	./joinNaive1.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS mine.Naive1 $inputfile_one $inputfile_two $rulefile -1
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive1 $inputfile_one $inputfile_two $rulefile -1 > $dir"/"logNaive1; }
fi

#JoinNaive2
if [[ $RUN_Naive2 == "True" ]];
then
	./joinNaive2.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.Naive2 $inputfile_one $inputfile_two $rulefile > $dir"/"logNaive2; }
fi

#SIJoin
if [[ $RUN_SIJoin == "True" ]];
then
	./joinSI.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS sigmod13.modified.SI_Join_Modified $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS sigmod13.modified.SI_Join_Modified $inputfile_one $inputfile_two $rulefile rslt_sijoin.txt > $dir"/"logSIJoin; }
fi

#JoinMin
if [[ $RUN_JoinMin == "True" ]];
then
	./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project
	echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile
	{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinH2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt4.txt -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinH2GramCompactTopDownHashSet; }
fi

#JoinMH
if [[ $RUN_JoinMH == "True" ]];
then
	for j in {1..1..1}; do
		./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j $project

		echo java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0
		{ time java -Xmx8G -Xms4G -cp $LIBS mine.JoinD2GramNoIntervalTree $inputfile_one $inputfile_two $rulefile rslt$j".txt" -n $j -compact -v TopDownHashSetSinglePathDS 0 > $dir"/"logJoinD2GramCompact$j"TopDownHashSet"; }
	done
fi

#JoinHybrid
if [[ $RUN_JoinHybrid == "True" ]];
then
	samplings=( 0.01 0.03 0.1 0.3 )
	for sampling in "${samplings[@]}"; do
		./joinHybrid.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $sampling $project

		echo java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramWithOptTheta4 $inputfile_one $inputfile_two $rulefile rslt6.txt -compact -v TopDownHashSetSinglePathDS 0 -s $sampling
		{ time java -Xmx8G -Xms4G -cp $LIBS mine.hybrid.Hybrid2GramWithOptTheta4 $inputfile_one $inputfile_two $rulefile rslt6.txt -compact -v TopDownHashSetSinglePathDS 0 -s $sampling > $dir"/"logAOLHybrid2GramWithOptTheta4_$sampling; }
	done
fi
