# Script to process the data used for plotting the Figure 9.
# The data come from the corresponding experiment in bitwatts-eurosys-2015.
# @author: Maxime Colmant
import os
import glob
import re
import numpy as np
import csv
import shutil
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("path", help="experiment data path.")
parser.add_argument('idle', help = 'idle power of the machine.')
args = parser.parse_args()

powers_filename = 'output-external.dat'
powerapi_filename = 'output-powerapi.dat'

output_path = os.path.join('errors-data')

# Creates the output data directory
if os.path.exists(output_path):
    shutil.rmtree(output_path)

os.mkdir(output_path)

for benchmark in filter((lambda name: os.path.isdir(os.path.join(args.path, name))), os.listdir(args.path)):
    benchmark_path = os.path.join(args.path, benchmark)
    benchmark_output_path = os.path.join(output_path, benchmark)
    
    if os.path.exists(benchmark_output_path):
        shutil.rmtree(benchmark_output_path)
    
    # Creates the output data directory for the benchmark
    os.mkdir(benchmark_output_path)
        
    # Reads files and converts data into numpy arrays
    real_powers_f = open(os.path.join(benchmark_path, powers_filename))
    real_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in real_powers_f ])
    real_powers_f.close()
    
    papi_powers_f = open(os.path.join(benchmark_path, powerapi_filename))
    papi_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in papi_powers_f ])
    papi_powers_f.close()
    
    # Computes the errors
    absolute_errors = real_powers_d - (papi_powers_d + float(args.idle))
    relative_errors = (real_powers_d - (papi_powers_d + float(args.idle))) / real_powers_d
    errors = zip(absolute_errors, relative_errors)
    
    # Writes errors for each benchmark for the gnuplot script
    with open("%s/errors.csv" % benchmark_output_path, 'a') as csvfile:
        writer = csv.writer(csvfile, quotechar='', quoting=csv.QUOTE_NONE, delimiter=' ')
        writer.writerow(['#', 'abs_error', 'rel_error'])

        for abs_error, rel_error in errors:
            writer.writerow([abs_error, rel_error])
            
    # Writes statistical informations about errors
    with open("%s/global-errors.txt" % output_path, 'a') as f:
       f.write("%s => median relative error: %f%%, max relative error: %f%%\n" % (benchmark, (np.median(relative_errors) * 100), (max(relative_errors) * 100)))
