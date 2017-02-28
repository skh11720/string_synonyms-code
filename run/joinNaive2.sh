inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
project=$7

ADDITIONAL=""

if [[ $# -ne 7 ]];
then
	echo 'illegal number of parameters: [SIJoin]'
	echo 1 $1
	echo 2 $2
	echo 3 $3
	echo 4 $4
	echo 5 $5
	echo 6 $6
	echo 7 $7
else
	echo SIJoin with $ADDITIONAL logging in $logdir"/"$project\_JoinNaive2
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm JoinNaive2 \
		-additional "$ADDITIONAL" > $logdir"/"$project\_JoinNaive2
fi
