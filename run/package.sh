#!/bin/bash

git pull

cd ..
mvn package

OUT=$?                                                                                                                                                                                                                                                               
if [ $OUT -eq "1" ]                                                                                                                                                                                                                                                  
then                                                                                                                                                                                                                                                                 
    echo "Error"                                                                                                                                                                                                                                                     
    read -p "Press any key to continue..."                                                                                                                                                                                                                           
fi  
