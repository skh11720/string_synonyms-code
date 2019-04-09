import pandas as pd


df = pd.read_csv('../tmp/DeltaHybridEstimationTest.txt', '\t')

"""
df.columns = Index(['alg', 'dataset', 'delta', 'sampleRatio', 't_idx', 't_gen', 't_filter',
       't_verify', 'n_strS', 'n_strT', 'sum_strLenT', 'sum_strLenT^d',
       'sum_appRule', 'sum_appRule^q', 'n_TPQ', 'n_verify', 'Unnamed: 16'],
      dtype='object')
"""

df['t_idx/n_strT'] = df['t_idx']/df['n_strT']
df['t_idx/sum_strLenT'] = df['t_idx']/df['sum_strLenT']

df['t_gen/sum_appRule'] = df['t_gen']/df['sum_appRule']
df['t_gen/sum_appRule^q'] = df['t_gen']/df['sum_appRule^q']
df['t_filter/n_TPQ'] = df['t_filter']/df['n_TPQ']
df['t_verify/n_verify'] = df['t_verify']/df['n_verify']
df['sel'] = df['n_verify']/df['n_strS']/df['n_strT']

df_avg = df.groupby(['alg', 'dataset', 'delta', 'sampleRatio']).mean()
df_avg.to_csv('../tmp/DeltaHybridEstimationTest_avg.txt', sep='\t')