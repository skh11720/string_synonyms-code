#!/bin/bash

inputfile="data_store/aol/sort_cleaned_removed.txt"
#inputfile="data_store/aol/strings.txt"

Size=$1

dir="data_store/aol/splitted"

echo Setting AOL data with size $Size

if [ ! -f $dir"/"aol_$Size\_data.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	head -n $Size $inputfile > $dir"/"aol_$Size\_data.txt
else
	echo data already exists in $dir
fi

#rm data_store/aol/current_data
#ln -s splitted/$Size data_store/aol/current_data
