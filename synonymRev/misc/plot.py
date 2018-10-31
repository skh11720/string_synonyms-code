#!/bin/usr/python3

from collections import defaultdict, OrderedDict
from bidict import bidict
from functools import reduce
import inspect
import json
import numpy as np
import os
from os import mkdir
from os.path import exists, join
import pickle
from shutil import rmtree
from subprocess import call
import sqlite3
import sys




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
alg_list['seq'] = [ 'JoinNaive', 'JoinMH', 'JoinMHNaive', 'JoinMHNaiveThres', 'JoinMin', 'JoinMinFast', 'JoinMinNaive', 'JoinMinNaiveThres', 'SIJoin', 'JoinPkduck', 'JoinPQFilterDP', 'JoinHybridAll', 'JoinHybridAll2', 'JoinHybridAll3', 'PassJoinExact', 'JoinMHDelta', 'JoinMHStrongDelta', 'JoinMHNaiveDelta', 'JoinMHNaiveThresDelta', 'JoinMinDelta', 'JoinMinStrongDelta', 'JoinMinNaiveDelta', 'JoinMinNaiveThresDelta', 'JoinNaiveDelta', 'JoinNaiveDelta2', 'JoinHybridAllDelta' ]
alg_list['set'] = [ 'JoinSetNaive', 'JoinPQFilterDPSet', 'JoinPkduckSet' ]
alg_list['all'] = alg_list['seq'] + alg_list['set']

dict_alg_name = {
 'JoinNaive':'JoinNaive', 
 'JoinMH':'JoinFKP',
 'JoinMHDP':'JoinFKPDP',
 'JoinMin':'JoinBKP',
 'JoinMinFast':'JoinBKPFast',
 'JoinMinDP':'JoinBKPDP',
 'SIJoin':'SIJoin',
 'JoinPkduck':'JoinPkduck',
 'JoinPQFilterDP':'JoinPQDP',
 'JoinHybridAll':'JoinHybrid',
 'JoinHybridAll2':'JoinHybrid2',
 'JoinHybridAll3':'JoinHybrid3',
 'PassJoinExact':'PassJoin',

 'JoinNaiveDelta':'JoinNaive', 
 'JoinNaiveDelta2':'PassJoin', 
 'JoinMHDelta':'JoinFKP',
 'JoinMHStrongDelta':'JoinFKPs',
 'JoinMHDeltaDP':'JoinFKPDP',
 'JoinMinDelta':'JoinBKP',
 'JoinMinStrongDelta':'JoinBKPs',
 'JoinMinDeltaDP':'JoinBKPDP',
 'JoinHybridAllDelta':'JoinHybrid',

 'JoinSetNaive':'JoinSetNaive',
 'JoinPQFilterDPSet':'JoinDPSet',
 'JoinPkduckSet':'JoinPkduckSet'
}

dict_alg_ver = defaultdict(lambda: None,
{
 'JoinNaive':2.01,
 'JoinMH':2.51,
 'JoinMin':2.51,
 'JoinHybridAll':2.63,
})

dict_param = OrderedDict()
dict_param['JoinPkduck'] = OrderedDict()
dict_param['JoinPkduck']['ord'] = ['FF']
dict_param['JoinPkduck']['verify'] = ['TD', 'greedy', 'naive']
dict_param['JoinPkduck']['rc'] = ['false', 'true']

dict_param['JoinPkduckSet'] = OrderedDict()
dict_param['JoinPkduckSet']['ord'] = ['FF']
dict_param['JoinPkduckSet']['verify'] = ['TD', 'greedy', 'naive']
dict_param['JoinPkduckSet']['rc'] = ['false', 'true']

dict_param['JoinPQFilterDP'] = OrderedDict()
dict_param['JoinPQFilterDP']['K'] = ['1', '2', '3']
dict_param['JoinPQFilterDP']['qSize'] = ['1', '2', '3']
dict_param['JoinPQFilterDP']['mode'] = ['dp1', 'dp3']
dict_param['JoinPQFilterDP']['index'] = ['FTK', 'FF']

