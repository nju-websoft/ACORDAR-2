# ColBERT

We reuse the implementation of [Omar et al., 2020](https://github.com/stanford-futuredata/ColBERT).

First follow `Code/src/preprocess/README.md` in our repo to generate pseudo documents for indexing.

```
git clone https://github.com/stanford-futuredata/ColBERT.git
```

## Training
We just modified ```train.py``` and ```training.py``` to add the validation set and changed commands as follows (taking fold0 as an example):

```

CUDA_VISIBLE_DEVICES=0 python -m colbert.train \
--amp --checkpoint ../ColBERT/downloads/colbertv2.0.dnn \
--doc_maxlen 512 --query_maxlen 64 \
--mask-punctuation --bsize 8 \
--triples ../ColBERT/docs/all/pairs_all_123.jsonl \
--root ../ColBERT/experiments/all/0/ \
--collection ../ColBERT/docs/corpus_all.tsv \
--queries ../ColBERT/docs/queries_123.tsv \
--valid_triples ../ColBERT/docs/all/pairs_all_4.jsonl \
--valid_queries ../ColBERT/docs/queries_4.tsv \
--experiment lr_1e-06_batch_size_8 \
--similarity cosine \
--run 4.25 --epoch 1 --lr 1e-06 
```

## Indexing and Retrieving

Follow the instructions of the repo of ColBERT to index and retrieve.

