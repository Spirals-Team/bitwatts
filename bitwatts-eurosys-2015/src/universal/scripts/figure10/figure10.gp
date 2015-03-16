# Plot the data processed with the process.py script.
# @author: Maxime Colmant

set term postscript color eps enhanced 22
set output "figure10.eps"

set ylabel "Power (W)"
set xlabel "Time (sec)"

set format x "%.0s"
set format y "%.0s"

set pointsize 0.75
set key below horizontal center

mdblue = '#000066'
hp1 = '#d7301f'
hp2 = '#fc8d59'
hpapi = '#fdcc8a'
hidle = '#fef0d9'

plot "results/data.csv" using 0:($4+$3+$2+$1) title 'x264' lc rgb hp1 lt 1 lw 4 with filledcurves x1, \
  "results/data.csv" using 0:($4+$3+$2) title 'freqmine' lc rgb hp2 lt 3 lw 4 with filledcurves x1, \
  "results/data.csv" using 0:($4+$3) title 'BitWatts' lc rgb hpapi lt 2 lw 4 with filledcurves x1, \
  "results/data.csv" using 0:($4) title 'Idle power' lc rgb hidle lt 1 lw 4 with filledcurves x1, \
  "results/data.csv" using 0:($5) title 'PowerSpy' lc rgb mdblue lt -1 lw 2 with lines