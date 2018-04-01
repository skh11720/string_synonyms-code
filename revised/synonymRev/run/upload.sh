#!/bin/bash

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
else
        cd uploader/ExperimentUploader
#        git pull
fi

cd -
./uploader/ExperimentUploader/uploadExperiment.py exp