dict_param['JoinPQFilterDPSet'] = OrderedDict()
dict_param['JoinPQFilterDPSet']['K'] = ['5', '4', '3', '2', '1'];
dict_param['JoinPQFilterDPSet']['verify'] = ['TD', 'GR1', 'GR3', 'MIT_GR']

dict_param['JoinMH'] = OrderedDict()
dict_param['JoinMH']['K'] = ['1', '2']
dict_param['JoinMH']['qSize'] = ['1', '2']

dict_param['JoinMin'] = OrderedDict()
dict_param['JoinMin']['K'] = ['1', '2']
dict_param['JoinMin']['qSize'] = ['1', '2']

dict_param['JoinHybridAll'] = OrderedDict()
dict_param['JoinHybridAll']['K'] = ['1']
dict_param['JoinHybridAll']['qSize'] = ['2']
dict_param['JoinHybridAll']['sample'] = ['0.01']
dict_param['JoinHybridAll']['nEst'] = ['1']

dict_param['JoinHybridAll2'] = OrderedDict()
dict_param['JoinHybridAll2']['K'] = ['1']
dict_param['JoinHybridAll2']['qSize'] = ['2']
dict_param['JoinHybridAll2']['sampleH'] = ['0.01']
dict_param['JoinHybridAll2']['sampleB'] = ['0.01']

dict_param['JoinHybridAll'] = OrderedDict()
dict_param['JoinHybridAll']['K'] = ['1']
dict_param['JoinHybridAll']['qSize'] = ['2']
dict_param['JoinHybridAll']['sample'] = ['0.01']
dict_param['JoinHybridAll']['nEst'] = ['1']
dict_param['JoinMHDelta'] = OrderedDict()
dict_param['JoinMHDelta']['K'] = ['1', '2']
dict_param['JoinMHDelta']['qSize'] = ['1', '2']
dict_param['JoinMHDelta']['delta'] = ['0', '1', '2']
dict_param['JoinMinDelta'] = dict_param['JoinMHDelta']

dict_param['JoinHybridAllDelta'] = OrderedDict()
dict_param['JoinHybridAllDelta']['K'] = ['2']
dict_param['JoinHybridAllDelta']['qSize'] = ['2']
dict_param['JoinHybridAllDelta']['sample'] = ['0.01', '0.05', '0.1']
dict_param['JoinHybridAllDelta']['nEst'] = ['1', '5']
dict_param['JoinHybridAllDelta']['delta'] = ['0', '1', '2']



attr_time_list = ['Result_0_Total_Time', 'Result_1_Initialize_Time', 'Result_2_Preprocess_Total_Time', 'Result_2_1_Estimation_Time', 'Result_3_1_Index_Building_Time', 'Result_3_2_Join_Time', 'Stat_CandQGramCountTime', 'Stat_FilterTime', 'Stat_EquivTime']
attr_count_list = ['Final Result Size', 'Stat_CandQGram_Avg', 'Stat_Equiv_Comparison', 'Val_Length_filtered', 'Val_PQGram_filtered',]




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
#			if name == 'JoinHybridAllDelta' and ver > 1.01: continue
			if dict_alg_ver[name] == ver or (dict_alg_ver[name] is None and ver > dict_ver[name]):
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
		try: print(name+" "+str(dict_ver[name]))
		except: pass
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
		elif k == 'Estimate_JoinMinSelected':
			d[k] = 0
			if type(d1[k]) == float: d[k] += d1[k]
			elif type(d1[k]) == unicode:
				if d1[k] == 'true': d[k] += 1.0
				elif d1[k] == 'false': pass
				else: raise Exception()
			else: raise Exception()
			if type(d2[k]) == float: d[k] += d2[k]
			elif type(d2[k]) == unicode:
				if d2[k] == 'true': d[k] += 1.0
				elif d2[k] == 'false': pass
				else: raise Exception()
			else: raise Exception()
		elif type(v) == int or type(v) == float:
			d[k] = d1[k] + d2[k]
	return d



