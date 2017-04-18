set term png
set output "scatter.png"
plot "expandTimesToken.txt" using 1:2 with points pt 1
