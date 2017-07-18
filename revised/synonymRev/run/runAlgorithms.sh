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
RUN_JoinBKSP=${15}
RUN_DEBUG=${16}
oneSide=${17}
UPLOAD=${18}

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
echo RUN_JoinBKSP $RUN_JoinBKSP
echo RUN_DEBUG $RUN_DEBUG
echo oneSide $oneSide
echo UPLOAD $UPLOAD
echo "--------------------------------------"


MH_K_START=1
MH_K_END=3
MH_Q_START=1
MH_Q_END=3

MHSP_K_START=1
MHSP_K_END=3
MHSP_Q_START=1
MHSP_Q_END=3

BK_K_START=1
BK_K_END=3
BK_Q_START=1
BK_Q_END=3

BKSP_K_START=1
BKSP_K_END=3
BKSP_Q_START=1
BKSP_Q_END=3

MIN_K_START=1
MIN_K_END=3
MIN_Q_START=1
MIN_Q_END=3

if [[ $# -ne 18 ]];
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

	#JoinNaive
	if [[ $RUN_Naive == "True" ]];
	then
		date
		./joinNaive.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project $oneSide false $UPLOAD
		date
		PREV="JoinNaive"
	fi

	#JoinNaiveSP
	if [[ $RUN_NaiveSP == "True" ]];
	then
		date
		./joinNaive.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $project $oneSide true $UPLOAD
		date

		./compare.sh $PREV JoinNaiveSP
		PREV="JoinNaiveSP"
	fi

	#SIJoin
	if [[ $RUN_JoinMHSP == "True" ]];
	then
		for ((k=MH_K_START;k<=MH_K_END;k++)); do
			for ((q=MH_Q_START;q<=MH_Q_END;q++));do

				date
				./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide true $UPLOAD
				date

				./compare.sh $PREV JoinMHSP
			done
		done
		PREV="JoinMHSP"
	fi

	#JoinMin
	if [[ $RUN_JoinMin == "True" ]];
	then
		for ((k=MIN_K_START;k<=MIN_K_END;k++)); do
			for ((q=MIN_Q_START;q<=MIN_Q_END;q++)); do
				date
				./joinMin.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide $UPLOAD
				date
				./compare.sh $PREV JoinMin
			done
		done
		PREV="JoinMin"
	fi

	#JoinMH
	if [[ $RUN_JoinMH == "True" ]];
	then
		for ((k=MH_K_START;k<=MH_K_END;k++)); do
			for ((q=MH_Q_START;q<=MH_Q_END;q++)); do
				date
				./joinMH.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide false $UPLOAD
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
				./joinHybridOpt.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $sampling $q $project $oneSide $UPLOAD
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
				./joinHybridThres.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $threshold $q $project $oneSide $UPLOAD

				date

				./compare.sh $PREV JoinHybridThres_Q
			done
		done
		PREV="JoinHybridThres_Q"
	fi

	#JoinBK
	if [[ $RUN_JoinBK == "True" ]];
	then
		for ((k=BK_K_START;k<=BK_K_END;k++)); do
			for ((q=BK_Q_START;q<=BK_Q_END;q++)); do
				date
				./joinBK.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide false $UPLOAD
				./compare.sh $PREV JoinBK
			done
		done
		PREV="JoinBK"
	fi

	#JoinBKSP
	if [[ $RUN_JoinBKSP == "True" ]];
	then
		for ((k=BKSP_K_START;k<=BKSP_K_END;k++)); do
			for ((q=BKSP_Q_START;q<=BKSP_Q_END;q++)); do
				date
				./joinBK.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide true $UPLOAD
				./compare.sh $PREV JoinBKSP
			done
		done
		PREV="JoinBKSP"
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
				./joinDebug.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide $UPLOAD
				./compare.sh $PREV JoinMin_QL
			done
		done
	fi
fi
