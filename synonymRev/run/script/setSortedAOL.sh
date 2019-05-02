#!/bin/bash

inputfile="data_store/aol/sorted_cleaned.txt"
#inputfile="data_store/aol/strings.txt"

Size=$1

dir="data_store/aol/splitted"

echo "Setting AOL data with size $Size"

if [ ! -f $dir/aol_sorted_$Size\_data.txt ];
then
	echo creating data in $dir with size $Size
	mkdir -p $dir
	head -n $Size $inputfile > $dir/aol_sorted_$Size\_data.txt
else
	echo data already exists in $dir
fi
