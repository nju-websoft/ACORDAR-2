# ACORDAR 2.0

ACORDAR 2.0 is a test collection for ad hoc content-based dataset retrieval, which is the task of answering a keyword query with a ranked list of datasets. Keywords may refer to the metadata and/or the data of each dataset. Compared with [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR), we implement two dense retrieval models for pooling and evaluation. For details about this test collection, please refer to the following paper.

## Paper and Citation

Qiaosheng Chen, Tengteng Lin, Weiqing Luo, Xiaxia Wang, Zixian Huang, Ahmet Soylu, Basil Ell, Baifan Zhou, Evgeny Kharlamov, and Gong Cheng. ACORDAR 2.0: A Test Collection for Ad Hoc Content-Based (Dense) RDF Dataset Retrieval. Submitted to ISWC 2022.

## RDF Datasets

We reuse the 31,589 RDF datasets collected from 540 data portals from [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR). The "[./Data/datasets.json](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/datasets.json)" file provides the ID and metadata of each dataset in JSON format. Each dataset can be downloaded via the links in the "download" field. We recommend using Apache Jena to parse the datasets.

## Queries

We reuse the 493 queries from [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR) and remove 3 queries after pooling. The "[./Data/all_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/all_queries.txt)" file provides all the remaining 490 queries. Each row represents a query with two tab-separated columns: **query_id** and **query_text**. The queries can be divided into synthetic queries created by our human annotators ("[./Data/synthetic_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/synthetic_queries.txt)") and TREC queries imported from the ad hoc topics (titles) used in the English Test Collections of TREC 1-8 ("[./Data/trec_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/trec_queries.txt)").

## Qrels

The "[./Data/qrels.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/qrels.txt)" file contains 18,727 qrels in TREC's qrels format, one qrel per row, where each row has four tab-separated columns: **query_id**, **iteration** (always zero and never used), **dataset_id**, and **relevancy** (0: irrelevant; 1: partially relevant; 2: highly relevant).

## Splits for Cross-Validation

To make evaluation results being comparable, one should use the train-valid-test splits that we provide for five-fold cross-validation. The "[./Data/Splits for Cross Validation](https://github.com/nju-websoft/ACORDAR-2/tree/main/Data/Splits%20for%20Cross%20Validation)" folder has five sub-folders. In each sub-folder we provide three qrel files as training, validation, and test sets, respectively.

## Baselines

We have evaluated four sparse retrieval models: (1) TF-IDF based cosine similarity, (2) BM25F, (3) Language Model using Dirichlet priors for smoothing (LMD), (4) Fielded Sequential Dependence Model (FSDM) and two dense retrieval models: (5) [Dense Passage Retrieval (DPR)](https://github.com/facebookresearch/DPR), (6) [Contextualized late interaction over BERT (ColBERT)](https://github.com/stanford-futuredata/ColBERT). We ran sparse models over an inverted index of four metadata fields (title, description, author, tags) and four data fields (literals, classes, properties, entities), and ran dense models with metadata documents and data documents. In each fold, for each sparse model, we merged the training and validation sets and performed grid search to tune its field weights from 0 to 1 in 0.1 increments using NDCG@10 as our optimization target. Dense models were fine-tuned in a standard way on the training and validation sets.

The "[./Baselines](https://github.com/nju-websoft/ACORDAR-2/tree/main/Baselines)" folder provides ranking output of the baseline methods in TREC's results format. Below we show mean evaluation results over the test sets in all five folds. One can use trec_eval for evaluation.

|         | NDCG@5 | NDCG@10 |  MAP@5 | MAP@10 |
| ------- | -----: | ------: | -----: | -----: |
| TF-IDF  | 0.4718 |  0.4752 | 0.1958 | 0.2722 |
| BM25F   | 0.5233 |  0.5184 | 0.2180 | 0.2988 |
| LMD     | 0.4877 |  0.4937 | 0.2150 | 0.2924 |
| FSDM    | 0.5556 |  0.5468 | 0.2476 | 0.3276 |
| DPR     | 0.3949 |  0.3755 | 0.1536 | 0.1958 |
| ColBERT | 0.2916 |  0.2784 | 0.1210 | 0.1470 |

## Source Codes

All source codes of implementation are provided in [./Code](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code).

### Dependencies

- JDK 8+
- Apache Lucene 8.7.0
- Python 3.6
- torch 1.10


### Sparse Models

- **Inverted Index:** Each RDF dataset was stored as a pseudo document in an inverted index which consists of eight fields. 

    - Four *metadata fields*: **title**, **description**, **tags**, and **author**.
    - Four *data fields*: **classes**, **properties**, **entities**, and **literals**.

    See codes in [./Code/sparse/indexing](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/indexing) for details.

- **Sparse Retrieval Models:** We implemented *TF-IDF*, *BM25F*, *LMD* and *FSDM* based on Apache Lucene 8.7.0. See codes in [./Code/sparse/models](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/models) for details.

- **Field Weights Tuning:** For each sparse model we performed grid search to tune its field weights from 0 to 1 in 0.1 increments using NDCG@10 as our optimization objective. See codes in [./Code/sparse/fieldWeightsTuing](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/fieldWeightsTuing) for details.

- **Retrieval Experiments:** We employed Acordar 2.0 to evaluate all four sparse models. See codes in [./Code/sparse/experiment](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/experiment) for details.

### Dense Models
- **Triples Extraction:** We used IlluSnip to represent the content of RDF datasets. See codes in [./Code/dense/preprocess/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/preprocess/README.md) for details.

- **Pseudo Documents:** To apply dense models to RDF datasets, for each dataset we created two pseudo documents: *metadata document* concatenating human-readable information in metadata and *data document* concatenating the human-readable forms of the subject, predicate, and object in each RDF triple. See [./Code/dense/preprocess/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/preprocess/README.md) for details.

- **Training and Retrieval:** Both dense models (*DPR* and *ColBERT*) were implemented on the basis of their original source code. See [./Code/dense/DPR/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/DPR/README.md) and [./Code/dense/ColBERT/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/ColBERT/README.md) for details.

## License
This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/nju-websoft/ACORDAR-2/blob/main/LICENSE) file for details.

## Contact

Qiaosheng Chen (qschen@smail.nju.edu.cn) and Gong Cheng (gcheng@nju.edu.cn)
