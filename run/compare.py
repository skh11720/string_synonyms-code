import sys

if len( sys.argv ) != 3:
    print( 'Check number of parameters' )
    sys.exit( 1 )

file_one = sys.argv[ 1 ]
file_two = sys.argv[ 2 ]
different = False

print( 'Comparing ' + file_one + ' with ' + file_two )

result_dic = set()

one_count = 0
with open( 'output/' + file_one ) as f_one:
    for line in f_one:
        line = line.strip()
        result_dic.add( line )
        one_count += 1

two_count = 0
with open( 'err.log', 'w' ) as err:
    with open( 'output/' + file_two ) as f_two:
        for line in f_two:
            line = line.strip()
            if line in result_dic:
                result_dic.remove( line )
                two_count += 1
            else:
                err.write( file_one + ' does not contains ' + line + '\n' )
                different = True

    for line in result_dic:
        err.write( file_two + ' does not contains ' + line + '\n' )
        different = True

    err.write( 'Comparing finished ' + file_one + ' ' + file_two + '\n' )
    print( 'Comparing finished ' + file_one + ' ' + file_two )

    if one_count != two_count:
        err.write( 'one count ' + str( one_count ) + '\n' )
        err.write( 'two count ' + str( two_count ) + '\n' )
        print( 'Count mismatch some results are duplicated' )

if different or ( one_count != two_count ):
    sys.exit( 1 )
