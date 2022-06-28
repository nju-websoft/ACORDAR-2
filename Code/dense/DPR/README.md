# DPR

We reuse the implementation of [Karpukhin et al., 2020](https://github.com/facebookresearch/DPR). 

First follow `Code/src/preprocess/README.md` in our repo to generate pseudo documents for indexing.

```
git clone https://github.com/facebookresearch/DPR.git
```

Then specify the paths of our pseudo documents in their config files in `conf/ctx_sources` and `conf/datasets`.

Then follow the instructions of their repo to index and retrieve (we combine those instructions in [./src/scripts/pipeline.sh](https://github.com/nju-websoft/ACORDAR-2/blob/main/Code/dense/DPR/src/scripts/pipeline.sh), to run the script you need to replace the corresponding file paths, e.g. replace `/home/wqluo/code/my-dpr/dpr/downloads/checkpoint/retriever/single-adv-hn/nq/bert-base-encoder.cp` in [pipeline.sh](https://github.com/nju-websoft/ACORDAR-2/blob/b346a22dd782a91774b4897de297ba92e13b9488/Code/dense/DPR/src/scripts/pipeline.sh#L15) with the path of your BERT encoder checkpoint).
