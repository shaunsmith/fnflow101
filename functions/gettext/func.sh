#!/bin/bash 

input=$(cat)


(>&2  echo opening file $input)

if [ -f data/$input ] ; then
    cat data/$input;
else
    (>&2  echo file $input not found, possible files: )
    (>&2  ls data/ )
    exit 1
fi
