from colbert.utils.database import cqs
import json
import pandas as pd

dataset_path = "/home/ttling/ColBERT/docs/datasets.txt"
ranking_path = "/home/ttling/ColBERT/experiments/lr_7e-06_batch_size_16/retrieve.py/2022-04-13_10.03.52/real-ranking.tsv"
output_path = "/home/ttling/ColBERT/experiments/lr_7e-06_batch_size_16/retrieve.py/2022-04-13_10.03.52/result.txt"
qrel_path = "/home/ttling/ColBERT/experiments/lr_7e-06_batch_size_16/retrieve.py/2022-04-13_10.03.52/qrel.txt"
# map_path = "/home/ttling/ColBERT/docs/corpus_all.tsv"
result = {}
map = []

# idx2dataset = {}
# idx=0
# with open(dataset_path,"r") as f:
#     for line in f.readlines():
#         docid = line.split("\t")[0]
#         idx2dataset[idx]=docid
#         idx2dataset[idx+31589]=docid
#         idx=idx+1


def get_queries(fold):
    cqs.execute("SELECT query_text,query_id FROM query_5folds WHERE split="+fold)
    results = cqs.fetchall()
    for r in results:
        text,id =r

def get_runs():
    with open(output_path,'r') as fp:
        result = json.load(fp)
    data = {}
    for qid in result.keys():
        tmp = {}
        rels = get_rel_for_qid_pid(qid);
        for dataset_id,rel_score in rels:
            tmp[dataset_id] = rel_score
        data[qid]=tmp

    with open(qrel_path,'w+') as fp:
        json.dump(data,fp,indent=4)



#todo 有问题，没有看metadata和content一起的id，需重写
def get_rel_for_qid_pid (qid):
    cqs.execute("SELECT dataset_id,rel_score FROM query_dataset_without_badquery WHERE query_id="+qid);
    rel = cqs.fetchall()
    return rel


# def get_map():
#     # map.append("Os dados da seção contêm informações sobre os cursos de pós-graduação stricto sensu no Brasil.    Esta versão apresenta os metadados para cursos de pós-graduação stricto sensu dos anos de 2017 a 2019, compreendendo os dados parciais do período de Avaliação e será atualizada até completar-se os quatro anos (2017-2020) do ciclo, finalizando em 2021, ano da próxima Avaliação Quadrienal.    Nova versão será apresentada com atualização em decorrência de reabertura de calendário de envio de dados do Coleta pelos Programas de pós-graduação, referente aos anos de 2017 a 2019.	[2017 a 2020] Cursos da Pós-Graduação Stricto Sensu no Brasil")
#     with open(map_path,"r") as f:
#
#         for line in f.readlines():
#             line = line.strip().split("\t")
#             # if line[0]=="1":
#             #     continue
#             map.append(line[0])


#还是人为设置的passage_id
def get_results():
    with open(ranking_path,"r") as f:
        pre=-1
        list = []

        for line in f.readlines():
            line = line.strip().split("\t")

            if(int(line[2])>10):
                continue


            if(line[0]!=pre and pre!=-1):

                tmp = {}
                for l in list:
                    # print(l[0])
                    tmp[int(l[0])]=l[1]
                    # tmp[l[0]] = l[1]
                result[pre] = tmp
                list=[]
                list.append((line[1], int(line[2]), float(line[3])))
            else:
                list.append((line[1],int(line[2]),float(line[3])))

            pre = line[0]

    with open(output_path,'w+') as fp:
        json.dump(result, fp, indent=4)


# get_map()
# print(map)
get_results()
get_runs()