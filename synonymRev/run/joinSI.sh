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

ADDITIONAL=""

ALG=SIJoin

if [[ $# -ne 10 ]];
then
	echo 'illegal number of parameters: [$ALG]'
	echo 'inputfile one' $1
	echo 'inputfile two' $2
	echo 'rulefile' $3
	echo 'output path' $4
	echo 'logdir' $5
	echo 'libs' $6
	echo 'project' $7
	echo 'oneSide' $8
	echo 'UPLOAD' $9
else
	echo $ALG with "$ADDITIONAL" logging in $logdir"/"$project\_$ALG
	time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm $ALG -oneSideJoin $oneSide -upload $UPLOAD \
		-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG
fi
