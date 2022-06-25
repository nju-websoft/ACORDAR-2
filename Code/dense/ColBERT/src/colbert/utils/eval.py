import sys
import pymysql
import pytrec_eval
import json

#todo
#是不是eval应该都是1没有2
run_path = "/home/ttling/ColBERT/experiments/lr_7e-06_batch_size_16/retrieve.py/2022-04-12_10.47.28/result.txt"
qrel_path = "/home/ttling/ColBERT/experiments/lr_7e-06_batch_size_16/retrieve.py/2022-04-12_10.47.28/qrel.txt"

qrel = {}
run = {}
with open(run_path,"r") as f:
    run = json.load(f)

with open(qrel_path,"r") as f:
    qrel = json.load(f)

evaluator = pytrec_eval.RelevanceEvaluator(
    qrel, {'map_cut', 'ndcg_cut'})

data = evaluator.evaluate(run)
ndcg_5=0
ndcg_10=0
map_5=0
map_10=0
for qid in data.keys():
    pids = data[qid]
    ndcg_5 += pids['ndcg_cut_5']
    ndcg_10 += pids['ndcg_cut_10']
    map_5 += pids['map_cut_5']
    map_10 += pids['map_cut_10']

print("ndcg_5:" + str(ndcg_5 / len(data)))
print("ndcg_10:" + str(ndcg_10 / len(data)))
print("map_5:" + str(map_5 / len(data)))
print("map_10:" + str(map_10 / len(data)))
# print(json.dumps(evaluator.evaluate(run), indent=1))