import numpy

ignoreError = True
threshold=1e6
#threshold=1.5e7
#threshold=3.5e7

timeArray = []
qgramArray = []
candArray = []

max_time = 0
max_line = ''

err_count = 0
resize_count = 0
gc_count = 0

rec_id = 0

with open('est_debug.txt') as r:
    for line in r:
        temp = line.strip().split( ' ' )

        time = float( temp[ 0 ] )
        qgram = float( temp[ 1 ] )
        #gc = float( temp[ 2 ] )
        #cand = float( temp[ 3 ] )

        if time > threshold:
            #if gc >= 1:
                #gc_count += 1
            #else:
            err_count += 1
            print( str( rec_id ) + " " + str( time ) )
            rec_id += 1
            continue

        rec_id += 1
        if max_time < time:
            max_time = time
            max_line = line.strip()

        timeArray.append( time )
        qgramArray.append( qgram )
        #candArray.append( cand )
print( numpy.corrcoef( qgramArray, timeArray ) )
#print( numpy.corrcoef( candArray, timeArray ) )


print( max_time )
print( max_line )

print( 'error count ' + str(err_count) )
print( 'resize count ' + str(resize_count) )
print( 'gc count ' + str(gc_count) )

with open( 'expandTimesToken.txt' , 'w' ) as w:
    #print( 'plot with candArray vs timeArray' )
    #for ( value, time ) in zip( candArray, timeArray ):
        #w.write( str(value) + " " + str(time) + "\n" )
    print( 'plot with qgramArray vs timeArray' )
    for ( value, time ) in zip( qgramArray, timeArray ):
        w.write( str(value) + " " + str(time) + "\n" )
