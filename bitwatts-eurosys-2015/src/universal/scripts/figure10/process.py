# Script to process the data used for plotting the Figure 10.
# The data come from the corresponding experiment in bitwatts-eurosys-2015.
# @author: Maxime Colmant
import os
import glob
import re
import numpy as np
import csv
import itertools as it
import shutil
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("path", help="experiment data path.")
parser.add_argument('idle', help = 'idle power of the machine.')
args = parser.parse_args()

separator = '=\n'
powers_filename = 'output-external.dat'
powerapi_filename = 'output-powerapi.dat'
powerapi_p1_filename = 'output-powerapi-p1.dat'
powerapi_p2_filename = 'output-powerapi-p2.dat'

output_path = os.path.join('results')

# Creates the output data directory
if os.path.exists(output_path):
    shutil.rmtree(output_path)

os.mkdir(output_path)

# Cleans the result file
if os.path.isfile('data.csv'):
    os.remove('data.csv')

data = []
relative_errors = []

real_powers_f = open(os.path.join(args.path, powers_filename))
real_powers_l = real_powers_f.readlines()
real_powers_f.close()

papi_powers_f = open(os.path.join(args.path, powerapi_filename))
papi_powers_l = papi_powers_f.readlines()
papi_powers_f.close()

papi_p1_powers_f = open(os.path.join(args.path, powerapi_p1_filename))
papi_p1_powers_l = papi_p1_powers_f.readlines()
papi_p1_powers_f.close()

papi_p2_powers_f = open(os.path.join(args.path, powerapi_p2_filename))
papi_p2_powers_l = papi_p2_powers_f.readlines()
papi_p2_powers_f.close()

# First part
sub1_real_powers_l = list(it.takewhile(lambda line: line != separator, real_powers_l))
sub1_real_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in sub1_real_powers_l if line != '' and line != '\n' ])
real_powers_l = list(it.dropwhile(lambda line: line != separator, real_powers_l))[1:]

sub1_papi_powers_l = list(it.takewhile(lambda line: line != separator, papi_powers_l))
sub1_papi_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in sub1_papi_powers_l if line != '' and line != '\n' ])
papi_powers_l = list(it.dropwhile(lambda line: line != separator, papi_powers_l))[1:]

sub1_papi_p1_powers_l = list(it.takewhile(lambda line: line != separator, papi_p1_powers_l))
sub1_papi_p1_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in sub1_papi_p1_powers_l if line != '' and line != '\n' ])
papi_p1_powers_l = list(it.dropwhile(lambda line: line != separator, papi_p1_powers_l))[1:]

## Sync. the data size
min_l = min(len(sub1_real_powers_d), len(sub1_papi_powers_d), len(sub1_papi_p1_powers_d))
sub1_real_powers_d = sub1_real_powers_d[0:min_l]
sub1_papi_powers_d = sub1_papi_powers_d[0:min_l]
sub1_papi_p1_powers_d = sub1_papi_p1_powers_d[0:min_l]

relative_errors.extend([ (real - (float(args.idle) + papi + p1)) / real for p1, papi, real in zip(sub1_papi_p1_powers_d, sub1_papi_powers_d, sub1_real_powers_d) if p1 > 0 ])
data.extend([ [0, p1, papi, float(args.idle), real] for p1, papi, real in zip(sub1_papi_p1_powers_d, sub1_papi_powers_d, sub1_real_powers_d) if p1 > 0 ])

# Second part
sub2_real_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in real_powers_l if line != '' and line != '\n' ])
sub2_papi_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in papi_powers_l if line != '' and line != '\n' ])
sub2_papi_p1_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in papi_p1_powers_l if line != '' and line != '\n' ])
sub2_papi_p2_powers_d = np.array([ float(re.search('power=([0-9]+\.[0-9]+)$', line, re.IGNORECASE).group(1)) for line in papi_p2_powers_l if line != '' and line != '\n' ])

## Sync. the data size
min_l = min(len(sub2_real_powers_d), len(sub2_papi_powers_d), len(sub2_papi_p1_powers_d), len(sub2_papi_p2_powers_d))
sub2_real_powers_d = sub2_real_powers_d[0:min_l]
sub2_papi_powers_d = sub2_papi_powers_d[0:min_l]
sub2_papi_p1_powers_d = sub2_papi_p1_powers_d[0:min_l]
sub2_papi_p2_powers_d = sub2_papi_p2_powers_d[0:min_l]

relative_errors.extend([ (real - (float(args.idle) + papi + p1 + p2)) / real for p1, p2, papi, real in zip(sub2_papi_p1_powers_d, sub2_papi_p2_powers_d, sub2_papi_powers_d, sub2_real_powers_d) if p1 > 0 and p2 > 0 ])
data.extend([ [p2, p1, papi, float(args.idle), real] for p1, p2, papi, real in zip(sub2_papi_p1_powers_d, sub2_papi_p2_powers_d, sub2_papi_powers_d, sub2_real_powers_d) if p1 > 0 and p2 > 0 ])

# Writes the results in files
with open('%s/data.csv' % (output_path), 'a') as csvfile:
  writer = csv.writer(csvfile, quotechar='', quoting=csv.QUOTE_NONE, delimiter=' ')
  writer.writerow(['#', 'x264', 'freqmine', 'idle', 'measured'])

  for line in data:
    writer.writerow(line)
    
# Writes statistical informations about errors
with open("%s/errors.txt" % output_path, 'a') as f:
    f.write("Avg. BitWatts: %fW\n" % (np.average(np.append(sub1_papi_powers_d, sub2_papi_powers_d))))
    f.write("median relative error: %f%%, max relative error: %f%%\n" % ((np.median(relative_errors) * 100), (max(relative_errors) * 100)))

