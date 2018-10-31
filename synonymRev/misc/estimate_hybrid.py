import plot
from plot import *



# connect to DB
conn = sqlite3.connect('/home/yjpark/django/django-project/mysite/db.sqlite3')
cur = conn.cursor()

# load info of algorithms and datasets
dict_alg, dict_data = load_metadata(cur)
attr_list = ['Estimate_Coeff1_Naive', 'Estimate_Coeff2_Naive', 'Estimate_Term1_Naive', 'Estimate_Term2_Naive', 'Estimate_Time_Naive', 'Estimate_Coeff1_Mh', 'Esitmate_Coeff2_Mh', 'Estimate_Coeff3_Mh', 'Estimate_Term1_Mh', 'Estimate_Term2_Mh', 'Estimate_Term3_Mh', 'Estimate_Time_Mh', 'Estimate_Coeff1_Min', 'Estimate_Coeff2_Min', 'Estimate_Coeff3_Min', 'Estimate_Term1_Min', 'Estimate_Term2_Min', 'Estimate_Term3_Min', 'Estimate_Time_Min', 'Result_0_Total_Time', 'Estimate_JoinMinSelected', 'Estimate_Threshold']
ratio_list = ['0.01', '0.02', '0.05', '0.1']

def output_varying_size(data_name, sample_ratio):
	#data_name = 'usps'
	#sample_ratio = '1.00'
	key = ('JoinHybridAll2', '-K 1 -qSize 2 -sampleH 0.02 -sampleB 0.02')

	alg_param_list = [key]
	aid_param_list = list(map(lambda x: (x[0], dict_alg.inv[x[0]], x[1]), alg_param_list))

	with open('SampleEst_'+data_name+'_'+sample_ratio+'.txt', 'w') as f:
		for alg_name, aid, param_str in aid_param_list:
			for did, size in zip(did_list[data_name], size_list[data_name]):
				result_list = load_exp_result_aux(cur, aid, did, param_str, 3)
				for i in range(len(result_list)):
					f.write(alg_name+'\t')
					f.write(str(size)+'\t')
					for attr in attr_list:
						f.write(str(result_list[i][attr])+'\t')
					f.write('\n')


def output_varying_ratio(data_name, size_idx=5): # size=100K
	for data_name in data_name_list:
		with open('SampleEst_vary_ratio_'+data_name+'.txt', 'w') as f:
			for sample_ratio in ratio_list:
				key = ('JoinHybridAll', '-K 1 -qSize 2 -sample '+sample_ratio)
				alg_param_list = [key]
				aid_param_list = list(map(lambda x: (x[0], dict_alg.inv[x[0]], x[1]), alg_param_list))
				dict_result = load_exp_result_from_list(cur, data_name, aid_param_list)
				result_list = dict_result[key]
				i = size_idx
				if len(result_list) <= i: continue
				#f.write(sample_ratio+'\t')
				for attr in attr_list:
					f.write(str(result_list[i][attr])+'\t')
				f.write('\n')


#for data_name in ['aol', 'sprot', 'usps']:
	#output_varying_ratio(data_name)
output_varying_size('aol', '0.01')
