#!/bin/bash

if [ ! -d json/uploaded ];
then
    mkdir -p json/uploaded
fi

if [ ! -e uploader/uploadExperiment.py ]
then
        echo "Downloadling uploadExperiment.py"
        mkdir uploader
        cd uploader
#        git clone ssh://yjpark@147.46.143.74/home/yjpark/repository/ExperimentUploader/
        wget -O uploadExperiment.py http://147.46.143.74:18000/projects/expUploader/
        chmod u+x uploadExperiment.py
else
        cd uploader
        #git pull
fi

cd -
./uploader/uploadExperiment.py exp
