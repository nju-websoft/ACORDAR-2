import itertools
import logging
import os
import subprocess as sp
import threading

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel(level=logging.INFO)

formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
# create console handler for logger.
ch = logging.StreamHandler()
ch.setLevel(level=logging.DEBUG)
ch.setFormatter(formatter)

# add handlers to logger.
logger.addHandler(ch)

def get_gpu_memory():
    _output_to_list = lambda x: x.decode('ascii').split('\n')[:-1]
    COMMAND = "nvidia-smi --query-gpu=memory.free --format=csv"
    memory_free_info = _output_to_list(sp.check_output(COMMAND.split()))[1:]
    memory_free_values = [int(x.split()[0]) for i, x in enumerate(memory_free_info)]
    return memory_free_values


def free_gpu_ids():
    usable_gpu_ids = open('usable_gpu_ids.txt', 'r', encoding='utf-8').read().strip()
    usable_gpu_ids = list(map(int, usable_gpu_ids.split(',')))
    # usable_gpu_ids=[0,1,2,3,4]
    assert min(usable_gpu_ids) >= 0 and max(usable_gpu_ids) <= 7
    ids = []
    memory_free_values = get_gpu_memory()
    for i in range(len(memory_free_values)):
        if memory_free_values[i] >= 28000: # 20000
            ids.append(i)
    ids = list(filter(lambda x: x in usable_gpu_ids, ids))
    return ids


def construct_script(gpu_ids, lr=2e-5, batch_size=8, cp_path="abc", fold=[0,1,2,3,4], use="metadata"):
    """for metadata"""
    script = f'''
cp_path="{cp_path}"
train_path="/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/{use}/$cp_path"
pretrain_file_path="/home/wqluo/code/my-dpr/dpr/downloads/checkpoint/retriever/single-adv-hn/nq/bert-base-encoder.cp"

CUDA_VISIBLE_DEVICES={gpu_ids[0]} \
python train_dense_encoder.py \
train_datasets=[ds_{fold[0]}_{use},ds_{fold[1]}_{use},ds_{fold[2]}_{use}] \
dev_datasets=[ds_{fold[3]}_{use}] \
train=biencoder_local \
output_dir=$train_path \
my_batch_size={batch_size} \
my_learning_rate={lr} \
ignore_checkpoint_optimizer=true \
model_file=$pretrain_file_path

best_cp_name=$(awk -v FS='\\t' '{{print $1}}' "$train_path/best_cp_name")

if [ ! -f $best_cp_name ]; then
    echo "best_cp_name not found!"
else
    find $train_path ! \( -wholename $best_cp_name -o -name "best_cp_name" \)  -type f -exec rm -f {{}} +
fi

'''
    return script


def run(script_name):
    logger.info(f'start {script_name}...')
    os.system(f'bash scripts_for_train/{script_name}')



''' hyper parameters to grid search '''
candidate_lr =  [2e-5, 1e-5, 1e-6, 2e-6] # [5e-6, 7e-6, 1e-5]
candidate_batch_size = [2, 4, 8] # [16, 8 ,4, 2]
uses = ["both"]# ["content", "metadata"]
# candidate_seed = [123, 1234, 42, 1,2,3,4,5]
# candidate_epoch = [5,10,15]

folds = [[0,1,2,3,4],[1,2,3,4,0],[2,3,4,0,1],[3,4,0,1,2],[4,0,1,2,3]]

all_threads = []
gpu_ids = free_gpu_ids()
gpu_id=0


# failed_try = [(1e-6, 8, folds[4]), (2e-6, 2, folds[0]), (2e-6, 4, folds[1]), (2e-6, 4, folds[4])]
for use, lr, batch_size, fold in itertools.product(uses, candidate_lr, candidate_batch_size, folds):
# for lr, batch_size, fold in failed_try:
    logger.info(f'free gpu: {gpu_ids}')
    print("args: ", lr, batch_size, fold)
    while len(gpu_ids) < 1:
        os.system('sleep 300')
        gpu_ids = free_gpu_ids()

    
    # gpu_ids = [4]

    script_name = f'ft_lr_{lr}_batch_size_{batch_size}_fold_{fold[0]}.sh'
    cp_path = script_name[:-3]
    if os.path.exists(f"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/{use}/{cp_path}/best_cp_name") \
        or os.path.exists(f"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/{use}/best_dirs/{cp_path}/best_cp_name"):
        continue
    script = construct_script(gpu_ids=gpu_ids, lr=lr, batch_size=batch_size, cp_path=cp_path, fold=fold, use=use)
    gpu_ids = gpu_ids[1:]
    output = open(f'scripts_for_train/{script_name}', 'w+', encoding='utf-8')
    output.write(script)
    output.close()

    x = threading.Thread(target=run, args=(script_name,))
    all_threads.append(x)
    x.start()
    os.system('sleep 10')


for i in range(len(all_threads)):
    all_threads[i].join()

