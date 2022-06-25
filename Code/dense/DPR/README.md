# DPR

We use the implementation of [Karpukhin et al., 2020](https://github.com/facebookresearch/DPR). 

First follow `Code/src/preprocess/README.md` to generate pseudo documents for retrieving.

```
git clone https://github.com/facebookresearch/DPR.git
```

Then specify the paths of our pseudo documents in their config files under `conf/ctx_sources` and `conf/datasets` directories.

Then follow the instructions of their repo to retrieve.
