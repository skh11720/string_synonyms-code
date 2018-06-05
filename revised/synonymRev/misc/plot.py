#!/bin/usr/python3

from collections import defaultdict
from bidict import bidict
import json
import os
from os import mkdir
from os.path import exists
import pickle
from subprocess import call
import sqlite3



#cur.execute("select count(*) from projectManager_expitem where exp_date >= \"2018-05-04 08:00:00\"")
#rows = cur.fetchall()
#for row in rows:
#	print(row)


data_name_list = ['aol', 'sprot', 'usps', 'synthetic_10000', 'synthetic_100000', 'synthetic_1000000']
did_list = {}
did_list['aol'] = [ 422, 454, 455, 456, 457, 424, 464, 465, 466, 469, 434 ]
did_list['sprot'] = [ 573, 574, 575, 576, 579, 584, 585, 586, 587 ]
did_list['usps'] = [ 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677 ]
did_list['synthetic_10000'] = [ 470, 471, 472, 473, 474, 458, 475, 476, 477, 478, 479 ]
did_list['synthetic_100000'] = [ 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 453 ]
did_list['synthetic_1000000'] = [ 495, 497, 498, 500, 501, 468, 506, 512, 517, 521, 524 ]

size_list = {}
size_list['aol'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000]
size_list['sprot'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 466158]
size_list['usps'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000]
size_list['synthetic_10000'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000]
size_list['synthetic_100000'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000]
size_list['synthetic_1000000'] = [10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000]

alg_list = {'seq':[], 'set':[]}
alg_list['seq'] = [ 'JoinNaive', 'JoinMH', 'JoinMHNaive', 'JoinMHNaiveThres', 'JoinMin', 'JoinMinNaive', 'JoinMinNaiveThres', 'SIJoin', 'JoinPkduck', 'JoinPQFilterDP' ]
alg_list['set'] = [ 'JoinPQFilterDPSet', 'JoinPkduckSet' ]

dict_param = {}
dict_param['JoinPkduck'] = {}
dict_param['JoinPkduck']['ord'] = ['FF']
dict_param['JoinPkduck']['verify'] = ['TD', 'greedy', 'naive']
dict_param['JoinPkduck']['rc'] = ['false', 'true']

dict_param['JoinPkduckSet'] = {}
dict_param['JoinPkduckSet']['ord'] = ['FF']
dict_param['JoinPkduckSet']['verify'] = ['TD', 'greedy', 'naive']
dict_param['JoinPkduckSet']['rc'] = ['false', 'true']

dict_param['JoinPQFilterDP'] = {}
dict_param['JoinPQFilterDP']['K'] = ['1', '2', '3']
dict_param['JoinPQFilterDP']['qSize'] = ['1', '2', '3']
dict_param['JoinPQFilterDP']['mode'] = ['dp1', 'dp3']
dict_param['JoinPQFilterDP']['index'] = ['FTK', 'FF']

dict_param['JoinPQFilterDPSet'] = {}
dict_param['JoinPQFilterDPSet']['K'] = ['5', '4', '3', '2', '1'];
dict_param['JoinPQFilterDPSet']['verify'] = ['TD', 'GR1', 'GR3', 'MIT_GR']





'''
CREATE TABLE "projectManager_expitem" (
	"id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
	 "exp_date" datetime NOT NULL,
	 "parameter" text NOT NULL,
	 "result" text NOT NULL,
	 "algorithm_id" integer NOT NULL REFERENCES "projectManager_algorithm" ("id"),
	 "project_id" integer NOT NULL REFERENCES "projectManager_project" ("id"),
	 "dataset_id" integer NULL REFERENCES "projectManager_dataset" ("id"),
	 "failed" bool NOT NULL,
	 "server_id" integer NULL REFERENCES "projectManager_server" ("id"),
	 "invalid" bool NOT NULL,
	 "upload_date" datetime NOT NULL);


'Result_3_7_checkTPQ', 'Result_0_Total_Time', 'Val_Comparisons', 'Mem_4_Joined', 'Final Result Size', 'Val_Length_filtered'
'''





def load_metadata(cur):
	'''
	Load info of algorithms.
	Specifically, build a bidirectional map between algorithm_id and algorithm_name.
	Each algorithm_name maps to the id of the latest version.
	'''
	if exists('tmp/dict_alg.pkl') and exists('tmp/dict_ver.pkl'): 
		dict_alg = pickle.load(open('tmp/dict_alg.pkl', 'rb'))
		dict_ver = pickle.load(open('tmp/dict_ver.pkl', 'rb'))
	else:
		dict_alg = bidict({})
		cur.execute("select id, name, version from projectManager_algorithm where project_id=30")
		rows = cur.fetchall()
		dict_ver = {}
		for row in rows:
			aid, name, ver = row[:3]
			try: dict_ver[name]
			except: dict_ver[name] = -1
			ver = float(ver)
			if ver > dict_ver[name]: 
				dict_ver[name] = ver
				if aid in dict_alg: del dict_alg[aid]
				if name in dict_alg.inv: del dict_alg.inv[name]
				dict_alg[aid] = name
		pickle.dump(dict_alg, open('tmp/dict_alg.pkl', 'wb'))
		pickle.dump(dict_ver, open('tmp/dict_ver.pkl', 'wb'))

	if exists('tmp/dict_data.pkl'): dict_data = pickle.load(open('tmp/dict_data.pkl', 'rb'))
	else:
		dict_data = bidict({})
		cur.execute("select id, name from projectManager_dataset")
		rows = cur.fetchall()
		for did, name in rows:
			if did in dict_data: del dict_data[did]
			if name in dict_data.inv: del dict_data.inv[name]
			dict_data[did] = name
		pickle.dump(dict_data, open('tmp/dict_data.pkl', 'wb'))
	
	print("Check algorithm versions:")
	for name in alg_list['seq']+alg_list['set']: 
		print(name+" "+str(dict_ver[name]))
	return dict_alg, dict_data



