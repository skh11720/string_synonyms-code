inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
sample=$7
qsize=$8
project=$9

ADDITIONAL="-qSize $qsize -s $sample -v TopDownHashSetSinglePathDS 0"

ALG=JoinHybridOpt

if [[ $# -ne 9 ]];
then
	echo 'illegal number of parameters [$ALG]'
	echo 1 $1
	echo 2 $2
	echo 3 $3
	echo 4 $4
	echo 5 $5
	echo 6 $6
	echo 7 $7
	echo 8 $8
	echo 9 $9
else
	echo $ALG with $ADDITIONAL logging in $logdir"/"$project\_$ALG\_$sample
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm $ALG \
		-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_$sample
fi

