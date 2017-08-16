threshold_list = []

est_param_list = []
est_time_list = []
est_stat_list = []

actual_param_list = []
actual_time_list = []
actual_stat_list = []

label = []
temp_label = []

with open('Estimation_DEBUG.txt') as r:
    started = False
    actual = False
    threshold = -1
    for line in r:
        if line.startswith('Estimation'):
            started = True
            threshold = int(line.strip().split(' ')[1])
            params = []
            times = []
            stats = []
            #print(threshold)
        elif line.startswith('Threshold'):
            actual = True
            threshold = int(line.strip().split(' ')[1])
            params = []
            times = []
            stats = []
            #print(threshold)
        elif started or actual:
            values = line.strip().split(' ')
            params.append(values[1])
            if actual and line.startswith('[Theta]'):
                stats.append(values[7])
            else:
                stats.append(values[5])
            times.append(values[3])
            
            if len(label) == 0:
                temp_label.append( values[0] )
            if line.startswith('[Beta]'):
                if started:
                    started = False
                    est_param_list.append(params)
                    est_time_list.append(times)
                    est_stat_list.append(stats)
                    threshold_list.append(threshold)

                    if len(label) == 0:
                        label = temp_label

                if actual:
                    actual = False
                    actual_param_list.append(params)
                    actual_time_list.append(times)
                    actual_stat_list.append(stats)

                    if len(label) == 0:
                        label = temp_label

#print(est_param_list)
#print(actual_param_list)            

with open( 'Debug.csv', 'w' ) as w:
    w.write('param\n')
    w.write('Threshold,')
    for l in label:
        w.write('Est' + l + ',')
        w.write('Act' + l + ',')
    w.write('\n') 

    for thres, est, actual in zip(threshold_list, est_param_list, actual_param_list):
        print(str(thres) + " " + str(est) + " " + str(actual))
        w.write( str(thres) + ',' )
        try:
            for i in range(len(label)):
                w.write(str(est[i]) + ',' + str(actual[i]) + ',')
        except:
            pass
        w.write('\n')

    w.write('\nStatistisc\n')
    w.write('Threshold,')
    for l in label:
        w.write('Est' + l + ',')
        w.write('Act' + l + ',')
    w.write('\n') 

    for thres, est, actual in zip(threshold_list, est_stat_list, actual_stat_list):
        print(str(thres) + " " + str(est) + " " + str(actual))
        w.write( str(thres) + ',' )
        try:
            for i in range(len(label)):
                w.write(str(est[i]) + ',' + str(actual[i]) + ',')
        except:
            pass
        w.write('\n')



    w.write('\ntime\n')
    w.write('Threshold,')
    for l in label:
        w.write('Est' + l + ',')
        w.write('Act' + l + ',')
    w.write('\n') 


    for thres, est, actual in zip(threshold_list, est_time_list, actual_time_list):
        print(str(thres) + " " + str(est) + " " + str(actual))
        w.write( str(thres) + ',' )
        try:
            for i in range(len(label)):
                w.write(str(est[i]) + ',' + str(actual[i]) + ',')
        except:
            pass
        w.write('\n')

