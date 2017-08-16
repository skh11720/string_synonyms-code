#!/bin/bash

./package.sh

oneSide=True
#oneSide=False

SIZE=10000

#inputfile_one=data_store/JiahengLu/splitted/USPS_$SIZE.txt
#inputfile_two=data_store/JiahengLu/splitted/USPS_$SIZE.txt
#rulefile=data_store/JiahengLu/USPS_rule.txt

inputfile_one=data_store/sprot/splitted/SPROT_$SIZE.txt
inputfile_two=data_store/sprot/splitted/SPROT_$SIZE.txt
rulefile=data_store/sprot/rule.txt

#inputfile_one=data_store/aol/splitted/$SIZE/aol_$SIZE\_data.txt
#inputfile_two=data_store/aol/splitted/$SIZE/aol_$SIZE\_data.txt
#rulefile=data_store/wordnet/rules.noun


outputPath=output
logdir=logs
LIBS=../target/Synonym.jar

k=2
qSize=3
#qSize=2
s=1
xmx=8G

ADDITIONAL="-K $k -qSize $qSize -sample $s"

project=Est

ALG=EstimationTest

UPLOAD=False

echo $ALG with k=$k and "$ADDITIONAL" logging in $logdir"/"$project\_$ALG\_$k\_$qSize
time java -Xmx$xmx -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.App \
	-dataOnePath $inputfile_one -dataTwoPath $inputfile_two -rulePath $rulefile -outputPath $outputPath \
	-algorithm $ALG -oneSideJoin $oneSide -upload $UPLOAD \
	-additional "$ADDITIONAL" | tee $logdir"/"$project\_$ALG\_$k\_$qSize\_$s 