def averageDict(d_list):
	d_sum = reduce(sumDict, d_list)
	print(d_sum['Estimate_JoinMinSelected'])
	d_avg = {k:v/len(d_list) for k,v in d_sum.items() if type(v) == int or type(v) == float}
	print(d_avg['Estimate_JoinMinSelected'])

	""" Auto_JoinMin_Selected or Estimate_JoinMinSelected """
	if 'Auto_JoinMin_Selected' in d_list[0]:
		n_true = len([d for d in d_list if d['Auto_JoinMin_Selected'] == 'true'])
		d_avg['Auto_JoinMin_Selected'] = '%.2f'%(n_true/len(d_list))
#	if 'Estimate_JoinMinSelected' in d_list[0]:
#		n_true = len([d for d in d_list if d['Estimate_JoinMinSelected'] == 'true'])
#		d_avg['Estimate_JoinMinSelected'] = '%.2f'%(n_true/len(d_list))
	return d_avg



def parse_settings(setting):
	alg_param_list = []
	with open(setting+'.param') as f:
		for line in f: 
			if line.startswith('#'): continue
			alg_name, param_str = line.split(' ', 1)
			param_str = param_str.strip()
			alg_param_list.append((alg_name, param_str))
	return alg_param_list


def parse_multiple_settings(setting):
	dict_alg_param_list = OrderedDict()
	title = None;
	alg_param_list = None
	with open(setting+'.param') as f:
		for line in f: 
			if line.strip() == '': continue
			if line.startswith('#'):
				if title is not None:
					dict_alg_param_list[title] = alg_param_list
				title = line.strip()[1:]
				alg_param_list = []
			else:
				alg_name, param_str = line.split(' ', 1)
				param_str = param_str.strip()
				alg_param_list.append((alg_name, param_str))
	if title is not None:
		dict_alg_param_list[title] = alg_param_list
	return dict_alg_param_list


def load_exp_result_aux(cur, aid, did, param_str, repeat=3):
	query = "select parameter, result from projectManager_expitem where algorithm_id=%d and dataset_id=%d and parameter like \'%%%s%%\' and parameter like \'%%oneSideJoin\": \"True\"%%\' and invalid=0 order by exp_date desc limit %d"
	#print(query%(aid, did, param_str, repeat))
	cur.execute(query%(aid, did, param_str, repeat))
	rows = cur.fetchall()
	result_list = []
	for row in rows:
		param, result = map(getDict, row)
		result_list.append(result)
	return result_list



def load_exp_result_from_list(cur, data_name, alg_param_list, repeat=3):
	'''
	Return a dict of entries whose key is the name of an algorithm and
	value is a list of tuples of (data size, result dictionaries for datasets with data_name).
	alg_param_list: a list of (alg_name, aid, param_str).
	'''
	dict_result = OrderedDict()
	for alg_name, aid, param_str in alg_param_list:
		dict_result[(alg_name, param_str)] = []
#		aid = dict_alg.inv[alg_name]
#		print(alg_name, aid, param_str)
		for did in did_list[data_name]:
			result_list = load_exp_result_aux(cur, aid, did, param_str, repeat)
			if len(result_list) > 0:
				result_avg = averageDict(result_list)
				dict_result[(alg_name, param_str)].append(result_avg)
		# end for did
	# end for alg_name, param_str
	return dict_result


def load_exp_result(cur, data_name, setting, repeat=3):
	alg_param_list = parse_settings(setting) # list of (alg_name, param_str)
	aid_param_list = list(map(lambda x: (x[0], dict_alg.inv[x[0]], x[1]), alg_param_list))
	return load_exp_result_from_list(cur, data_name, aid_param_list, repeat=3)



list_pt = [1, 2, 8, 4, 6, 12, 3]


