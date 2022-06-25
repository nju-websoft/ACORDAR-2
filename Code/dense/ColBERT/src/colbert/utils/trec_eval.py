import pytrec_eval
import pymysql

db=pymysql.connect(
    host='114.212.82.63',
    user='root',
    password='lzy199661',
    database='test_collection_2022apr',
    charset='utf8'
)

c=db.cursor()
datasets_pooled = []
with open("D:\DenseRetrieval\ColBERT\docs\datasets_pooled.txt") as f:
    for line in f.readlines():
        datasets_pooled.append(line.strip())

def calculate(split, method):

    qrel = {}

    sql = f'SELECT * FROM query_dataset_5folds WHERE split={split}'
    c.execute(sql)
    rel_list = c.fetchall()
    for rel in rel_list:
        query_id = str(rel[1])
        dataset_id = str(rel[2])
        rel_score = int(rel[3])
        if query_id not in qrel:
            qrel[query_id] = {}
        qrel[query_id][dataset_id] = rel_score
    # print(qrel)

    run = {}

    sql = f'SELECT * FROM result_5folds WHERE method=\'{method}\' AND split={split}'
    c.execute(sql)
    res_list = c.fetchall()
    for res in res_list:
        query_id = str(res[1])
        dataset_id = str(res[2])
        #todo
        if dataset_id not in datasets_pooled: continue
        score = float(res[6])
        if query_id not in run:
            run[query_id] = {}
        run[query_id][dataset_id] = score

    evaluator = pytrec_eval.RelevanceEvaluator(
        qrel, {'map_cut_5', 'ndcg_cut_5', 'map_cut_10', 'ndcg_cut_10'})

    metrics = evaluator.evaluate(run)
    # print(metrics)
    return metrics
