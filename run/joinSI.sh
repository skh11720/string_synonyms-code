inputfile_one=$1
inputfile_two=$2
rulefile=$3
outputPath=$4
logdir=$5
LIBS=$6
j=$7

ADDITIONAL=""

echo SIJoin with $ADDITIONAL logging in $dir"/"$project\_SIJoin
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.driver.Driver \
	-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
	-algorithm SIJoin \
	-additional "$ADDITIONAL" > $dir"/"aolJoinD2GramCompactTopDownHashSet

