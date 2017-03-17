#!/bin/bash
LIBS="commons-cli-1.3.1.jar:SynonymOpt.jar"

#inputfile="aol/1MT100Filtered.txt"
#inputfile="aol/1M.txt"
inputfile="aol/strings.txt"
#inputfile="aol/stemmed1MT100Filtered.txt"
#inputfile="aol/stemmed1M.txt"
#inputfile="JiahengLu/data.txt"
#inputfile="samples/validateModel3/1M_T"

dir="AOL""_Sample2"
lines=`wc -l $inputfile | sed 's/ .*//'`
j=1
echo "Total" $lines "lines"
mkdir $dir
for i in 1000000 1200000 1400000 1600000 1800000 2000000 2200000 2400000 2600000 2800000 3000000; do
  # 0.01 0.01584893 0.025118864 0.039810717 0.063095734 0.1 0.158489319 0.251188643 0.398107171 0.630957344; do
  samplelines=$i #`echo "$i * $lines" | bc | sed 's/\.[0-9]*//'`
  echo "Sample" $samplelines "lines"
  shuf $inputfile | head -n $samplelines > $dir"/sample"$j
  j=$(( $j + 1 ))
done
