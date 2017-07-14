inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
project=$7
oneSide=$8
split=$9
UPLOAD=${10}

ADDITIONAL="-1"

ALG=JoinNaive

if [[ $# -ne 10 ]];
then
	echo illegal number of parameters: [$ALG]
	echo 1 $1
	echo 2 $2
	echo 3 $3
	echo 4 $4
	echo 5 $5
	echo 6 $6
	echo 7 $7
	echo oneSide $oneSide
	echo split $split
	echo UPLOAD $UPLOAD
else
	if [[ "$split" = true ]];
	then
		echo $ALG SP with $ADDITIONAL logging in $logdir"/"$project\_$ALG\_SP
		time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide -split -upload $UPLOAD \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_SP
	else
		echo $ALG with $ADDITIONAL logging in $logdir"/"$project\_$ALG
		time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide -upload $UPLOAD \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG
	fi
fi