#def plot(data_name, dict_result, setting, attr=u'Result_0_Total_Time', logscale=True):
def plot(data_name, dict_result, setting, subsetting, alg_param_list, attr=u'Result_0_Total_Time', logscale=True, output_format='png'):
	print(data_name, setting, subsetting)

	#if output_format == 'png':
		#alg_param_list = parse_settings(setting)
	title_list = list(map(lambda x: ' '.join(x), alg_param_list))
	valid_title_list = []
	if subsetting is None: subsetting = setting

	with open(join(setting, data_name+'_'+subsetting+'.result'), 'w') as f:
		for i in range(len(title_list)):
			title = title_list[i]
			alg_name, param_str = title.split(' ', 1)
			title_list[i] = ' '.join([dict_alg_name[alg_name], param_str])
			if output_format == 'pdf':
				title_list[i] = dict_alg_name[alg_name]
			res_list = dict_result[(alg_name, param_str)]
			if len(res_list) == 0: continue
			valid_title_list.append(title_list[i])
			f.write('#'+alg_name+'\n')
			f.write('#'+param_str+'\n')
			for size, res in zip(size_list[data_name], res_list):
				try: f.write('%d\t%.3f\n'%(size, res[attr]/1000.0))
				except: break
			f.write('\n\n')
	with open(join(setting, data_name+'_'+subsetting+'.plot'), 'w') as f:
		# default options
		f.write("set format x \"10^{%L}\""+'\n')
		f.write("set format y \"10^{%L}\""+'\n')
		f.write("set xlabel \"Number of strings\""+'\n')
		f.write("set key top left"+'\n')
		f.write("set ylabel \"Execution time \(sec\)\""+'\n')
		if data_name == 'sprot': f.write("set xrange [10000:466158]\n")
		if logscale:
			f.write("set logscale y"+'\n')
			f.write("set logscale x"+'\n')
		if output_format == 'png':
			f.write("set term png font \"arial,8\"\n")
			#f.write("set term png font \"arial,24\""+'\n')
			#f.write("set size 640,480"+'\n')
			f.write("set output \"%s.png\""%(join("plot", '_'.join([data_name, subsetting])))+'\n')
		elif output_format == 'pdf':
			f.write("set size 0.6,0.6"+'\n')
			f.write("set term postscript"+'\n')
			f.write("set output \"| ps2pdf - %s.pdf\""%(join("plot", '_'.join([data_name, subsetting])))+'\n')

		cmd_list = []
		#valid_title_list = ['K=1', 'K=2', 'K=3', 'K=4', 'K=5']
		#valid_title_list = ['q=1', 'q=2', 'q=3', 'q=4', 'q=5']
		for idx, title in enumerate(valid_title_list):
			cmd = ''
			cmd += "\"%s.result\""%(join(setting, data_name+'_'+subsetting))

			if output_format == 'pdf':
				cmd += " index %d with linespoints lc \"black\" lw 3 ps 1.5 dt %d pt %d"%(idx, idx+1, list_pt[idx % len(list_pt)])
				cmd += " title \"%s\""%title.replace('_', '-')
			elif output_format == 'png':
				cmd = "\"%s.result\" index %d with linespoints title \"%s\""%(join(setting, data_name+'_'+subsetting), idx, title.replace('_', '-'))
			cmd_list.append(cmd)
		f.write("plot\\\n"+',\\\n'.join(cmd_list))

	call(['gnuplot', join(setting, data_name+'_'+subsetting+'.plot')])
	call(['sleep', '0.3'])
	if output_format == 'pdf':
		call(['pdfcrop', join("plot", data_name+'_'+subsetting+'.pdf')])
		call(['mv', join("plot", data_name+'_'+subsetting+'-crop.pdf'), join("plot", data_name+"_"+subsetting+".pdf")])



#def plot_one(setting, data_name=None, attr='Result_0_Total_Time', logscale=True):
#	if not exists(setting): mkdir(setting)
#	if data_name is None: data_names = data_name_list
#	else: data_names = [data_name]
#	for data_name in data_names:
#		dict_result = load_exp_result(data_name, setting)
#		plot(data_name, dict_result, setting, None, attr, logscale=logscale)
			


def plot_all(setting, data_name=None, attr='Result_0_Total_Time', logscale=True, output_format='png'):
	if not exists(setting): mkdir(setting)
	dict_alg_param_list = parse_multiple_settings(setting)
	if data_name is None: data_names = data_name_list
	else: data_names = [data_name]
	for title, alg_param_list in dict_alg_param_list.items():
		aid_param_list = list(map(lambda x: (x[0], dict_alg.inv[x[0]], x[1]), alg_param_list))
		for data_name in data_names:
			dict_result = load_exp_result_from_list(cur, data_name, aid_param_list, repeat=3)
			#print(data_name, setting, title, alg_param_list)
			plot(data_name, dict_result, setting, title, alg_param_list, attr=attr, logscale=logscale, output_format=output_format)



