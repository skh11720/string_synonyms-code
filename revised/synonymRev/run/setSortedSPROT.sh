#!/bin/bash

inputfile="data_store/sprot/cc.txt"

Size=$1

dir="data_store/sprot/splitted/"

echo Setting SPROT data with size $Size

if [ ! -f $dir/SPROT_$Size.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	shuf $inputfile | head -n $Size > $dir/SPROT_$Size.txt
else
	echo data already exists in $dir
fi

#rm data_store/JiahengLu/current_data
#ln -s splitted/$Size data_store/JiahengLu/current_data
