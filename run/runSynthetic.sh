#!/bin/bash

nTokens=1000000
nRecords=1000000
nRules=200000
nTokensInRule=30000
avgRecLen=5
avgLhsLen=2
avgRhsLen=2
skewZ=1
ratio=0

./setSynthetic.sh $nTokens $nRecords $nRules $nTokensInRule $avgRecLen $avgLhsLen $avgRhsLen $skewZ $ratio

