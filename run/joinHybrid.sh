inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
sample=$7
project=$8

ADDITIONAL="-s $sample -compact -v TopDownHashSetSinglePathDS 0"

if [[ $# -ne 8 ]];
then
	echo 'illegal number of parameters [joinHybrid]'
	echo 1 $1
	echo 2 $2
	echo 3 $3
	echo 4 $4
	echo 5 $5
	echo 6 $6
	echo 7 $7
	echo 8 $8
else
	echo JoinHybrid with $ADDITIONAL logging in $logdir"/"$project\_JoinHybrid\_$sample
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm JoinHybrid \
		-additional "$ADDITIONAL" > $logdir"/"$project\_JoinHybrid\_$sample
fi

