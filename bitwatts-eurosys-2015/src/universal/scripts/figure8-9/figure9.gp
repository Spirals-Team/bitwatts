# Plot the data processed with the process.py script.
# @author: Maxime Colmant

set term postscript color eps enhanced 22
set output "figure9.eps"
set termoption dashed
set style boxplot nooutliers
set style data boxplot
set style fill solid 0.8 border -1 
set boxwidth 0.5 absolute
set border 2 lt 1 lc rgb "black"
set ylabel "Relative error"

set xtics ("blackscholes" 1,"bodytrack" 2,"facesim" 3,"fluidanimate" 4,"freqmine" 5,"swaptions" 6,"vips" 7,"x264" 8)
set xtics rotate by -45 nomirror font ",16"
set format y "%.00f%%"

unset key

mred = '#CC0000'

plot 0 lt 3 linecolor rgb "black",\
  'errors-data/blackscholes/errors.csv' u (1):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/bodytrack/errors.csv' u (2):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/facesim/errors.csv' u (3):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/fluidanimate/errors.csv' u (4):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/freqmine/errors.csv' u (5):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/swaptions/errors.csv' u (6):($2*100) lt 1 linecolor rgb mred,\
  'errors-data/vips/errors.csv' u (7):($2*100) lt 1 linecolor rgb mred
