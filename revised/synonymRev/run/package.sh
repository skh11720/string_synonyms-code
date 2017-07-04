#!/bin/bash

if [ ! -d output ]
then
	mkdir output
fi

if [ ! -d logs ]
then
	mkdir logs
fi

git pull

cd ..
mvn package

OUT=$?                                                                                                                                                                                                                                                               
if [ $OUT -eq "1" ]                                                                                                                                                                                                                                                  
then                                                                                                                                                                                                                                                                 
    echo "Error"                                                                                                                                                                                                                                                     
    read -p "Press any key to continue..."                                                                                                                                                                                                                           
fi  
