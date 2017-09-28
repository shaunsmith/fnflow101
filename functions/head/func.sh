#!/bin/bash 
set -x 

lines=${FN_HEADER_Lines:-10}
(>&2 echo Taking first $lines from input )

head -n $lines
