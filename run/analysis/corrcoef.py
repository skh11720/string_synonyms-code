import numpy

timeArray = []
expandedSize = []
tokenLength = []
functionCall = []

expandedSizeRatio = []
tokenLengthRatio = []
functionCallRatio = []

expandPerToken = []

appRule = []
appRuleTimesToken = []


max_time = 0;
max_line = '';

with open('est_debug.txt') as r:
    for line in r:
        temp = line.strip().split( ' ' )
        expandedSize.append( int( temp[ 0 ] ) )
        tokenLength.append( int( temp[ 1 ] ) )
        functionCall.append( int( temp[ 2 ] ) )

        expandedSizeRatio.append( float( temp[ 3 ] ) )
        tokenLengthRatio.append( float( temp[ 4 ] ) )
        functionCallRatio.append( float( temp[ 5 ] ) )

        time = float( temp[ 6 ] )
        timeArray.append( time )

        if max_time < time:
            max_time = time
            max_line = line

        expandPerToken.append( float( temp[ 0 ] ) * float( temp[ 1 ] ) )
        appRule.append( float( temp[ 7 ] ) )
        appRuleTimesToken.append( float( temp[ 7 ] ) * float( temp[ 1 ] ) )


print( numpy.corrcoef( expandedSize, timeArray ) )
print( numpy.corrcoef( tokenLength, timeArray ) )
print( numpy.corrcoef( functionCall, timeArray ) )
print( numpy.corrcoef( expandPerToken, timeArray ) )
print( numpy.corrcoef( appRule, timeArray ) )
print( numpy.corrcoef( appRuleTimesToken, timeArray ) )


print( numpy.var( expandedSizeRatio ) )
print( numpy.var( tokenLengthRatio ) )
print( numpy.var( functionCallRatio ) )


print( max_time )
print( max_line )


with open( 'expandTimesToken.txt' , 'w' ) as w:
    for ( value, time ) in zip( expandPerToken, timeArray ):
        w.write( str(value) + " " + str(time) + "\n" )
