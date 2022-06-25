import os
import itertools, logging, threading
import subprocess as sp

logger = logging.getLogger(__name__)

step = 2  # index_faiss:1, retrieve:2

path = "/home/ttlin/ColBERT/experiments/"


# all 3 4   data 0123   meta 0134
candidate_part = ["all", "meta", "data"] #
candidate_fold = ["0","1","2","3","4"]  # todo ,
candidate_lr = ["1e-06"] #"7e-06","3e-06",
candidate_batch = ["16"] #"8",


def get_gpu_memory():
    _output_to_list = lambda x: x.decode('ascii').split('\n')[:-1]
    COMMAND = "nvidia-smi --query-gpu=memory.free --format=csv"
    memory_free_info = _output_to_list(sp.check_output(COMMAND.split()))[1:]
    memory_free_values = [int(x.split()[0]) for i, x in enumerate(memory_free_info)]
    return memory_free_values


def free_gpu_ids():
    # usable_gpu_ids = open('usable_gpu_ids', 'r', encoding='utf-8').read().strip()
    # usable_gpu_ids = list(map(int, usable_gpu_ids.split(',')))
    usable_gpu_ids = [0, 1, 2, 3, 4, 5,6,7]
    assert min(usable_gpu_ids) >= 0 and max(usable_gpu_ids) <= 8

    ids = []
    memory_free_values = get_gpu_memory()
    for i in range(len(memory_free_values)):
        if memory_free_values[i] > 30000:
            ids.append(i)
    ids = list(filter(lambda x: x in usable_gpu_ids, ids))
    return ids


all_threads = []
gpu_ids = free_gpu_ids()


def construct_script(gpu_id, path, part, fold, lr, batch):
    script = ""
    if step == 0:
        script = f'''
        CUDA_VISIBLE_DEVICES={gpu_id[0]} \
        python -m colbert.index \
        --amp \
        --doc_maxlen 512 \
        --mask-punctuation \
        --bsize 32 \
        --checkpoint {path} \
        --collection docs/corpus_all.tsv \
        --index_root indexes/{part}/{fold}/ \
        --index_name lr_{lr}_batch_size_{batch} \
        --root experiments/{part}/{fold} \
        --experiment lr_{lr}_batch_size_{batch} \
        '''
    elif step == 1:
        script = f'''
                CUDA_VISIBLE_DEVICES={gpu_id[0]} \
python -m colbert.index_faiss \
--index_root indexes/{part}/{fold}/ \
--index_name lr_{lr}_batch_size_{batch} \
--partitions 16384 \
--sample 0.3 \
--root experiments/{part}/{fold} \
--experiment lr_{lr}_batch_size_{batch} \
                '''
    elif step == 2:
        script = f'''
                CUDA_VISIBLE_DEVICES={gpu_id[0]} \
python -m colbert.retrieve \
--amp \
--doc_maxlen 512 \
--mask-punctuation \
--queries docs/queries_{fold}.tsv \
--partitions 16384 \
--faiss_depth 1024 \
--index_root indexes/{part}/{fold} \
--index_name lr_{lr}_batch_size_{batch} \
--checkpoint {path} \
--root experiments/{part}/{fold} \
--experiment lr_{lr}_batch_size_{batch} \
                '''

    return script


def run(script_name):
    logger.info(f'start {script_name}...')
    os.system(f'/bin/sh ../ColBERT/script_name/{script_name}')


def run_index(path, part, fold, lr, batch):
    global gpu_ids
    while len(gpu_ids) < 1:
        os.system('sleep 30')
        gpu_ids = free_gpu_ids()

    print(gpu_ids)
    logger.info(f'free gpu: {gpu_ids}')
    script = construct_script(gpu_id=gpu_ids[:1], path=path, part=part, fold=fold, lr=lr, batch=batch)
    gpu_ids = gpu_ids[1:]
    script_name = f'{step}_index_{part}_{fold}_lr_{lr}_batch_size_{batch}.sh'
    output = open(f'../ColBERT/script_name/{script_name}', 'w', encoding='utf-8')
    output.write(script)
    output.close()

    x = threading.Thread(target=run, args=(script_name,))
    all_threads.append(x)
    x.start()
    os.system('sleep 90')


# def run_index_faiss( path,part, fold,lr,batch):
#     while len(gpu_ids) < 1:
#         os.system('sleep 30')
#         gpu_ids = free_gpu_ids()
#
#     print(gpu_ids)
#     logger.info(f'free gpu: {gpu_ids}')
#     script = construct_script(gpu_id=gpu_ids[:1], path=path,part=part,fold=fold,lr=lr,batch=batch)
#     gpu_ids = gpu_ids[1:]
#     script_name = f'index_{part}_{fold[2]}_lr_{lr}_batch_size_{batch}.sh'
#     output = open(f'../ColBERT/script_name/{script_name}', 'w', encoding='utf-8')
#     output.write(script)
#     output.close()
#
#     x = threading.Thread(target=run, args=(script_name,))
#     all_threads.append(x)
#     x.start()
#     os.system('sleep 90')
#
# def run_index( path,part, fold,lr,batch):
#     while len(gpu_ids) < 1:
#         os.system('sleep 30')
#         gpu_ids = free_gpu_ids()
#
#     print(gpu_ids)
#     logger.info(f'free gpu: {gpu_ids}')
#     script = construct_script(gpu_id=gpu_ids[:1], path=path,part=part,fold=fold,lr=lr,batch=batch)
#     gpu_ids = gpu_ids[1:]
#     script_name = f'index_{part}_{fold[2]}_lr_{lr}_batch_size_{batch}.sh'
#     output = open(f'../ColBERT/script_name/{script_name}', 'w', encoding='utf-8')
#     output.write(script)
#     output.close()
#
#     x = threading.Thread(target=run, args=(script_name,))
#     all_threads.append(x)
#     x.start()
#     os.system('sleep 90')

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

    print(f'''{part}\t{fold}\t{ansLR}\t{ansBatch}\t{ansEval}''')
    point_path = f'''{path}/lr_{ansLR}_batch_size_{ansBatch}/train.py/4.25/checkpoints/colbert-0.dnn'''
    # print(point_path)
    run_index(point_path, part, fold, ansLR, ansBatch)


for part, fold in itertools.product(candidate_part, candidate_fold):
    get_max_checkpoint(f'''{path}{part}/{fold}''')
