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

ADDITIONAL="-K $k -qSize $qSize"

ALG=JoinMin

if [[ $# -ne 9 ]];
then
	echo 'illegal number of parameters: [$ALG]'
	echo 1 $1
	echo 2 $2
	echo 3 $3
	echo 4 $4
	echo 5 $5
	echo LIBS $LIBS
	echo k $k
	echo qSize $qSize
	echo project $project
	echo oneSide $oneSide
else
	echo $ALG with $ADDITIONAL logging in $logdir"/"$project\_$ALG
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm $ALG -oneSideJoin $oneSide \
		-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG
fi

