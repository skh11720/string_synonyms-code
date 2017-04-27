#!/bin/bash


cp JoinMin_Join_Debug.txt est_debug.txt
python corrcoef_joinmin_q.py
gnuplot scatter.plot