def compare_attr(setting, func_list):
	did = 424 # aol 100000
	alg_param_list = parse_settings(setting)
	for alg_name, param_str in alg_param_list:
		aid = dict_alg.inv[alg_name]
		result_list = load_exp_result_aux(cur, aid, did, param_str)
		output_list = []
		for func in func_list:
			val_list = list(map(func, result_list))
			if any(map(lambda x: x is None, val_list)): output_list.append(0)
			else: output_list.append(np.mean(val_list))
		if len(output_list) == 0: continue
		print(' '.join([alg_name, param_str])+'\t'+' '.join(map(str, output_list)))
	'''
	with open('compare_attr_'+setting+'.result', 'w') as f:
		f.write(' '.join([alg_name, param_str]) +'\t'+ '\t'.join(map(str, output_list))+'\n')

	with open(setting+'.plot', 'w') as f:
		# default options
		f.write(""+\
		"set style data histograms\n"+\
		"set style histogram rowstacked\n"+\
		"set boxwidth 0.75 relative\n"+\
		"set style fill solid 1.0 border -1\n"+\
		"set datafile separator \"\t\"\n"+\
		"set output \"%s.png\""%('compare_attr_'+setting)+'\n')

		
		cmd_list = ["\"%s.result\" using index %d with linespoints title \"[\'%s\']\""%(setting, idx, title) for idx, title in enumerate(title_list)]
		f.write("plot "+',\\\n'.join(cmd_list))
	'''
	


def compare_alg(cur, setting, attr, data_list=None):
	f = open('compare_alg.txt', 'w')
	if data_list is None: data_list = data_name_list
	for data_name in data_list:
		f.write(data_name+'\n')
		dict_result_list = load_exp_result(cur, data_name, setting)
		f.write('\t'.join([''] + list(map(str, size_list[data_name])))+'\n')
		for key, dict_result in dict_result_list.items():
			alg_name = key[0]
			val_list = map(lambda x: str(x.get(attr)), dict_result)
			f.write('\t'.join([alg_name] + list(val_list))+'\n')
		f.write(''+'\n')
	f.flush()
	f.close()



def compare_acc(data_list=None):
	setting = 'compare_acc'
	if data_list is None: data_list = data_name_list
	dict_output = defaultdict(list)
	for data_name in data_list:
		dict_result_list = load_exp_result(cur, data_name, setting)
		for key, dict_result in dict_result_list.items():
			alg_name, param = key
			title = ' '.join(key)
			val_list = map(lambda x: x.get(u'Final Result Size'), dict_result)
			dict_output[alg_name].append((param, data_name, val_list))

	with open('compare_acc.txt', 'w') as f:
		for alg_name in dict_output.keys():
			f.write(alg_name+'\n')
			f.write('\t'.join(['']+list(map(str, size_list['aol'])))+'\n')
			for param, data_name, val_list in dict_output[alg_name]:
				if not data_name.startswith('synthetic'): val_list = [v-size for v,size in zip(val_list, size_list[data_name]) if v is not None]
				f.write('\t'.join([param, data_name] + list(map(str, val_list)))+'\n')
			f.write(''+'\n')



#attr_time_list = ['Result_0_Total_Time', 'Result_1_Initialize_Time', 'Result_2_Preprocess_Total_Time', 'Result_2_1_Estimation_Time', 'Result_3_1_Index_Building_Time', 'Result_3_2_Join_Time', 'Stat_CandQGramCountTime', 'Stat_FilterTime', 'Stat_EquivTime']
#attr_count_list = ['Stat_InvListCount', 'Stat_InvSize', 'Stat_CandQGramCount', 'Val_Comparisons', 'Val_Length_filtered', 'Val_PQGram_filtered',]
stat_header_list = ['Dataset', 'Model', 't_all', 't_init', 't_prep', 't_est', 't_index', 't_join', 't_tpq', 't_filter', 't_verify', 'n_result', 'avg_n_cand', 'n_verify', 'n_lenF', 'n_pqF']

#attr_count_list = ['Final Result Size', 'Stat_CandQGram_Avg', 'Stat_Equiv_Comparison', 'Val_Length_filtered', 'Val_PQGram_filtered',]

