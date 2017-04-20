#!/bin/bash


cp est_debug_orig.txt est_debug.txt
python corrcoef.py
gnuplot scatter.plot
