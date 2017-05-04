#!/bin/bash


LIBS=../target/Synonym.jar

dir=logs

#SIZES=( 10000 )
#SIZES=( 1000 3000 10000 30000 )
SIZES=( 1000 3000 10000 30000 100000 300000 1000000 )

for SIZE in ${SIZES[@]};
do
	./setUSPS.sh $SIZE

	project=usps_$SIZE
	inputfile_one=data_store/JiahengLu/splitted/USPS_$SIZE.txt
	inputfile_two=data_store/JiahengLu/splitted/USPS_$SIZE.txt
	rulefile=data_store/JiahengLu/USPS_rule.txt
	outputPath=output

	./EstimateNaive.sh $inputfile_one $inputfile_two $rulefile  output logs $LIBS EST_USPS
	./EstimateJoinMin.sh $inputfile_one $inputfile_two $rulefile  output logs $LIBS 2 EST_AOL
	./upload.sh
done
