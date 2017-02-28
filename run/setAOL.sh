#!/bin/bash

inputfile="data_store/aol/strings.txt"

Size=$1

dir="data_store/aol/splitted/$Size"

echo Setting AOL data with size $Size

if [ ! -d $dir ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	shuf $inputfile | head -n $Size > $dir"/"data.txt
else
	echo data already exists in $dir
fi

rm data_store/aol/current_data
ln -s splitted/$Size data_store/aol/current_data
