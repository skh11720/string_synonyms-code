inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
j=$7

ADDITIONAL="-n $j -compact -v TopDownHashSetSinglePathDS 0"

echo JoinMH with $ADDITIONAL logging in $dir"/"aolJoinD2GramCompactTopDownHashSet
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
	-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
	-algorithm JoinMH \
	-additional "$ADDITIONAL" > $dir"/"aolJoinD2GramCompactTopDownHashSet

