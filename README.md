# ACORDAR 2.0



## Paper and Citation

Qiaosheng Chen, Tengteng Lin, Weiqing Luo, Xiaxia Wang, Zixian Huang, Ahmet Soylu, Basil Ell, Baifan Zhou, Evgeny Kharlamov, and Gong Cheng. ACORDAR 2.0: A Test Collection for Ad Hoc Content-Based (Dense) RDF Dataset Retrieval. Submitted to ISWC 2022.

## RDF Datasets

We reuse the RDF datasets 31,589 RDF datasets collected from 540 data portals from [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR). The "./Data/datasets.json" file provides the ID and metadata of each dataset in JSON format. The dataset itself can be downloaded via the links in the download field. We recommend using Apache Jena to parse the downloaded datasets.

## Queries

We reuse the 493 queries from [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR) with 3 of them being discarded. The "./Data/queries.txt" file presents all the 490 queries, one query per line, where each line has two tab-separated fields: **query_id** and **query_text**. The queries can be divided into synthetic queries created by our human annotators (see "./Data/synthetic_queries.txt") and TREC queries imported from the ad hoc topics (titles) used in the English Test Collections of TREC 1-8 (see "./Data/trec_queries.txt").

## Qrels

The "./Data/qrels.txt" file contains 18,727 qrels in TREC's qrels format, one qrel per line, where each line has four tab-separated fields: **query_id**, **iteration** (always zero and not used), **dataset_id**, and **relevancy** (0: irrelevant; 1: partially relevant; 2: highly relevant).

## Splits for Cross-Validation

To allow comparable evaluation results, one should use the train-valid-test splits we provide for five-fold cross-validation. The "./Data/Splits for Cross Validation" folder has five sub-folders, one for each fold. In each sub-folder we provide three qrel files as the training, validation, and test sets.

# Baselines

We evaluated four sparse retrieval models: TF-IDF based cosine similarity, BM25F, Language Model using Dirichlet priors for smoothing (LMD), Fielded Sequential Dependence Model (FSDM) and two dense retrieval models: [Dense Passage Retrieval (DPR)](https://github.com/facebookresearch/DPR), and [Contextualized late interaction over BERT (ColBERT)](https://github.com/stanford-futuredata/ColBERT). We ran sparse models over an inverted index of four metadata fields (title, description, author, tags) and four data fields (literals, classes, properties, entities) and ran dense models with metadata document and data document representing the dataset as input. In each fold, for each sparse model, we merged the training and validation sets and performed grid search to tune its field weights from 0 to 1 in 0.1 increments using NDCG@10 as our optimization objective. Dense models were fine-tuned in a standard way on the training and validation sets.

The "./Baselines" folder contains rankings outputted by the baselines in TREC's results format. Below we show the evaluation results averaged over the test sets in all the five folds. One can use trec_eval for evaluation.

|         | NDCG@5 | NDCG@10 |  MAP@5 | MAP@10 |
| ------- | -----: | ------: | -----: | -----: |
| TF-IDF  | 0.4718 |  0.4752 | 0.1958 | 0.2722 |
| BM25F   | 0.5233 |  0.5184 | 0.2180 | 0.2988 |
| LMD     | 0.4877 |  0.4937 | 0.2150 | 0.2924 |
| FSDM    | 0.5556 |  0.5468 | 0.2476 | 0.3276 |
| DPR     | 0.3949 |  0.3756 | 0.1536 | 0.1958 |
| ColBERT | 0.2916 |  0.2784 | 0.1210 | 0.1470 |

# Contact

Qiaosheng Chen (qschen@smail.nju.edu.cn) and Gong Cheng (gcheng@nju.edu.cn)