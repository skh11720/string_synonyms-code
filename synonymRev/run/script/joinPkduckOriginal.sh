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

ADDITIONAL="-ord ${11} -theta ${12} -rc ${13} -lf ${14}"

ALG=JoinPkduckOriginal


if [[ -f xmx.txt ]];
then
	xmx=`cat xmx.txt`
else
	echo 'Make file xmx.txt with memory size'
	read -p "Press any key to continue..."
fi


if [[ $# -ne 14 ]]
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
	echo globalOrder ${11}
	echo verify ${12}
	echo useRulecomp ${13}
	echo length filter ${14}
else
	rm output/$ALG
	if [[ "$split" = true ]];
	then
		echo $ALG SP with $ADDITIONAL logging in $logdir"/"$project\_$ALG\_SP
		time java -Xmx$xmx -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide -split -upload $UPLOAD \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG\_SP
	else
		echo $ALG with $ADDITIONAL logging in $logdir"/"$project\_$ALG
		time java -Xmx$xmx -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
			-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
			-algorithm $ALG -oneSideJoin $oneSide -upload $UPLOAD \
			-additional "$ADDITIONAL" > $logdir"/"$project\_$ALG
	fi
fi