def getDict(dict_str):
	dic = json.loads(dict_str)
	dic2 = {}
	for k,v in dic.items():
		try: int(v)
		except: pass
		else:
			dic2[k] = int(v)
			continue
		try: float(v)
		except: pass
		else:
			dic2[k] = float(v)
			continue
		dic2[k] = v
	return dic2



def sumDict(d1, d2):
	'''
	add d2 into d1.
	'''
	d = {}
	for k,v in d1.items(): 
		if k not in d2.keys(): continue
		if type(v) == int or type(v) == float:
			d[k] = d1[k] + d2[k]
	return d



def load_exp_result(data_name, setting, repeat=3):
	'''
	Return a dict of entries whose key is the name of an algorithm and
	value is a list of tuples of (data size, result dictionaries for datasets with data_name).
	'''
	dict_result = defaultdict(list)
	alg_param_list = []
	with open(setting+'.param') as f:
		for line in f: 
			alg_name, param_str = line.split(' ', 1)
			param_str = param_str.strip()
			alg_param_list.append((alg_name, param_str))
	for alg_name, param_str in alg_param_list:
		aid = dict_alg.inv[alg_name]
		print(alg_name, aid, param_str)
		for did in did_list[data_name]:
			query = "select parameter, result from projectManager_expitem where algorithm_id=%s and dataset_id=%s and parameter like \'%%%s%%\' and parameter like \'%%oneSideJoin\": \"True\"%%\' and invalid=0 order by exp_date desc limit %d"
			cur.execute(query%(str(aid), str(did), param_str, repeat))
			rows = cur.fetchall()
			result_avg = None
			for row in rows:
				param, result = map(getDict, row)
				#print(alg_name, result['Result_0_Total_Time'])
				if result_avg is None: result_avg = result
				else: result_avg = sumDict(result_avg, result)
			if result_avg is not None:
				for k,v in result_avg.items(): 
					if type(v) == int or type(v) == float: result_avg[k] /= len(rows)
				dict_result[(alg_name, param_str)].append(result_avg)
	return dict_result



def plot(data_name, dict_result, setting, logscale=True):
	print(data_name, setting)
	title_list = []
	with open(setting+'.param') as f:
		for line in f: 
			alg_name, param_str = line.split(' ', 1)
			param_str = param_str.strip()
			title_list.append(' '.join([alg_name, param_str]))
	with open(setting+'.result', 'w') as f:
		for title in title_list:
			alg_name, param_str = title.split(' ', 1)
			res_list = dict_result[(alg_name, param_str)]
			f.write('#'+alg_name+'\n')
			f.write('#'+param_str+'\n')
			for size, res in zip(size_list[data_name], res_list):
				try: f.write('%d\t%.3f\n'%(size, res[u'Result_0_Total_Time']/1000.0))
				except: break
			f.write('\n\n')
	with open(setting+'.plot', 'w') as f:
		# default options
		f.write("set xlabel \"Number of strings\""+'\n')
		f.write("set key top left width -12"+'\n')
		f.write("set ylabel \"Execution time \(sec\)\""+'\n')
		f.write("set term png size 800,600"+'\n')
		if logscale:
			f.write("set logscale x"+'\n')
			f.write("set logscale y"+'\n')
		f.write("set output \"%s_%s.png\""%(setting, data_name)+'\n')
		
		cmd_list = ["\"%s.result\" index %d with linespoints title \"[\'%s\']\""%(setting, idx, title) for idx, title in enumerate(title_list)]
		f.write("plot "+',\\\n'.join(cmd_list))

	call(['gnuplot', setting+'.plot'])



def plot_one(setting, data_name=None):
	if data_name is None: data_names = data_name_list
	else: data_names = [data_name]
	for data_name in data_names:
		dict_result = load_exp_result(data_name, setting)
		plot(data_name, dict_result, setting, logscale=True)
			


def plot_all():
	for setting, logscale in zip(['compare_seq', 'compare_set'], [True, False]):
	 for data_name in ['aol', 'sprot', 'usps', 'synthetic_10000', 'synthetic_100000', 'synthetic_1000000']:
		dict_result = load_exp_result(data_name, setting)
		plot(data_name, dict_result, setting, logscale=logscale)


if __name__ == '__main__':
	# connect to DB
	conn = sqlite3.connect('/home/yjpark/django/django-project/mysite/db.sqlite3')
	cur = conn.cursor()

	# load info of algorithms and datasets
	dict_alg, dict_data = load_metadata(cur)
	print("number of algorithms:",len(dict_alg))
	print("number of datasets:",len(dict_data))

	# draw plots
	plot_one('compare_pqfdp')

	conn.close()