hybrid_stat_list = ['Estimate_Threshold', 'Estimate_Best_Time', 'Estimate_JoinMinSelected']


#attr_count_list = ['Stat_InvListCount', 'Stat_InvSize', 'Stat_CandQGramCount', 'Val_Comparisons', 'Val_Length_filtered', 'Val_PQGram_filtered', 'Auto_Best_Threshold', 'Auto_JoinMin_Selected']


def output_stat(cur, setting, size=None, data_name=None ):
	dict_alg_param_list = parse_multiple_settings(setting)
	if data_name is None: data_names = data_name_list
	else: data_names = [data_name]
	for data_name in data_names:
		output_path = join('stat', data_name+'_'+setting+'.stat')
		if exists(output_path): os.remove(output_path)
		for title, alg_param_list in dict_alg_param_list.items():
			aid_param_list = list(map(lambda x: (x[0], dict_alg.inv[x[0]], x[1]), alg_param_list))
			dict_result = load_exp_result_from_list(cur, data_name, aid_param_list, repeat=3)
			if size is None: size_list_tmp = size_list[data_name]
			else: size_list_tmp = [size]
#	if not exists(setting): mkdir(setting)
#	if data_name is None: data_names = data_name_list
#	else: data_names = [data_name]
#	for data_name in data_names:
#		dict_result = load_exp_result(data_name, setting)
			with open(output_path, 'a') as f:
				for _size in size_list_tmp:
					f.write('\t'.join(stat_header_list)+'\n')
					for target, result_list in dict_result.items():
						# target is a tuple of (alg, param).
						# result_list is the list of result dicts for every size of the current dataset.
						alg_name, param_str = target
						target = (dict_alg_name[alg_name], param_str)
						idx = size_list[data_name].index(_size)
						try: result = result_list[idx]
						except: 
							print("No result at %s, %s, %d"%(target,data_name,_size))
							continue
						time_list = list(map(lambda x: x if x is not None else 0, map(result.get, attr_time_list)))
						#time_list = time_list[0:5]+[time_list[5]+time_list[6]]+time_list[7:]
						count_list = list(map(lambda x: x if x is not None else 0, map(result.get, attr_count_list)))
						hybrid_stat = list(map(lambda x: str(x) if x is not None else '0', map(result.get, hybrid_stat_list)))
						print(' '.join(target), time_list+count_list+hybrid_stat)
						f.write('\t'.join(map(str, [data_name+'_'+str(_size), ' '.join(target)]+time_list+count_list+hybrid_stat))+'\n')



def diff_version( alg_name, ver0, ver1 ):
	ver_list = [ver0, ver1]
	list_param_str = ['']
	for key in dict_param[alg_name]:
		list_param_str_new = []
		for param_str in list_param_str:
			for val in dict_param[alg_name][key]:
				list_param_str_new.append(param_str+' '+'-%s %s'%(key, str(val)))
		list_param_str = list_param_str_new
	list_param_str = map(str.strip, list_param_str)

	aid_list = []
	output_list = []
	for ver in ver_list:
		cur.execute("select id, name, version from projectManager_algorithm where project_id=30 and name=\'%s\' and version=\'%s\'"%(alg_name, ver))
		rows = cur.fetchall()
		for row in rows: aid, name, ver = row[:3]
		output = []
		alg_param_list = [(alg_name+'v'+ver, aid, param_str) for param_str in list_param_str]
		for data_name in ['aol', 'sprot', 'usps', 'synthetic_10000', 'synthetic_100000']:
			dict_result = load_exp_result_from_list(cur, data_name, alg_param_list)
			for key, result_list in dict_result.items():
				for i in range(len(size_list[data_name])):
				#for size, result in zip(size_list[data_name], result_list):
					size = size_list[data_name][i]
					label = '_'.join(key)+'_%s_%d'%(data_name, size)
					try:
						result = result_list[i]
						val_str = '\t'.join([str(result[attr]) for attr in ['Result_0_Total_Time', 'Final Result Size', 'Stat_Equiv_Comparison']])
						#val_str = '\t'.join([str(result[attr]) for attr in ['Result_0_Total_Time', 'Final Result Size', 'Auto_Best_Threshold', 'Auto_JoinMin_Selected']]) # JoinHybridAll 2.63
