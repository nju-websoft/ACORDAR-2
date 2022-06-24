import argparse
from database import analysis, dashboard
from transformers.tokenization_bert import BertTokenizer

tokenizer = BertTokenizer.from_pretrained(
    "bert-base-uncased",
    do_lower_case=True
)

def rm_tab_and_CRLF(s: str):
    return s.replace("\r"," ")\
            .replace("\n"," ")\
            .replace("\t"," ") if s is not None else None

def get_dataset_text(dataset_id):
    analysis.execute(f"SELECT title, description FROM dataset_summary WHERE dataset_id={dataset_id}")
    title, description = analysis.fetchone()
    title, description = rm_tab_and_CRLF(title), rm_tab_and_CRLF(description)
    dashboard.execute(f"SELECT snippet FROM IlluSnip_2 WHERE  dataset_id={dataset_id}")
    snippet ,= dashboard.fetchone()
    if snippet == "":
        return title, description, ""
    triples = snippet.split(',')
    analysis.execute(f"SELECT file_id FROM pid WHERE dataset_id={dataset_id}")
    file_ids = analysis.fetchall()
    rdf_term_sql_ = f"SELECT iri,label FROM rdf_term \
        WHERE file_id={file_ids[0][0]} and " \
        if len(file_ids)==1 else\
        f"SELECT iri,label FROM rdf_term_deduplicate \
            WHERE dataset_id={dataset_id} and "
    snippet_text = "[CLS]"
    for triple in triples:
        sub, pre, obj = triple.strip().split(' ')
        triple_text = str()
        for rdf_term_id in (sub, pre, obj):
            rdf_term_id = int(rdf_term_id)
            rdf_term_sql = rdf_term_sql_ + f"id={rdf_term_id}"
            analysis.execute(rdf_term_sql)
            _ , label = analysis.fetchone()
            if label is None:
                label = ""
            label = rm_tab_and_CRLF(label)
            triple_text = triple_text + label + " "
        tokens = tokenizer.tokenize(triple_text.strip())
        truncate_res = tokenizer.encode(triple_text.strip(), max_length=45, \
            truncation=True, add_special_tokens=False) # limit to 45 tokens
        truncate_res = tokenizer.decode(truncate_res)
        snippet_text = f"{snippet_text} {truncate_res} [SEP]"

    return title, description, snippet_text


def create_pseudo_content_document(out_path):
    analysis.execute("select DISTINCT(dataset_id) from pid ORDER BY dataset_id asc")
    data = analysis.fetchall()
    with open(out_path, "w+", encoding="utf-8") as fp:
        for item in data:
            dataset_id ,= item
            title, _, snippet_text = get_dataset_text(dataset_id)
            if title is None:
                continue
            fp.write(f"{dataset_id}\t{snippet_text}\n")


def create_pseudo_metadata_document(out_path):
    analysis.execute("select DISTINCT(dataset_id) from pid ORDER BY dataset_id asc")
    data = analysis.fetchall()
    with open(out_path, "w+", encoding="utf-8") as fp:
        for item in data:
            dataset_id ,= item
            analysis.execute(f"SELECT title, description, tags, author FROM dataset_summary WHERE dataset_id={dataset_id}")
            title, description, tag, author = [rm_tab_and_CRLF(item) for item in analysis.fetchone() ]
            metadata_valid = [item for item in [title, description, tag, author] if item is not None]
            metadata_str = f"[CLS] {' [SEP] '.join(metadata_valid)} [SEP]"
            fp.write(f"{dataset_id}\t{metadata_str}\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='create metadata and data content')
    parser.add_argument('--output_path', type=str, help='output path of query')
    args = parser.parse_args()
    create_pseudo_metadata_document(args.output_path)
    create_pseudo_content_document(args.output_path)
