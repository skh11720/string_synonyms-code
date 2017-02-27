inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
project=$7

ADDITIONAL="-compact -v TopDownHashSetSinglePathDS 0"

echo JoinMin with $ADDITIONAL logging in $logdir"/"$project\_JoinMin
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
	-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
	-algorithm JoinMin \
	-additional "$ADDITIONAL" > $logdir"/"$project\_JoinMin

