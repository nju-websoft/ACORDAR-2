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
    # usable_gpu_ids=[3,4,5,6]
    assert min(usable_gpu_ids) >= 0 and max(usable_gpu_ids) <= 7
    ids = []
    memory_free_values = get_gpu_memory()
    for i in range(len(memory_free_values)):
        if memory_free_values[i] >= 16000: # 20000
            ids.append(i)
    ids = list(filter(lambda x: x in usable_gpu_ids, ids))
    return ids


def construct_script(gpu_ids, ckpt_path, query_fold_num, use):
    script = f'''
model_type="finetune"
data_type="{use}"
data_type1="metadata"
data_type2="content"
embedding_name="embedding_512_bench_ft_{use}_query_fold_{query_fold_num}"

ctx_source1="dataset_ctx_${{data_type1}}" # "dataset_ctx_${{data_type}}", for both method
ctx_source2="dataset_ctx_${{data_type2}}" # "dataset_ctx_${{data_type}}", for both method

ctx_source_both="[$ctx_source1,$ctx_source2]"
ctx_source_metadata="[$ctx_source1]"
ctx_source_content="[$ctx_source2]"

embedding_path1="/home/wqluo/code/my-dpr/my_output/embeddings/${{data_type1}}/${{model_type}}/${{embedding_name}}"
embedding_path2="/home/wqluo/code/my-dpr/my_output/embeddings/${{data_type2}}/${{model_type}}/${{embedding_name}}"

embedding_path_both=[\"$embedding_path1\",\"$embedding_path2\"]
embedding_path_metadata=[\"$embedding_path1\"]
embedding_path_content=[\"$embedding_path2\"]


res_id="bench_512_ft_{use}_query_fold_{query_fold_num}"
res_name="res_${{res_id}}.json"
QUERY_SOURCE=query_fold{query_fold_num} # query_all # "query_split5"  # "query_pre" # query
retrieve_res_path="/home/wqluo/code/my-dpr/my_output/retrieve_results/${{data_type}}/${{model_type}}/${{res_name}}"

model_file_path={ckpt_path}
embedding(){{
	CUDA_VISIBLE_DEVICES={gpu_ids[0]} python generate_dense_embeddings.py \\
	model_file=$model_file_path \\
	ctx_src=$1 \\
	out_file=$2 \\
	insert_title=False
}}

QUERY_SOURCE=query_fold{query_fold_num}
retrieve(){{
	CUDA_VISIBLE_DEVICES={gpu_ids[0]} python dense_retriever.py \\
	model_file=$model_file_path \\
	qa_dataset=$QUERY_SOURCE \\
	ctx_datatsets=$ctx_source_{use} \\
	encoded_ctx_files=$embedding_path_{use} \\
	out_file=$retrieve_res_path
}}

echo "===================== embedding start ====================="
if [ $data_type == "both" ]; then
    embedding $ctx_source1 $embedding_path1
    embedding $ctx_source2 $embedding_path2
elif [ $data_type == "metadata" ]; then
    embedding $ctx_source1 $embedding_path1
elif [ $data_type == "content" ]; then
    embedding $ctx_source2 $embedding_path2
else
    echo "not supported data_type!"
fi

echo "===================== retrieve start ====================="
retrieve

'''
    return script


def run(script_name):
    logger.info(f'start {script_name}...')
    os.system(f'bash scripts_for_pipeline_corrected/{script_name}')

folds = [[0,1,2,3,4],[1,2,3,4,0],[2,3,4,0,1],[3,4,0,1,2],[4,0,1,2,3]]

all_threads = []
gpu_ids = free_gpu_ids()
gpu_id=0

best_ckpt_paths_both = [
"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/ft_lr_2e-05_batch_size_4_fold_0/dpr_biencoder.37",
"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/ft_lr_2e-05_batch_size_4_fold_1/dpr_biencoder.35",
"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/ft_lr_2e-05_batch_size_2_fold_2/dpr_biencoder.33",
"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/ft_lr_2e-05_batch_size_2_fold_3/dpr_biencoder.35",
"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/ft_lr_2e-05_batch_size_4_fold_4/dpr_biencoder.39"
]


# for lr, batch_size, fold in itertools.product(candidate_lr, candidate_batch_size, folds):
# for use in ["content", "metadata"]:
for fold in range(0, 5):
    use = "both" # "content"
    # fold = 3
    logger.info(f'free gpu: {gpu_ids}')
    best_ckpt_dir = f"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/{use}/best_dirs"
    dirs = os.listdir(best_ckpt_dir)
    for item in dirs:
        if item[-6:] == f"fold_{fold}":
            for _ in os.listdir(os.path.join(best_ckpt_dir, item)):
                if _[:4] == "dpr_":
                    best_ckpt_path = os.path.join(best_ckpt_dir, item, _)
            break

    print("best", best_ckpt_path)
    assert os.path.isfile(best_ckpt_path)

    query_fold = folds[fold][4]
    print("fold: ", best_ckpt_path, query_fold)
    while len(gpu_ids) < 1:
        os.system('sleep 300')
        gpu_ids = free_gpu_ids()

    script_name = f'pipeline_best_ckpt_{use}_query_fold_{query_fold}.sh'
    cp_path = script_name[:-3]
    # if os.path.isdir(f"/home/wqluo/code/my-dpr/my_output/checkpoints/finetune/{cp_path}"):
    #     continue
    script = construct_script(gpu_ids=gpu_ids, ckpt_path=best_ckpt_path, query_fold_num=query_fold, use=use)
    gpu_ids = gpu_ids[1:]
    output = open(f'scripts_for_pipeline_corrected/{script_name}', 'w+', encoding='utf-8')
    output.write(script)
    output.close()

    x = threading.Thread(target=run, args=(script_name,))
    all_threads.append(x)
    x.start()
    os.system('sleep 10')

for i in range(len(all_threads)):
    all_threads[i].join()
