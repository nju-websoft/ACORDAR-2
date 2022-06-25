from argparse import ArgumentParser
import itertools, os, re
from colbert.utils.database import cqs,db3

# parser = ArgumentParser(usage="from idx to dataset_id, max")
# parser.add_argument('--fold')
# parser.add_argument('--part')
# args = parser.parse_args()
lr_batch = "lr_1e-06_batch_size_16"
date_hour = "2022-04-30_10."

path = "../ColBERT/docs/datasets.txt"

idx2dataset = {}
idx = 0


with open(path, "r") as f:
    for line in f.readlines():
        docid = line.split("\t")[0]
        idx2dataset[idx] = docid
        idx2dataset[idx + 31589] = docid
        idx = idx + 1


# todo

def resort_write(rank_Path, out_path, fold, part):
    query2datasetscore = {}
    with open(rank_Path, 'r') as f:
        for line in f.readlines():
            line = line.split('\t')
            if query2datasetscore.__contains__(line[0]):
                dataset2score = query2datasetscore[line[0]]
                if dataset2score.__contains__(idx2dataset[int(line[1])]):
                    now = dataset2score[idx2dataset[int(line[1])]]
                    dataset2score[idx2dataset[int(line[1])]] = max(now, float(line[3]))
                    query2datasetscore[line[0]] = dataset2score
                else:
                    dataset2score[idx2dataset[int(line[1])]] = float(line[3])
                    query2datasetscore[line[0]] = dataset2score

            else:
                dataset2score = {}
                dataset2score[idx2dataset[int(line[1])]] = float(line[3])
                query2datasetscore[line[0]] = dataset2score

    with open(out_path, 'w') as f:
        for query, dataset2score in query2datasetscore.items():
            # dataset2score按照score排序
            dataset2score = sorted(dataset2score.items(), key=lambda d: d[1], reverse=True)
            cnt = 1
            for dataset, score in dataset2score:
                f.write(query+"\t"+dataset.strip()+"\t"+str(cnt)+"\t"+str(score)+"\n")
                # todo 加入database
                # print(f'''{query},{dataset.strip()},{tmp},{cnt},{fold},{score}''')
                # sql = f'''INSERT INTO result_5folds(query_id, dataset_id,method,ranknum,split,score) VALUES ({int(query)},{int(dataset.strip())},'{part}',{cnt},{fold},{score})'''
                # cqs.execute(sql)

                cnt = cnt + 1
                if cnt > 100: break



candidate_part = ["all", "meta", "data"]
candidate_fold = ["0", "1", "2", "3", "4"]
pattern = re.compile(r'2022-04-30_17.[0-9][0-9]\.[0-9][0-9]')
for part, fold in itertools.product(candidate_part, candidate_fold):
    rank_Path_dir = f'''../ColBERT/experiments/{part}/{fold}/{lr_batch}/retrieve.py/'''  # 2022-04-30_10.59.47/ranking.tsv
    listdir = os.listdir(rank_Path_dir)
    assert len(listdir) == 1
    print(listdir)
    if pattern.match(listdir[0]):
        rank_Path = rank_Path_dir + listdir[0]
    else:
        rank_Path = "null"

    if part == "all":
        tmp = 'ColBERT'
    else:
        tmp = 'ColBERT [' + part[0] + ']'
    resort_write(rank_Path + '/ranking.tsv', rank_Path + '/real-ranking-100.tsv', fold, tmp)

# db3.commit()
