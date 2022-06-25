map_path = "/home/ttling/ColBERT/docs/collections_meta_2.tsv"
rank_path = "/home/ttling/ColBERT/experiments/distilColBERT/retrieve.py/2022-03-27_20.33.10/ranking.tsv"
output_path = "/home/ttling/ColBERT/experiments/distilColBERT/retrieve.py/2022-03-27_20.33.10/ranking-real.tsv"
map = []

def get_map():
    map.append("Os dados da seção contêm informações sobre os cursos de pós-graduação stricto sensu no Brasil.    Esta versão apresenta os metadados para cursos de pós-graduação stricto sensu dos anos de 2017 a 2019, compreendendo os dados parciais do período de Avaliação e será atualizada até completar-se os quatro anos (2017-2020) do ciclo, finalizando em 2021, ano da próxima Avaliação Quadrienal.    Nova versão será apresentada com atualização em decorrência de reabertura de calendário de envio de dados do Coleta pelos Programas de pós-graduação, referente aos anos de 2017 a 2019.	[2017 a 2020] Cursos da Pós-Graduação Stricto Sensu no Brasil")
    with open(map_path,"r") as f:

        for line in f.readlines():
            line = line.strip().split("\t")
            if line[0]=="1":
                continue
            map.append(line[0])

def get_results():
    with open(rank_path,"r") as f:

        list = []

        for line in f.readlines():
            line = line.strip().split("\t")

            if(int(line[2])>50):
                continue

            tmp = map[int(line[1])]

            list.append((line[0], tmp, line[2], line[3]))




    with open(output_path,'w+') as fp:
        for l in list:
            fp.write(l[0]+"\t"+l[1]+'\t'+l[2]+'\t'+l[3]+'\n')

get_map()
get_results()