#!/bin/bash

ONE=$1
TWO=$2

if [ $ONE != "None" ]
then
	python compare.py $1 $2

	OUT=$?                                                                                                                                                                                                                                                               
	if [ $OUT -eq "1" ]                                                                                                                                                                                                                                                  
	then                                                                                                                                                                                                                                                                 
    	echo "Error"                                                                                                                                                                                                                                                     
	    read -p "Press any key to continue..."                                                                                                                                                                                                                           
	fi  
fi
