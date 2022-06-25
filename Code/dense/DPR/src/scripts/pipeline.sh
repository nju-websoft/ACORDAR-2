#!/bin/bash 
model_type="finetune"

data_type="both"
data_type1="metadata"
data_type2="content"

embedding_name="embedding_512_bench_ft_fold0"

ctx_source1="dataset_ctx_${data_type1}"
ctx_source2="dataset_ctx_${data_type2}"

QUERY_SOURCE=query_fold4 # query_all # "query_split5"  # "query_pre" # query

pretrain_file_path="/home/wqluo/code/my-dpr/dpr/downloads/checkpoint/retriever/single-adv-hn/nq/bert-base-encoder.cp"

embedding_path1="/home/wqluo/code/my-dpr/my_output/embeddings/${data_type1}/${model_type}/${embedding_name}"
embedding_path2="/home/wqluo/code/my-dpr/my_output/embeddings/${data_type2}/${model_type}/${embedding_name}"

res_id="bench_512_ft_fold0"
res_name="res_${res_id}.json"
parse_name="res_${res_id}_repooling_new_ndcg_parsed.json"

retrieve_res_path="/home/wqluo/code/my-dpr/my_output/retrieve_results/${data_type}/${model_type}/${res_name}"
res_parse_path="/home/wqluo/code/Scripts-for-DPR/file/res_parsed/${data_type}/${model_type}/${parse_name}"

batch_size=8
lr=2e-5
train_name="finetune_bs${batch_size}"
train_path="/home/wqluo/code/my-dpr/my_output/checkpoints/${train_name}"

if [ ! -f "$train_path/best_cp_name" ]; then
	echo "best_cp_name not found!"
	model_file_path=$pretrain_file_path
else
	best_cp_name=$(awk -v FS='\t' '{print $1}' "$train_path/best_cp_name")
	model_file_path=$best_cp_name
fi

embedding(){
	CUDA_VISIBLE_DEVICES=4 python generate_dense_embeddings.py \
	model_file=$model_file_path \
	ctx_src=$1 \
	out_file=$2 \
	insert_title=False
}

retrieve(){
	CUDA_VISIBLE_DEVICES=4 python dense_retriever.py \
	model_file=$model_file_path \
	qa_dataset=${QUERY_SOURCE} \
	ctx_datatsets=[$ctx_source1,$ctx_source2] \
	encoded_ctx_files=[\"${embedding_path1}\",\"${embedding_path2}\"] \
	out_file=$retrieve_res_path
}

parse(){
	python /home/wqluo/code/Scripts-for-DPR/src/parse_retrieve_res.py $retrieve_res_path $res_parse_path
}

train(){
	# CUDA_VISIBLE_DEVICES=0,1,2,3
	CUDA_VISIBLE_DEVICES=0 \
	python train_dense_encoder.py \
	train_datasets=[ds_0,ds_1,ds_2] \
	dev_datasets=[ds_3] \
	train=biencoder_local \
	output_dir=$train_path \
	model_file=$pretrain_file_path \
	my_batch_size=$batch_size \
	my_learning_rate=$lr \
	ignore_checkpoint_optimizer=true

	best_cp_name=$(awk -v FS='\t' '{print $1}' "$train_path/best_cp_name")
	model_file_path=$best_cp_name

	if [ ! -f $best_cp_name ]; then
		echo "best_cp_name not found!"
	else
		find $train_path ! \( -wholename $best_cp_name -o -name "best_cp_name" \)  -type f -exec rm -f {} +
	fi

}

if [ $1 == "embedding" ]; then
    embedding $ctx_source1 $embedding_path1
	embedding $ctx_source2 $embedding_path2
	# echo "hello"

elif [ $1 == "retrieve" ]; then
	echo "===================== retrieve start ====================="
	retrieve
	echo "===================== parse start ====================="
	parse

elif [ $1 == "train" ]; then
    echo "===================== train start ====================="
	train
	
elif [ $1 == "parse" ]; then
	parse

elif [ $1 == "pipeline" ]; then
	echo "===================== train start ====================="
	train
    echo "===================== embedding start ====================="
	embedding $ctx_source1 $embedding_path1
	embedding $ctx_source2 $embedding_path2
	echo "===================== retrieve start ====================="
	retrieve
	echo "===================== parse start ====================="
	parse


else
    echo "$1: not supported"

fi
