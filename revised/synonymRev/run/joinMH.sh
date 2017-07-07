inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
k=$7
qSize=$8
project=$9
oneSide=${10}
split=${11}

ADDITIONAL="-K $k -qSize $qSize"

ALG=JoinMH

if [[ $# -ne 11 ]];
then
	echo illegal number of parameters: [$ALG]
	echo inputfile_one $1
	echo inputfile_two $2
	echo rulefile $3
	echo outputPath $4
	echo logdir $5
	echo LIBS $6
	echo k $7
	echo qSize $8
	echo project $9
	echo oneSide $oneSide
	echo split $split
else
	if [[ "$split" = true ]];
	then
		echo $ALG SP with k=$k and "$ADDITIONAL" logging in $logdir"/"$project\_$ALG\_SP\_$k\_$qSize
		time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide -split \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_SP\_$k\_$qSize
	else
		echo $ALG with k=$k and "$ADDITIONAL" logging in $logdir"/"$project\_$ALG\_$k\_$qSize
		time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_$k\_$qSize
	fi
fi
