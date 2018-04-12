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
if [ $? -ne "0" ]
then
	echo "Error"
	read -p "Press any key to continue..."
fi

cd ..
mvn package

OUT=$?                                                                                                                                                                                                                                                               
if [ $OUT -ne "0" ]                                                                                                                                                                                                                                                  
then                                                                                                                                                                                                                                                                 
    echo "Error"                                                                                                                                                                                                                                                     
    read -p "Press any key to continue..."                                                                                                                                                                                                                           
fi  
cd -

if [ ! -d json/uploaded ];
then
    mkdir -p json/uploaded
fi

if [ ! -e uploader/ExperimentUploader/uploadExperiment.py ]
then
    echo "Downloadling uploadExperiment.py"
    mkdir uploader
    cd uploader
    git clone ssh://yjpark@147.46.143.74/home/yjpark/repository/ExperimentUploader/
	cd -
fi

if [ ! -e upload.sh ];
then
	echo "./uploader/ExperimentUploader/uploadExperiment.py exp" > upload.sh
	chmod u+x upload.sh
fi

