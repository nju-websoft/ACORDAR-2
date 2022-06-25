from colbert.utils.database import cqs
import itertools


candidate_k = [5,10]
candidate_fold = [0,1,2,3,4]
candidate_part = ['ColBERT','ColBERT [d]','ColBERT [m]']




for part in candidate_part:
    for fold in range(5):

        sql = f'''SELECT AVG(ndcg_score) FROM trec_eval_ndcg_pooled WHERE k=5 and method='{part}' and fold = {fold}'''
        cqs.execute(sql)
        data = cqs.fetchone()
        print(str(data[0])+"\t",end="")

        sql = f'''SELECT AVG(ndcg_score) FROM trec_eval_ndcg_pooled WHERE k=10 and method='{part}' and fold = {fold}'''
        cqs.execute(sql)
        data = cqs.fetchone()
        print(str(data[0]) + "\t", end="")

        sql = f'''SELECT AVG(ap_score) FROM trec_eval_map_pooled WHERE k=5 and method='{part}' and fold = {fold}'''
        cqs.execute(sql)
        data = cqs.fetchone()
        print(str(data[0]) + "\t",end="")

        sql = f'''SELECT AVG(ap_score) FROM trec_eval_map_pooled WHERE k=10 and method='{part}' and fold = {fold}'''
        cqs.execute(sql)
        data = cqs.fetchone()
        print(str(data[0]) + "\t\n",end="")
    print("\n")
