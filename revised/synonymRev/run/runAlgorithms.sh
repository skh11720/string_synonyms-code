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
RUN_JoinMinNaive=${12}
RUN_JoinMinNaive_Thres=${13}
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
echo RUN_JoinMinNaive $RUN_JoinMinNaive
echo RUN_JoinMinNaive_Thres $RUN_JoinMinNaive_Thres
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

MIN_NAIVE_K_START=1
MIN_NAIVE_K_END=3
MIN_NAIVE_Q_START=1
MIN_NAIVE_Q_END=3

MIN_RANGE_K_START=1
MIN_RANGE_K_END=3
MIN_RANGE_Q_START=1
MIN_RANGE_Q_END=3

MIN_NAIVE_THRES=( 3 10 30 100 300 )
MIN_NAIVE_THRES_K_START=1
MIN_NAIVE_THRES_K_END=1
MIN_NAIVE_THRES_Q_START=2
MIN_NAIVE_THRES_Q_END=2

MH_NAIVE_THRES=( 3 10 30 100 300 )
MH_NAIVE_THRES_K_START=1
MH_NAIVE_THRES_K_END=1
MH_NAIVE_THRES_Q_START=2
MH_NAIVE_THRES_Q_END=2



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
		for ((k=MHSP_K_START;k<=MHSP_K_END;k++)); do
			for ((q=MHSP_Q_START;q<=MHSP_Q_END;q++));do

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
	if [[ $RUN_JoinMinNaive == "True" ]];
	then
		samplings=( 0.01 )
		for sampling in "${samplings[@]}"; do
			for ((k=MIN_NAIVE_K_START;k<=MIN_NAIVE_K_END;k++)); do
				for ((q=MIN_NAIVE_Q_START;q<=MIN_NAIVE_Q_END;q++)); do
					date
					./joinMinNaive.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $sampling $k $q $project $oneSide $UPLOAD
					date

					./compare.sh $PREV JoinMinNaive
				done
			done
		done
		PREV="JoinMinNaive"
	fi

	#JoinHybridThres
	if [[ $RUN_JoinMinNaive_Thres == "True" ]];
	then
		for ((k=MIN_NAIVE_THRES_K_START;k<=MIN_NAIVE_THRES_K_END;k++)); do
			for ((q=MIN_NAIVE_THRES_Q_START;q<=MIN_NAIVE_THRES_Q_END;q++)); do
				for threshold in "${MIN_NAIVE_THRES[@]}"; do
					date
					./joinMinNaiveThres.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $threshold $k $q $project $oneSide $UPLOAD

					date

					./compare.sh $PREV JoinMinNaiveThres
				done
			done
		done
		PREV="JoinMinNaiveThres"
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
		for ((k=MH_NAIVE_THRES_K_START;k<=MH_NAIVE_THRES_K_END;k++)); do
			for ((q=MH_NAIVE_THRES_Q_START;q<=MH_NAIVE_THRES_Q_END;q++)); do
				for threshold in "${MH_NAIVE_THRES[@]}"; do
					date
					./joinMHNaiveThres.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $threshold $k $q $project $oneSide $UPLOAD

					date

					./compare.sh $PREV JoinMHNaiveThres
				done
			done
		done
		PREV="JoinMHNaiveThres"

		#for ((k=MIN_RANGE_K_START;k<=MIN_RANGE_K_END;k++)); do
		#	for ((q=MIN_RANGE_Q_START;q<=MIN_RANGE_Q_END;q++)); do
		#		date
		#		#./joinMinRange.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide $UPLOAD
		#		./joinMHNaiveThres.sh
		#		#./joinDebug.sh $inputfile_one $inputfile_two $rulefile $outputPath $dir $LIBS $k $q $project $oneSide $UPLOAD
		#		./compare.sh $PREV JoinMinRange
		#	done
		#done
	fi
fi
