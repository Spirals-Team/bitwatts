# Plot the formulae written inside the formula.inc file.
# Be careful of the curve names.
# @author: Maxime Colmant
set term postscript dashed color eps enhanced 22
set termoption dash
set output "figure6.eps"

load 'formulae.inc'

mred = '#CC0000'
mgreen = '#009900'
mlblue = '#6699FF'
mblack = '#000000'

hp1 = '#d7301f'
hp2 = '#fc8d59'
hpapi = '#fdcc8a'
hidle = '#fef0d9'

set style line 1 lc rgb mred lw 4
set style line 2 dt 3 lc rgb mgreen lw 6
set style line 3 lc rgb "orange" lw 8
set style line 4 dt 4 lc rgb mblack lw 6
set style line 5 dt 3 lc rgb mlblue lw 8

set xlabel "# Unhalted cycles"
set ylabel "Estimated Power (W)"

set key bottom right spacing 1.2
set xrange [0:10e8]
set xtics 1e8
set format x "%.0te^%01T"

plot e(x) title '2.660 GHz' ls 1 with lines, \
     d(x) title '2.527 GHz' ls 2 with lines, \
     c(x) title '2.394 GHz' ls 3 with lines, \
     b(x) title '2.261 GHz' ls 4 with lines, \
     a(x) title '2.128 GHz' ls 5 with lines