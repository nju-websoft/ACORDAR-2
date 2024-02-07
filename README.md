# ACORDAR 2.0

ACORDAR 2.0 is a test collection for ad hoc content-based dataset retrieval, which is the task of answering a keyword query with a ranked list of datasets. Keywords may refer to the metadata and/or the data of each dataset. Compared with [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR), we implement two dense retrieval models for pooling and evaluation. For details about this test collection, please refer to the following paper.

## RDF Datasets

We reused the 31,589 RDF datasets collected from 540 data portals from [ACORDAR 1.0](https://github.com/nju-websoft/ACORDAR). The "[./Data/datasets.json](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/datasets.json)" file provides the ID and metadata of each dataset in JSON format. Each dataset can be downloaded via the links in the "download" field. We recommend using Apache Jena to parse the datasets.

We also provide deduplicated RDF data files in N-Triples format for each dataset available at [Zenodo](https://doi.org/10.5281/zenodo.6683710).

```
{
   "datasets":
   [
      {
         "license":"...",
         "download":"..."
         "size":"...",
         "author":"...",
         "created":"...",
         "dataset_id":"...",
         "description":"...",
         "title":"...",
         "version":"...",
         "updated":"...",
         "tags":"..."
      },
      ...
   ]
}
```

## Keyword Queries

The "[./Data/all_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/all_queries.txt)" file provides 510 keyword queries. Each row represents a query with two tab-separated columns: **query_id** and **query_text**. The queries can be divided into synthetic queries created by our human annotators ("[./Data/synthetic_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/synthetic_queries.txt)") and TREC queries imported from the ad hoc topics (titles) used in the English Test Collections of TREC 1-8 ("[./Data/trec_queries.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/trec_queries.txt)").

## Question Queries

The "[./Data/question_queries.json](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/question_queries.json)" file provides 1,377 question queries with corresponding keyword queries.
```
[
    {
        "query_id": "...",
        "query_text": "...",
        "split": "...",
        "questions": [
            "...",
            "..."
        ]
    },
    ...
]
```

## Qrels

The "[./Data/qrels.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Data/qrels.txt)" file contains 19,340 qrels in TREC's qrels format, one qrel per row, where each row has four tab-separated columns: **query_id**, **iteration** (always zero and never used), **dataset_id**, and **relevancy** (0: irrelevant; 1: partially relevant; 2: highly relevant).

## Splits for Cross-Validation

To make evaluation results being comparable, one should use the train-valid-test splits that we provide for five-fold cross-validation. The "[./Data/Splits for Cross Validation](https://github.com/nju-websoft/ACORDAR-2/tree/main/Data/Splits%20for%20Cross%20Validation)" folder has five sub-folders. In each sub-folder we provide three qrel files as training, validation, and test sets, respectively.

## Baselines

We have evaluated four sparse retrieval models: (1) TF-IDF based cosine similarity, (2) BM25, (3) Language Model using Dirichlet priors for smoothing (LMD), (4) Fielded Sequential Dependence Model (FSDM) and two dense retrieval models: (5) [Dense Passage Retrieval (DPR)](https://github.com/facebookresearch/DPR), (6) [Contextualized late interaction over BERT (ColBERT)](https://github.com/stanford-futuredata/ColBERT). We ran sparse models over an inverted index of four metadata fields (title, description, author, tags) and four data fields (literals, classes, properties, entities), and ran dense models over pseudo metadata documents and (sampled) data documents. In each fold, for each sparse model, we merged the training and validation sets and performed grid search to tune its field weights from 0 to 1 in 0.1 increments using NDCG@10 as our optimization target. Dense models were fine-tuned in a standard way on the training and validation sets.

The "[./Baselines](https://github.com/nju-websoft/ACORDAR-2/tree/main/Baselines)" folder provides the output of each baseline method in TREC's results format. Below we show the mean evaluation results over the test sets in all five folds. One can use trec_eval for evaluation.

|         | NDCG@5 | NDCG@10 |  MAP@5 | MAP@10 |
| ------- | -----: | ------: | -----: | -----: |
| TF-IDF  | 0.4572 |  0.4605 | 0.1920 | 0.2654 |
| BM25    | 0.5067 |  0.5020 | 0.2134 | 0.2910 |
| LMD     | 0.4725 |  0.4783 | 0.2105 | 0.2848 |
| FSDM    | 0.5222 |  0.5078 | 0.2395 | 0.3080 |
| DPR     | 0.3597 |  0.3469 | 0.1452 | 0.1809 |
| ColBERT | 0.2788 |  0.2676 | 0.1133 | 0.1387 |

## Source Codes

All source codes of our implementation are provided in [./Code](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code).

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

- **Sparse Retrieval Models:** We implemented *TF-IDF*, *BM25*, *LMD* and *FSDM* based on Apache Lucene 8.7.0. See the codes in [./Code/sparse/models](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/models) for details.

- **Field Weights Tuning:** For each sparse model we performed grid search to tune its field weights from 0 to 1 in 0.1 increments using NDCG@10 as our optimization objective. See the codes in [./Code/sparse/fieldWeightsTuing](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/fieldWeightsTuning) for details. Field weights for pooling are stored at [./Code/sparse/pooling-field-weights.txt](https://github.com/nju-websoft/ACORDAR-2/blob/main/Code/sparse/pooling-field-weights.txt).

- **Retrieval Experiments:** We employed ACORDAR 2.0 to evaluate all four sparse models. See the codes in [./Code/sparse/experiment](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/sparse/experiment) for details.

### Dense Models
- **Triples Extraction:** We used IlluSnip to sample the content of RDF datasets. See [./Code/dense/preprocess/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/preprocess/README.md) for details.

- **Pseudo Documents:** To apply dense models to RDF datasets, for each dataset we created two pseudo documents: *metadata document* concatenating human-readable information in metadata and *data document* concatenating the human-readable forms of the subject, predicate, and object in each sampled RDF triple. See [./Code/dense/preprocess/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/preprocess/README.md) for details.

- **Training and Retrieval:** Both dense models (*DPR* and *ColBERT*) were implemented on the basis of their original source code. See [./Code/dense/DPR/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/DPR/README.md) and [./Code/dense/ColBERT/README.md](https://github.com/nju-websoft/ACORDAR-2/tree/main/Code/dense/ColBERT/README.md) for details.

## License
This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/nju-websoft/ACORDAR-2/blob/main/LICENSE) file for details.

## Contact

Qiaosheng Chen (qschen@smail.nju.edu.cn) and Gong Cheng (gcheng@nju.edu.cn)
