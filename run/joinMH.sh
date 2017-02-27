inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
j=$7
project=$8

ADDITIONAL="-n $j -compact -v TopDownHashSetSinglePathDS 0"

if [[ $# -ne 8 ]];
then
	echo 'illegal number of parameters: [joinMH]'
	echo inputfile_one $1
	echo inputfile_two $2
	echo rulefile $3
	echo outputPath $4
	echo logdir $5
	echo LIBS $6
	echo j $7
	echo project $8
else
	echo JoinMH with j=$j and $ADDITIONAL logging in $logdir"/"$project\_JoinMH\_$j
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm JoinMH \
		-additional "$ADDITIONAL" > $logdir"/"$project\_JoinMH\_$j
fi
