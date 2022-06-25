

data_path = "/home/ttling/ColBERT/docs/adata.tsv"
meta_path = "/home/ttling/ColBERT/docs/ameta.tsv"
out_path = "/home/ttling/ColBERT/docs/answer.tsv"

querydataset2meta = {}
querydataset2data = {}
querydataset2all = {}


query2meta = {}
query2data = {}








with open(data_path, 'r') as f:
    # data = f.read().splitlines()
    for line in f.readlines():
        line = line.strip().split("\t")

        querydataset2data[(line[0],line[1])] = line[3]

with open(meta_path, 'r') as f:
    for line in f.readlines():

        line = line.strip().split("\t")
        # print(len(line))
        # print(line)
        querydataset2meta[(line[0], line[1])] = line[3]

for key, score in querydataset2meta.items():
    if querydataset2data.get(key) is None:
        querydataset2all[key] = float(score)
        continue
    querydataset2all[key] = float(score) + float(querydataset2data[key])
    querydataset2data.pop(key)

for key,score in querydataset2data.items():
    querydataset2all[key] = float(querydataset2data[key])

#要知道排序，才能
query2all = {}
for (query,dataset_id), score in querydataset2all.items():
    if query2all.get(query) is None: tmp = []
    else: tmp = query2all.get(query)

    tmp.append((dataset_id,score))
    query2all[query] = tmp

with open(out_path,'w') as f:
    for query, li in query2all.items():
        li.sort(key=lambda x:x[1])
        li.reverse()
        idx=1
        for da, s in li :
            f.write(query+'\t'+da+'\t'+str(idx)+'\t'+str(s)+'\n')
            idx=idx+1