#						val_str = '\t'.join([str(result[attr]) for attr in ['Result_0_Total_Time', 'Final Result Size', 'Estimate_Threshold', 'Estimate_JoinMinSelected']]) # JoinHybridAll 2.65

					except Exception as e: 
						print(e)
						val_str = '-\t-'
					output.append((label, val_str))
		output_list.append(output)
	
	with open('diff_ver_%s_v%s_v%s.txt'%(alg_name, ver0, ver1), 'w') as f:
		output0, output1 = output_list
		for i in range(len(output0)):
			val0 = output0[i][1]
			val1 = output1[i][1]
			f.write(output0[i][0]+'\t'+val0+'\t'+val1+'\n')





"""
A packaging function consisting of several subfunctions given a setting.
"""
def anaylze_setting(setting):
	# plot
	plot_one(setting, logscale=True)



if __name__ == '__main__':
	'''
	plot [setting]
	'''
	# connect to DB
	conn = sqlite3.connect('/home/yjpark/django/django-project/mysite/db.sqlite3')
	cur = conn.cursor()

	# load info of algorithms and datasets
	dict_alg, dict_data = load_metadata(cur)
	print("number of algorithms:",len(dict_alg))
	print("number of datasets:",len(dict_data))

	# draw plots
	#plot_one('compare_delta', logscale=True)
	#plot_one('mh_default', logscale=True)
	#plot_one('min_default', logscale=True)
	#plot_one('hybrid_default', logscale=True)
	#plot_one('mh_d0_vary_K', logscale=True)
	#plot_one('mh_d0_vary_q', logscale=True)
	#plot_one('min_d0_vary_K', logscale=True)
	#plot_one('min_d0_vary_q', logscale=True)
	#plot_one('mh_d1_vary_K', logscale=True)
	#plot_one('mh_d1_vary_q', logscale=True)
	#plot_one('min_d1_vary_K', logscale=True)
	#plot_one('min_d1_vary_q', logscale=True)
	#plot_one('compare_delta', attr='Mem_3_BuildIndex', logscale=False)
	#plot_one('compare_tmp')
	#plot_all('compare_delta', logscale=True)
	#plot_all('scalability', logscale=True, output_format='pdf')
	#plot_all('compare_min_fast', logscale=True, output_format='png')
	#plot_all('vary_K', data_name='aol', logscale=True, output_format='png')
	#plot_all('vary_q', data_name='aol', logscale=True, output_format='png')
	#plot_all('hybrid_1.01', logscale=True)
	#plot_all('compare_hybrid', logscale=True)
	#plot_all('compare_set', logscale=True)
	#plot_all('compare_pkduck', logscale=True)
	#plot_all('weak_strong', logscale=True)
	#plot_all('min_fast_vary_sample', logscale=True)
	#plot_all('hybrid2', logscale=True)

	# compare attributes
	#compare_attr('compare_seq', [lambda x, key=key:x.get(key) for key in [u'Val_Comparisons']])
	#compare_attr('compare_seq', [lambda x, key=key:x.get(key) for key in [u'Result_0_Total_Time', u'Result_1_Initialize_Time', u'Result_2_Preprocess_Total_Time']])
	#compare_acc()

	#output_stat('compare_delta')
	#output_stat('hybrid_default')
	#output_stat(cur, 'vary_K', size=100000)
	#output_stat(cur, 'vary_q', size=100000)
	#output_stat(cur, 'hybrid2')
	#output_stat('mh_d0_vary_K')
	#output_stat('mh_d0_vary_q')
	#output_stat('min_d0_vary_K')
	#output_stat('min_d0_vary_q')
	#output_stat('compare_naive')
	#output_stat('compare_tmp')
	#output_stat('compare_tmp', size=158489)
	#output_stat(cur, 'compare_hybrid')
	#output_stat(cur, 'scalability')
	output_stat(cur, 'hybrid2_vary_sample')
	#plot_one('naive_delta')

	#diff_version('JoinHybridAll2', '1.00', '1.01')


	conn.close()




