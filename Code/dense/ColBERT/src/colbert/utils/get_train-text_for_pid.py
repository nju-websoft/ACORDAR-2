

map = []
map.append("Os dados da seção contêm informações sobre os cursos de pós-graduação stricto sensu no Brasil.    Esta versão apresenta os metadados para cursos de pós-graduação stricto sensu dos anos de 2017 a 2019, compreendendo os dados parciais do período de Avaliação e será atualizada até completar-se os quatro anos (2017-2020) do ciclo, finalizando em 2021, ano da próxima Avaliação Quadrienal.    Nova versão será apresentada com atualização em decorrência de reabertura de calendário de envio de dados do Coleta pelos Programas de pós-graduação, referente aos anos de 2017 a 2019.	[2017 a 2020] Cursos da Pós-Graduação Stricto Sensu no Brasil")
with open("../../docs/collections_meta_2.tsv","r") as f:
    for line in f.readlines():
        line = line.strip().split("\t")
        if line[0] == "1":
            continue
        map.append(line)


print(map[1611])