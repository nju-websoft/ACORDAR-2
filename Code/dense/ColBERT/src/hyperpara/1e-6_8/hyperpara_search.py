import itertools
import logging
import os
import subprocess as sp
import threading

logger = logging.getLogger(__name__)


def get_gpu_memory():
    _output_to_list = lambda x: x.decode('ascii').split('\n')[:-1]
    COMMAND = "nvidia-smi --query-gpu=memory.free --format=csv"
    memory_free_info = _output_to_list(sp.check_output(COMMAND.split()))[1:]
    memory_free_values = [int(x.split()[0]) for i, x in enumerate(memory_free_info)]
    return memory_free_values


def free_gpu_ids():
    # usable_gpu_ids = open('usable_gpu_ids', 'r', encoding='utf-8').read().strip()
    # usable_gpu_ids = list(map(int, usable_gpu_ids.split(',')))
    usable_gpu_ids = [0,1,2,3,4,5]
    assert min(usable_gpu_ids) >= 0 and max(usable_gpu_ids) <= 6
    ids = []
    memory_free_values = get_gpu_memory()
    for i in range(len(memory_free_values)):
        if memory_free_values[i] > 10000:
            ids.append(i)
    ids = list(filter(lambda x: x in usable_gpu_ids, ids))
    return ids


# 这两个什么意思？
# --per_device_eval_batch_size {batch_size} \
# --per_device_train_batch_size 1 \

# --save_steps 290 \  为什么还要保存，不是每个epoch结束看情况吗

# --logging_steps 200 \ 输出当前损失？

lr=1e-6
batch_size=8
# 返回的执行这个文件的log？return？ 没有返回
def construct_script(gpu_id, part, fold):
    script = f'''
CUDA_VISIBLE_DEVICES={gpu_id[0]} python -m colbert.train \
  --amp \
  --checkpoint ../ColBERT/downloads/colbertv2.0.dnn \
  --doc_maxlen 512 \
  --query_maxlen 64 \
  --mask-punctuation \
  --bsize {batch_size} \
  --triples ../ColBERT/docs/{part}/pairs_{part}_{fold[0]}.jsonl \
  --root ../ColBERT/experiments/{part}/{fold[2]}/ \
  --collection ../ColBERT/docs/corpus_all.tsv \
  --queries ../ColBERT/docs/queries_{fold[0]}.tsv \
  --valid_triples ../ColBERT/docs/{part}/pairs_{part}_{fold[1]}.jsonl \
  --valid_queries ../ColBERT/docs/queries_{fold[1]}.tsv \
  --experiment lr_{lr}_batch_size_{batch_size} \
  --similarity cosine \
  --run 4.25 \
  --epoch 1 \
  --lr {lr} \
'''
    return script


def run(script_name):
    logger.info(f'start {script_name}...')
    os.system(f'/bin/sh ../ColBERT/script_name/{script_name}')



''' 待搜索的超参范围 '''
# candidate_lr = [ 7e-6, 3e-6, 1e-6]
# candidate_batch_size = [16, 8]
# candidate_lr = [3e-6]
# candidate_batch_size = [16]
candidate_part = ["all"]
candidate_fold = [("123","4","0"),("234","0","1"),("340","1","2"),("401","2","3")]

# candidate_lr = [3e-6, 7e-6, 1e-6]
# candidate_batch_size = [16, 8]

# candidate_similarity = ['l2','cosine']
# 不懂的
# --mask-punctuation
# --accum   这个还需要调吗？
# --amp


all_threads = []
gpu_ids = free_gpu_ids()
for part,fold in itertools.product(candidate_part, candidate_fold):


    while len(gpu_ids) < 1:
        os.system('sleep 30')
        gpu_ids = free_gpu_ids()

    print(gpu_ids)
    logger.info(f'free gpu: {gpu_ids}')
    script = construct_script(gpu_id=gpu_ids[:1], part=part,fold=fold)
    gpu_ids = gpu_ids[1:]
    script_name = f'{part}_{fold[2]}_lr_{lr}_batch_size_{batch_size}.sh'
    output = open(f'../ColBERT/script_name/{script_name}', 'w', encoding='utf-8')
    output.write(script)
    output.close()

    x = threading.Thread(target=run, args=(script_name,))
    all_threads.append(x)
    x.start()
    os.system('sleep 30')

for i in range(len(all_threads)):
    all_threads[i].join()

