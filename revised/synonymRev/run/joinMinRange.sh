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
UPLOAD=${11}

ADDITIONAL="-K $k -qSize $qSize"

ALG=JoinMinRange


if [[ -f xmx.txt ]];
then
	xmx=`cat xmx.txt`
else
	echo 'Make file xmx.txt with memory size'
	read -p "Press any key to continue..."
fi


if [[ $# -ne 11 ]];
then
	echo illegal number of parameters: [$ALG]
	echo one $inputfile_one
	echo two $inputfile_two
	echo rule $rulefile
	echo output $outputPath
	echo log $logdir
	echo LIBS $LIBS
	echo k $k
	echo qSize $qSize
	echo project $project
	echo oneSide $oneSide
	echo UPLOAD $UPLOAD
else
	rm output/$ALG
	echo $ALG with $ADDITIONAL logging in $logdir"/"$project\_$ALG\_$k\_$qSize
	time java -Xmx$xmx -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
		-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
		-algorithm $ALG -oneSideJoin $oneSide -upload $UPLOAD \
		-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_$k\_$qSize
fi

