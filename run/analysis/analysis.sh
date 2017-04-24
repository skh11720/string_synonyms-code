#!/bin/bash


cp Debug_est.txt est_debug.txt
python corrcoef_joinmin.py
gnuplot scatter.plot
