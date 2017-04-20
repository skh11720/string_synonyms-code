set term png
set output "scatter.png"
#set logscale y
plot "expandTimesToken.txt" using 1:2 with points pt 1
