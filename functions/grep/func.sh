#!/bin/bash
set -e 
word=${FN_HEADER_Word:-foo}

(>&2  echo Searching for $word in input)

grep -ie  "$word"
