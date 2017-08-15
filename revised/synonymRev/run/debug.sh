#!/bin/bash

outputPath=debug.txt
LIBS=../target/Synonym.jar

hybrid=False

# AOL
#SIZE=1000000
#inputfile_one=data_store/aol/splitted/$SIZE/data.txt
#inputfile_two=data_store/aol/splitted/$SIZE/data.txt
#rulefile=data_store/wordnet/rules.noun

# USPS
#SIZE=1000000
#inputfile_one=data_store/JiahengLu/splitted/USPS_$SIZE.txt
#inputfile_two=data_store/JiahengLu/splitted/USPS_$SIZE.txt
#rulefile=data_store/JiahengLu/USPS_rule.txt

# USPS sample
#nId=11
#inputfile_one=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
#inputfile_two=/home/kddlab/wooyekim/Synonym/USPS_Sample/sample$nId
#rulefile=/home/kddlab/wooyekim/Synonym/JiahengLu/rule.txt

# Removed AOL
#nId=7
#inputfile_one=/home/kddlab/wooyekim/Synonym/AOL_Sample/removed/sample${nId}removed
#inputfile_two=/home/kddlab/wooyekim/Synonym/AOL_Sample/removed/sample${nId}removed
#rulefile=/home/kddlab/wooyekim/Synonym/wordnet/deduplicated_rules.noun

#inputfile_two=data_store/data/1000000_5_3000_1.0_0.0_1/data.txt
#inputfile_one=data_store/data/1000000_5_3000_1.0_0.0_2/data.txt
#rulefile=data_store/rule/30000_2_2_200000_0.0_0/rule.txt
#inputfile_one=data_store/data/1000000_5_63095_1.0_0.0_1/data.txt
#inputfile_two=data_store/data/1000000_5_63095_1.0_0.0_2/data.txt
#rulefile=data_store/rule/30000_2_2_200000_0.0_0/rule.txt


#SPROT
SIZE=63095
#SIZE=158489
inputfile_one=data_store/sprot/splitted/SPROT_$SIZE.txt
inputfile_two=data_store/sprot/splitted/SPROT_$SIZE.txt
rulefile=data_store/sprot/rule.txt


#recordId=4840
#recordId=41505
recordId=45597

./package.sh

echo PrintRecordInfo $recordId
time java -Xmx8G -Xms4G -cp $LIBS snu.kdd.synonym.synonymRev.algorithm.misc.PrintRecordInfo \
	 $rulefile $inputfile_one $inputfile_two $outputPath $recordId 


