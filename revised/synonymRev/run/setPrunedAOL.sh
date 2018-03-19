#!/bin/bash

inputfile="data_store/aol/new_aol_1e6.txt"
#inputfile="data_store/aol/strings.txt"

Size=$1

dir="data_store/aol/pruned"

echo Setting AOL data with size $Size

if [ ! -f $dir"/"aol_pruned_$Size\_data.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	head -n $Size $inputfile > $dir"/"aol_pruned_$Size\_data.txt
else
	echo data already exists in $dir
fi

#rm data_store/aol/current_data
#ln -s splitted/$Size data_store/aol/current_data
