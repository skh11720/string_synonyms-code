import numpy

ignoreError = True
threshold=2e4
#threshold=1.5e7
#threshold=3.5e7

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

expandIter = []

max_time = 0
max_line = ''

err_count = 0
resize_count = 0
gc_count = 0

with open('est_debug.txt') as r:
    for line in r:
        temp = line.strip().split( ' ' )

        time = float( temp[ 6 ] )
        resize = int( temp[ 9 ] )
        gc = int( temp[ 14 ] )
        if time >= threshold:
            print line.strip()
            if resize >= 1:
                resize_count += 1
                #continue
            elif gc >= 1:
                gc_count += 1
            else:
                err_count += 1

            if ignoreError:
                continue

        expandedSize.append( int( temp[ 0 ] ) )
        tokenLength.append( int( temp[ 1 ] ) )
        functionCall.append( int( temp[ 2 ] ) )

        expandedSizeRatio.append( float( temp[ 3 ] ) )
        tokenLengthRatio.append( float( temp[ 4 ] ) )
        functionCallRatio.append( float( temp[ 5 ] ) )
        expandIter.append( float( temp[ 15 ] ) )

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


print( 'error count ' + str(err_count) )
print( 'resize count ' + str(resize_count) )
print( 'gc count ' + str(gc_count) )
with open( 'expandTimesToken.txt' , 'w' ) as w:
    #print( 'plot with expandIter vs timeArray' )
    #for ( value, time ) in zip( expandIter, timeArray ):
       #w.write( str(value) + " " + str(time) + "\n" )

    #print( 'plot with functionCall vs timeArray' )
    #for ( value, time ) in zip( functionCall, timeArray ):
        #w.write( str(value) + " " + str(time) + "\n" )

    #print( 'plot with expandPerToken vs timeArray' )
    #for ( value, time ) in zip( expandPerToken, timeArray ):
        #w.write( str(value) + " " + str(time) + "\n" )

    print( 'plot with expandedSize vs timeArray' )
    for ( value, time ) in zip( expandedSize, timeArray ):
        w.write( str(value) + " " + str(time) + "\n" )
