from colbert.utils.database import dashboard,analysis,cqs

#todo
#代码有问题
path = "D:\DenseRetrieval\ColBERT\experiments\lr_1e-06_batch_size_16\\retrieve.py\\2022-04-15_07.48.30\\real-ranking.tsv"


res = []




cqs.execute("select query_id,dataset_id from query_dataset_5folds_dense;")
data = cqs.fetchall()
for query_id,dataset_id in data:
    res.append(int(query_id)*int(dataset_id) )


with open(path,'r') as f:
    for line in f.readlines():
        line = line.split('\t')
        if res.__contains__(int(line[0]) * int(line[1])):
            continue
        else:
            print(line[0], line[1])