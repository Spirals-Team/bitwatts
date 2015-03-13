#!/bin/bash

cd $1
source env.sh
parsecmgmt -a run -i native -p $2 &>/dev/null &
exit 0
