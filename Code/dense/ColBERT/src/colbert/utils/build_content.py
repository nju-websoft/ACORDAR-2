from colbert.utils.database import dashboard,analysis
from colbert.utils.utils import print_message



def Map_id_2_label(dataset):
    # print_message("#>>> Map id2label start.")
    res = {}

    dashboard.execute("SELECT id,label,iri,kind FROM rdf_term WHERE dataset_id=" + dataset)
    data = dashboard.fetchall()
    for id, label, iri, kind in data:
        # print_message(str(dataset)+"\t"+str(id))
        if label is None:
            res[id] = iri.replace("\t", " ").replace("\r", " ").replace("\n", " ")
        else:
            res[id] = label.replace("\t", " ").replace("\r", " ").replace("\n", " ")
    # print_message("#>>> {} Map id2label finished.".format(dataset))
    return res


def get_illusnip_text_for_dataset(dataset):
    illusnip = ""
    res = Map_id_2_label(dataset)


    dashboard.execute("SELECT snippet FROM IlluSnip_2 WHERE dataset_id=" + dataset)
    data = dashboard.fetchall()
    for snippet in data:
        data = snippet[0].strip().split(", ")
        break
    for d in data:
        one = d.split(" ")
        if len(one)<3: continue
        # print(one)
        illusnip += (res[int(one[0])]+' '+ res[int(one[1])]+' '+ res[int(one[2])]+". ")
    print_message("#>>> {} IlluSnip finished.".format(dataset))
    return illusnip


def write_snip_data(datasets_path, out_path):
    datasets = []
    with open(datasets_path, "r") as f:
        for line in f.readlines():
            datasets.append(line.strip())

    with open(out_path,'w') as f:
        for d in datasets:
            triples = get_illusnip_text_for_dataset(d)
            f.write(d+"\t"+triples+"\n")

write_snip_data("/home/ttling/ColBERT/docs/datasets.txt","/home/ttling/ColBERT/docs/collections_illusnip.tsv")