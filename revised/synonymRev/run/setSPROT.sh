#!/bin/bash

inputfile="data_store/sprot/shuffled.txt"

Size=$1

dir="data_store/sprot/splitted/"

echo Setting SPROT data with size $Size

if [ ! -f $dir/SPROT_two_$Size.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	head -n $Size $inputfile > $dir/SPROT_two_$Size.txt
else
	echo data already exists in $dir
fi

#rm data_store/JiahengLu/current_data
#ln -s splitted/$Size data_store/JiahengLu/current_data
