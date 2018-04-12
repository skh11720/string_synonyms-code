#!/bin/bash
echo JoinHybridAll with -K 1 -qSize 3 -sample 0.01 logging in logs/JoinHybridAll__K_1__qSize_3__sample_0.01
java -Xmx6G -Xms4G -cp ../target/Synonym.jar snu.kdd.synonym.synonymRev.App \
-dataOnePath data_store/data/1000000_5_100000_1.0_0.0_1.txt -dataTwoPath data_store/data/1000000_5_100000_1.0_0.0_2.txt -rulePath data_store/rule/30000_2_2_300000_0.0_0.txt -outputPath output \
-algorithm JoinHybridAll -oneSideJoin True -upload True \
-additional "-K 1 -qSize 3 -sample 0.01"  | tee  logs/JoinHybridAll\__K_1__qSize_3__sample_0.01

exit "${PIPESTATUS[0]}"
