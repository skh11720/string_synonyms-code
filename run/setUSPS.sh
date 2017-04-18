#!/bin/bash

inputfile="data_store/JiahengLu/data.txt"

Size=$1

dir="data_store/JiahengLu/splitted/"

echo Setting USPS data with size $Size

if [ ! -f $dir/$Size.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	shuf $inputfile | head -n $Size > $dir/USPS_$Size.txt
else
	echo data already exists in $dir
fi

#rm data_store/JiahengLu/current_data
#ln -s splitted/$Size data_store/JiahengLu/current_data
