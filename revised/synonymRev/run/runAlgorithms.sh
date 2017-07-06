project=$1
inputfile_one=$2
inputfile_two=$3
rulefile=$4
outputPath=$5
dir=$6
RUN_Naive=$7
RUN_NaiveSP=$8
RUN_JoinMHSP=$9
RUN_JoinMin=${10}
RUN_JoinMH=${11}
RUN_JoinHybridOpt=${12}
RUN_JoinHybridThres=${13}
RUN_JoinBK=${14}
RUN_DEBUG=${15}
oneSide=${16}

LIBS=../target/Synonym.jar

echo "------------[run algorithms]-----------"
echo project $project
echo inputfile_one $inputfile_one
echo inputfile_two $inputfile_two
echo rulefile $rulefile
echo outputPath $outputPath
echo dir $dir
echo RUN_Naive $RUN_Naive
echo RUN_NaiveSP $RUN_NaiveSP
echo RUN_JoinMHSP $RUN_JoinMHSP
echo RUN_JoinMin $RUN_JoinMin
echo RUN_JoinMH $RUN_JoinMH
echo RUN_JoinHybridOpt $RUN_JoinHybridOpt
echo RUN_JoinHybridThres $RUN_JoinHybridThres
echo RUN_JoinBK $RUN_JoinBK
echo RUN_DEBUG $RUN_DEBUG
echo oneSide $oneSide
echo "--------------------------------------"

if [[ $# -ne 16 ]];
	then
		echo 'illegal number of parameters'
	else

	PREV="None"
	if [ ! -d 'logs' ];
	then
		mkdir logs
	fi

	if [ ! -d 'json' ];
	then
		mkdir json
	fi

	if [ ! -d 'result' ];
	then
		mkdir result
	fi

	if [ ! -d 'output' ];
	then
		mkdir output
	fi

	#JoinNaive1
	if [[ $RUN_Naive == "True" ]];
	then
		date
		./joinNaive.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project $oneSide false
		date
		PREV="JoinNaive"
	fi

	#JoinNaive2
	if [[ $RUN_NaiveSP == "True" ]];
	then
		date
		./joinNaive.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project $oneSide true
		date

		./compare.sh $PREV JoinNaiveSP
		PREV="JoinNaiveSP"
	fi

	#SIJoin
	if [[ $RUN_JoinMHSP == "True" ]];
	then
		for j in {2..3..1}; do
			for q in {2..4..1};do

				date
				./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j $q $project $oneSide true
				date

				./compare.sh $PREV JoinMHSP
			done
		done
		PREV="JoinMHSP"
	fi

	#JoinMin
	if [[ $RUN_JoinMin == "True" ]];
	then
		K=( 1 2 3 4 5 )
		#K=( 1 )
		for k in "${K[@]}"; do
			for q in {1..5..1}; do
			#for q in {1..3..1}; do
				date
				./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $K $q $project $oneSide
				date
				./compare.sh $PREV JoinMin
			done
		done
		PREV="JoinMin"
	fi

	#JoinMH
	if [[ $RUN_JoinMH == "True" ]];
	then
		#for j in {1..3..1}; do
		for j in {2..3..1}; do
			for q in {2..4..1};do
			#for q in {1..3..1}; do

				date
				./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $j $q $project $oneSide false
				date
				./compare.sh $PREV JoinMH
			done
		done
		PREV="JoinMH"
	fi

	#JoinHybridOpt
	if [[ $RUN_JoinHybridOpt == "True" ]];
	then
		samplings=( 0.01 )
		#samplings=( 0.001 0.003 0.01 0.03 )
		#samplings=( 0.01 0.02 0.03 0.001 0.002 0.003 0.008 )
		#samplings=( 0.01 0.001 0.0001 100 1000 10000 )
		#samplings=( 0.001 0.003 0.01 0.03 )
		#samplings=( 0.0001 0.0003 0.001 0.003 0.01 0.03 )
		for q in {2..2..1}; do
			for sampling in "${samplings[@]}"; do
				date
				./joinHybridOpt.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $sampling $q $project $oneSide

				date

				./compare.sh $PREV JoinHybridOpt_Q
			done
		done
		PREV="JoinHybridOpt_Q"
	fi

	#JoinHybridThres
	if [[ $RUN_JoinHybridThres == "True" ]];
	then
		#thresholds=( 1 )
		#thresholds=( 100 )
		#thresholds=( 10 50 100 150 )
		#thresholds=( 10 50 100 150 500 1000 )
		thresholds=( 3 10 30 100 )
		#thresholds=( 0 1 3 10 100  )
		#thresholds=( 0 10 100 1000 10000 10000000 )
		for q in {2..2..1}; do
			for threshold in "${thresholds[@]}"; do
				date
				./joinHybridThres.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $threshold $q $project $oneSide

				date

				./compare.sh $PREV JoinHybridThres_Q
			done
		done
		PREV="JoinHybridThres_Q"
	fi

	#JoinBK
	if [[ $RUN_JoinBK == "True" ]];
	then
		K=( 1 2 3 4 5 )
		#K=( 1 )
		for k in "${K[@]}"; do
			#for q in {1..5..1}; do
			for q in {2..5..1}; do
				date
				./joinBK.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide
				./compare.sh $PREV JoinBK
			done
		done
		PREV="JoinBK"
	fi

	#JoinMH_QL
	if [[ $RUN_DEBUG == "True" ]];
	then
		#K=( 1 2 3 4 5 )
		K=( 2 3 )
		#K=( 1 )
		for k in "${K[@]}"; do
			for q in {2..4..1}; do
			#for q in {1..3..1}; do
				date
				./joinDebug.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide
				./compare.sh $PREV JoinMin_QL
			done
		done
	fi

	./upload.sh
fi
