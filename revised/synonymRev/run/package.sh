#!/bin/bash
rm json/*.txt

if [ ! -d output ]
then
	mkdir output
fi

if [ ! -d logs ]
then
	mkdir logs
fi

if [ ! -d json ]
then
	mkdir json
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
