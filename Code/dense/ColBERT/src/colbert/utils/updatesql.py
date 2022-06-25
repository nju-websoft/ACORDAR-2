import colbert.utils.trec_eval as trec_eval
import pymysql

db=pymysql.connect(
    host='114.212.82.63',
    user='root',
    password='lzy199661',
    database='test_collection_2022apr',
    charset='utf8'
)

c=db.cursor()

# methods = ['BM25', 'BM25 [d]', 'BM25 [m]', 'TFIDF', 'TFIDF [d]', 'TFIDF [m]', 'LMD', 'LMD [d]', 'LMD [m]']

methods = ['ColBERT', 'ColBERT [d]', 'ColBERT [m]']

def updatesql(method, metrics, split, fold):
    sql = f'SELECT query_id FROM query_5folds WHERE split={split}'
    c.execute(sql)
    query_ids=c.fetchall()
    for query_id in query_ids:
        query_id = str(query_id[0])
        ndcg_cut_5 = 0.0
        ndcg_cut_10 = 0.0
        map_cut_5 = 0.0
        map_cut_10 = 0.0
        if query_id in metrics:
            ndcg_cut_5 = metrics[query_id]['ndcg_cut_5']
            ndcg_cut_10 = metrics[query_id]['ndcg_cut_10']
            map_cut_5 = metrics[query_id]['map_cut_5']
            map_cut_10 = metrics[query_id]['map_cut_10']

        # print(f'''{method},{query_id},5,{ndcg_cut_5},{fold}''')
        # print(f'''{method},{query_id},10,{ndcg_cut_10},{fold}''')
        # print(f'''{method},{query_id},5,{map_cut_5},{fold}''')
        # print(f'''{method},{query_id},10,{map_cut_10},{fold}''')
        sql = f'INSERT INTO trec_eval_ndcg_pooled VALUES(\'{method}\',{query_id},5,{ndcg_cut_5},{fold})'
        c.execute(sql)
        sql = f'INSERT INTO trec_eval_ndcg_pooled VALUES(\'{method}\',{query_id},10,{ndcg_cut_10},{fold})'
        c.execute(sql)
        sql = f'INSERT INTO trec_eval_map_pooled VALUES(\'{method}\',{query_id},5,{map_cut_5},{fold})'
        c.execute(sql)
        sql = f'INSERT INTO trec_eval_map_pooled VALUES(\'{method}\',{query_id},10,{map_cut_10},{fold})'
        c.execute(sql)
    db.commit()

if __name__=='__main__':
    for i in range(5):
        for m in methods:
            metrics = trec_eval.calculate(i, m)
            updatesql(m, metrics, i, (i + 1) % 5)