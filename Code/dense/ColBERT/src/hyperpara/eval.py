import os
import itertools, logging, threading
import subprocess as sp

logger = logging.getLogger(__name__)

path = "/home/ttlin/ColBERT/experiments/"

candidate_part = ["all"] # , "meta", "data"
candidate_fold = ["0"]  # todo ,"1","2","3","4"
candidate_lr = ["7e-06"]
candidate_batch = ["8", "16"]


#todo idx_2_meta_cont -> get_file -> eval

def get_max_checkpoint(path):
    ansEval = float('inf')
    ansLR = 0
    ansBatch = 0
    for lr, batch in itertools.product(candidate_lr, candidate_batch):
        eval_path = f'''{path}/lr_{lr}_batch_size_{batch}/train.py/4.25/eval_loss.txt'''
        with open(eval_path, 'r') as f:
            eval_loss = float(f.readline().split("\t")[1])
            # print(f'''{part}\t{fold}\t{lr}\t{batch}\t{eval_loss}\n''')
            if eval_loss < ansEval:
                ansEval = eval_loss
                ansLR = lr
                ansBatch = batch

    # print(f'''{part}\t{fold}\t{ansLR}\t{ansBatch}\t{ansEval}\n''')
    point_path = f'''{path}/lr_{ansLR}_batch_size_{ansBatch}/train.py/4.25/checkpoints/colbert-0.dnn'''
    # print(point_path)



for part, fold in itertools.product(candidate_part, candidate_fold):
    get_max_checkpoint(f'''{path}{part}/{fold}''')