package example;

import PCSG.PATHS;
import util.DBUtil;
import PLL.WeightedPLL;
import graph.WeightedGraph;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import util.ReadFile;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DatasetIndexer {
    private static Connection connection = new DBUtil().conn;

    public static int getTypeID(int local_id){
        int typeID = -1;
        try{
//            String str = String.format("SELECT * FROM rdf_term WHERE file_id=%d AND iri='%s';",local_id,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            String str = String.format("SELECT * FROM rdf_term_deduplicate WHERE dataset_id=%d AND iri='%s';",local_id,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            Statement stmt = connection.createStatement();
            ResultSet rst = stmt.executeQuery(str);

            while(rst.next()){
                typeID = rst.getInt("id");
            }

        }catch (SQLException e){
            e.printStackTrace();
        }

        return typeID;
    }

    public static HashSet<Integer> getLiteralSet(int local_id){
        HashSet<Integer> literals = new HashSet<Integer>();

//        String str = String.format("SELECT id FROM rdf_term WHERE file_id=%d AND kind=2",local_id);
        String str = String.format("SELECT id FROM rdf_term_deduplicate WHERE dataset_id=%d AND kind=2",local_id);
        try{
            Statement statement = connection.createStatement();
            ResultSet rst = statement.executeQuery(str);

            while(rst.next()){
                literals.add(rst.getInt(1));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        return literals;
    }

    private static void getPatternIndexes(int local_id) {
        int typeID = getTypeID(local_id);

        Map<Integer, Set<Integer>> entity2EDP = new HashMap<>(); //entity -> EDP
        Set<Integer> classSet = new HashSet<>(); //set of classes
        Set<Integer> literalSet = getLiteralSet(local_id);

        List<List<Integer>> tripleList = new ArrayList<>();
//        List<List<Integer>> tripleList = ReadFile.readInteger(folder + "dataset.txt", "\t");

        try{
            String str = String.format("SELECT * FROM triple_all WHERE resource_id=%d",local_id);
            Statement stmt = connection.createStatement();
            stmt.setFetchSize(1000);
            ResultSet rst = stmt.executeQuery(str);
            while(rst.next()){
                List<Integer> one = new ArrayList<>();
                int subject = rst.getInt("subject");
                int predicate = rst.getInt("predicate");
                int object = rst.getInt("object");
                one.add(subject);
                one.add(predicate);
                one.add(object);
                tripleList.add(one);

                Set<Integer> subjectEDP = entity2EDP.getOrDefault(subject, new HashSet<>());
                if (typeID != 0 && predicate == typeID){ //S-TYPE-CLASS
                    subjectEDP.add(object);
                    classSet.add(object);
                }
                else {
                    subjectEDP.add(predicate);
                }
                entity2EDP.put(subject, subjectEDP);
                if (predicate != typeID && !literalSet.contains(object)){ //object is an entity
                    Set<Integer> objectEDP = entity2EDP.getOrDefault(object, new HashSet<>());
                    objectEDP.add(-predicate);
                    entity2EDP.put(object, objectEDP);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        Map<Set<Integer>, Integer> EDP2count = new HashMap<>();
        for (int iter: entity2EDP.keySet()){
            Set<Integer> edp = entity2EDP.get(iter);
            int count = EDP2count.getOrDefault(edp, 0);
            EDP2count.put(edp, count + 1);
        }
        List<Map.Entry<Set<Integer>, Integer>> rankedEDPList = new ArrayList<>(EDP2count.entrySet());
        Collections.sort(rankedEDPList, new Comparator<Map.Entry<Set<Integer>, Integer>>() {
            @Override
            public int compare(Map.Entry<Set<Integer>, Integer> o1, Map.Entry<Set<Integer>, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        Map<Set<Integer>, Integer> EDP2ID = new HashMap<>();
        int i = 0;
        for (Map.Entry<Set<Integer>, Integer> iter: rankedEDPList) {
            i++;
            EDP2ID.put(iter.getKey(), i);
        }
        try{
            String edpIndexFolder = PATHS.ProjectData +local_id+ "/EDP/";
            File file = new File(edpIndexFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            Directory directory = FSDirectory.open(Paths.get(edpIndexFolder));
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
            Map<Integer, Integer> edp2size = new HashMap<>();
            for (Set<Integer> pattern: EDP2ID.keySet()) {
                Document document = new Document();
                int edpId = EDP2ID.get(pattern);
                document.add(new IntPoint("id", edpId));
                document.add(new StoredField("id", edpId));
                int count = EDP2count.get(pattern);
                document.add(new IntPoint("count", count));
                document.add(new StoredField("count", count));
                int edpSize = pattern.size();
                document.add(new IntPoint("size", edpSize));
                document.add(new StoredField("size", edpSize));
                edp2size.put(edpId, edpSize);
                StringBuilder inProp = new StringBuilder();
                StringBuilder outProp = new StringBuilder();
                StringBuilder classes = new StringBuilder();
                for (int id: pattern) {
                    if (classSet.contains(id)) {
                        classes.append(id).append(" ");
                    }
                    else if (id > 0) {
                        outProp.append(id).append(" ");
                    }
                    else {
                        inProp.append(-id).append(" ");
                    }
                }
                document.add(new TextField("classes", classes.toString().trim(), Field.Store.YES));
                document.add(new TextField("inProperty", inProp.toString().trim(), Field.Store.YES));
                document.add(new TextField("outProperty", outProp.toString().trim(), Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();// Finish EDP index.

            PrintWriter entityWriter = new PrintWriter(PATHS.ProjectData +local_id +"/entity-EDP.txt");
            for (int entity: entity2EDP.keySet()) {
                int edpId = EDP2ID.get(entity2EDP.get(entity));
                entityWriter.println(entity + "\t" + edpId + "\t" + EDP2count.get(entity2EDP.get(entity)));
            }
            entityWriter.close(); // Finish entity index.

            Map<List<Integer>, List<Integer>> triple2LP = new HashMap<>();
            Map<List<Integer>, Integer> LP2count = new HashMap<>();
            for (List<Integer> triple: tripleList) {
                int subject = triple.get(0);
                int predicate = triple.get(1);
                int object = triple.get(2);
                if (predicate == typeID || literalSet.contains(object)) {
                    continue;
                }
                List<Integer> LP = new ArrayList<>(Arrays.asList(EDP2ID.get(entity2EDP.get(subject)), predicate, EDP2ID.get(entity2EDP.get(object))));
                triple2LP.put(triple, LP);
                int count = LP2count.getOrDefault(LP, 0);
                LP2count.put(LP, count + 1);
            }
            List<Map.Entry<List<Integer>, Integer>> rankedLPList = new ArrayList<>(LP2count.entrySet());
            Collections.sort(rankedLPList, new Comparator<Map.Entry<List<Integer>, Integer>>() {
                @Override
                public int compare(Map.Entry<List<Integer>, Integer> o1, Map.Entry<List<Integer>, Integer> o2) {
                    return o2.getValue() - o1.getValue();
                }
            });
            Map<List<Integer>, Integer> LP2ID = new HashMap<>();
            i = 0;
            for (Map.Entry<List<Integer>, Integer> iter: rankedLPList) {
                i++;
                LP2ID.put(iter.getKey(), i);
            }
            PrintWriter lpWriter = new PrintWriter(PATHS.ProjectData +local_id+ "/LP.txt");
            for (Map.Entry<List<Integer>, Integer> rankedLP: rankedLPList) {
                List<Integer> iter = rankedLP.getKey();
                int lpId = LP2ID.get(iter);
                int count = LP2count.get(iter);
                String lp = iter.get(0) + " " + iter.get(1) + " " + iter.get(2);
                int lpSize = edp2size.get(iter.get(0)) + edp2size.get(iter.get(2)) - 1;
                lpWriter.println(lpId + "\t" + lp + "\t" + count + "\t" + lpSize);
            }
            lpWriter.close(); // Finish LP index.

            PrintWriter tripleWriter = new PrintWriter(PATHS.ProjectData +local_id+ "/triple-LP.txt");
            for (List<Integer> iter: triple2LP.keySet()) {
                String triple = iter.get(0) + " " + iter.get(1) + " " + iter.get(2);
                List<Integer> value = triple2LP.get(iter);
                String lp = value.get(0) + " " + value.get(1) + " " + value.get(2);
                int count = LP2count.get(value);
                tripleWriter.println(triple + "\t" + lp + "\t" + count);
            }
            tripleWriter.close(); // Finish triple-LP index.
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getComponents(int local_id) {
        Map<Integer, Integer> entity2edp = new HashMap<>();
        for (List<Integer> iter: ReadFile.readInteger(PATHS.ProjectData +local_id+ "/entity-EDP.txt", "\t")) {
            entity2edp.put(iter.get(0), iter.get(1));
        }
        Map<String, Integer> lp2id = new HashMap<>();
        for (List<String> iter: ReadFile.readString(PATHS.ProjectData +local_id+ "/LP.txt", "\t")) {
            lp2id.put(iter.get(1), Integer.parseInt(iter.get(0)));
        }
        Map<String, Integer> triple2LocalId = new HashMap<>();
        Map<Integer, String> localId2lp = new HashMap<>();
        int i = 1;
        for (List<String> iter: ReadFile.readString(PATHS.ProjectData +local_id + "/triple-LP.txt", "\t")) {
            triple2LocalId.put(iter.get(0), -i);
            localId2lp.put(-i, iter.get(1));
            i++;
        }
        Set<Integer> literalSet = getLiteralSet(local_id);
        int typeID = getTypeID(local_id);
        Map<Integer, Set<List<Integer>>> entity2triple = new HashMap<>(); // including subject and object
        Multigraph<Integer, DefaultEdge> ERgraph = new Multigraph<>(DefaultEdge.class);

        try{
            String str = String.format("SELECT * FROM triple_all WHERE resource_id=%d",local_id);
            Statement stmt = connection.createStatement();
            stmt.setFetchSize(1000);
            ResultSet rst = stmt.executeQuery(str);

            while(rst.next()){
                int subject = rst.getInt("subject");
                int predicate = rst.getInt("predicate");
                int object = rst.getInt("object");
                List<Integer> one = new ArrayList<>();
                one.add(subject);
                one.add(predicate);
                one.add(object);
                if (predicate == typeID || literalSet.contains(object)) { // do not need to be added into the graph
                    ERgraph.addVertex(subject);
                    Set<List<Integer>> subjectTripleSet = entity2triple.getOrDefault(subject, new HashSet<>());
                    subjectTripleSet.add(one);
                    entity2triple.put(subject, subjectTripleSet);
                }
                else {
                    int link = triple2LocalId.get(subject + " " + predicate + " " + object);
                    ERgraph.addVertex(subject);
                    ERgraph.addVertex(link);
                    ERgraph.addVertex(object);
                    ERgraph.addEdge(subject, link);
                    ERgraph.addEdge(link, object);
                    Set<List<Integer>> subjectTripleSet = entity2triple.getOrDefault(subject, new HashSet<>());
                    subjectTripleSet.add(one);
                    entity2triple.put(subject, subjectTripleSet);
                    Set<List<Integer>> objectTripleSet = entity2triple.getOrDefault(object, new HashSet<>());
                    objectTripleSet.add(one);
                    entity2triple.put(object, objectTripleSet);
                }
            }


        }catch(Exception e){
            e.printStackTrace();
        }

        ConnectivityInspector<Integer, DefaultEdge> inspector = new ConnectivityInspector<>(ERgraph);
        List<Set<Integer>> components = inspector.connectedSets();
        Map<Set<Integer>, Set<Integer>> component2edp = new HashMap<>();
        Map<Set<Integer>, Set<List<Integer>>> component2triple = new HashMap<>();
        Map<Set<Integer>, Set<Integer>> component2lp = new HashMap<>();
        Map<Set<Integer>, Integer> component2Size = new HashMap<>();
        for (Set<Integer> comp: components) {
            Set<Integer> edpSet = new TreeSet<>(); //instantiated edps in the component
            Set<List<Integer>> tripleSet = new HashSet<>(); // involved triples in the component
            Set<Integer> lpSet = new TreeSet<>(); // involved lps in the component
            for (int node: comp) {
                if (entity2edp.containsKey(node)) {
                    edpSet.add(entity2edp.get(node));
                    tripleSet.addAll(entity2triple.get(node));
                }
                if (localId2lp.containsKey(node)) {
                    lpSet.add(lp2id.get(localId2lp.get(node)));
                }
            }
            component2edp.put(comp, edpSet);
            component2triple.put(comp, tripleSet);
            component2lp.put(comp, lpSet);
            component2Size.put(comp, edpSet.size() + lpSet.size());
        }
        List<Map.Entry<Set<Integer>, Integer>> rankedCompList = new ArrayList<>(component2Size.entrySet());
        Collections.sort(rankedCompList, new Comparator<Map.Entry<Set<Integer>, Integer>>() {
            @Override
            public int compare(Map.Entry<Set<Integer>, Integer> o1, Map.Entry<Set<Integer>, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        int count = 0; // the i-th component
        for (Map.Entry<Set<Integer>, Integer> iter: rankedCompList) {
            Set<Integer> comp = iter.getKey();
            Set<List<Integer>> tripleSet = component2triple.get(comp);
            StringBuilder tripleStr = new StringBuilder();
            for (List<Integer> triple: tripleSet) {
                tripleStr.append(triple.get(0)).append(" ").append(triple.get(1)).append(" ").append(triple.get(2)).append("\n");
            }
            Set<Integer> edpList = component2edp.get(comp);
            StringBuilder edpStr = new StringBuilder();
            for (int edp: edpList) {
                edpStr.append(edp).append("\n");
            }
            Set<Integer> lpList = component2lp.get(comp);
            StringBuilder lpStr = new StringBuilder();
            for (int lp: lpList) {
                lpStr.append(lp).append("\n");
            }
            count++;
            File file0 = new File(PATHS.ProjectData+local_id + "/component/");
            if (file0.exists()) {
                file0.delete();
            }
            String compFolder = PATHS.ProjectData + local_id+"/component/" + count + "/";
            File file = new File(compFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            try {
                PrintWriter graphWriter = new PrintWriter(compFolder + "graph.txt");
                graphWriter.print(tripleStr.toString().trim());
                graphWriter.close();

                PrintWriter edpWriter = new PrintWriter(compFolder + "edp.txt");
                edpWriter.print(edpStr.toString().trim());
                edpWriter.close();

                PrintWriter lpWriter = new PrintWriter(compFolder + "lp.txt");
                lpWriter.print(lpStr.toString().trim());
                lpWriter.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void getSetCoverComponents(int local_id) {
        Set<Integer> alledp = new HashSet<>();
        Set<Integer> alllp = new HashSet<>();
        Map<Integer, Set<Integer>> component2edp = new HashMap<>();
        Map<Integer, Set<Integer>> component2lp = new HashMap<>();
        File[] files = new File(PATHS.ProjectData + local_id+"/component/").listFiles();
        for (File file: files) {
            Set<Integer> edpSet = new HashSet<>(ReadFile.readInteger(file.getPath() + "/edp.txt"));
            Set<Integer> lpSet = new HashSet<>(ReadFile.readInteger(file.getPath() + "/lp.txt"));
            component2edp.put(Integer.parseInt(file.getName()), edpSet);
            component2lp.put(Integer.parseInt(file.getName()), lpSet);
            alledp.addAll(edpSet);
            alllp.addAll(lpSet);
        }
        List<Integer> setCoverResult = new ArrayList<>();
        Set<Integer> coveredEDP = new HashSet<>();
        Set<Integer> coveredLP = new HashSet<>();
        Set<Integer> currentEDP = new HashSet<>();
        Set<Integer> currentLP = new HashSet<>();
        while (coveredEDP.size() < alledp.size() || coveredLP.size() < alllp.size()) {
            int maxSize = 0;
            int maxComp = 0;
            for (Map.Entry<Integer, Set<Integer>> iter: component2edp.entrySet()) {
                int comp = iter.getKey();
                iter.getValue().removeAll(currentEDP);
                component2lp.get(comp).removeAll(currentLP);
                int size = iter.getValue().size() + component2lp.get(comp).size();
                if (maxSize < size) {
                    maxComp = comp;
                    maxSize = size;
                }
            }
            setCoverResult.add(maxComp);
            currentEDP = component2edp.get(maxComp);
            coveredEDP.addAll(currentEDP);
            currentLP = component2lp.get(maxComp);
            coveredLP.addAll(currentLP);
            component2edp.remove(maxComp);
            component2lp.remove(maxComp);
        }
        try {
            PrintWriter writer = new PrintWriter(PATHS.ProjectData+local_id + "/set-cover-components.txt");
            for (int iter: setCoverResult) {
                writer.println(iter);
            }
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getHubLabel(int local_id) {
        Map<Integer, String> lpId2LP = new HashMap<>();
        for (List<String> iter: ReadFile.readString(PATHS.ProjectData+local_id + "/LP.txt", "\t")) {
            lpId2LP.put(Integer.parseInt(iter.get(0)), iter.get(1));
        }
        Map<String, String> entity2edp = new HashMap<>();
        for (List<String> iter: ReadFile.readString(PATHS.ProjectData+local_id + "/entity-EDP.txt", "\t")) {
            entity2edp.put(iter.get(0), iter.get(1));
        }
        Map<String, String> triple2lp = new HashMap<>();
        for (List<String> iter: ReadFile.readString(PATHS.ProjectData+local_id + "/triple-LP.txt", "\t")) {
            triple2lp.put(iter.get(0), iter.get(1));
        }
        List<Integer> setCover = ReadFile.readInteger(PATHS.ProjectData+local_id + "/set-cover-components.txt");
        File file=new File(PATHS.ProjectData+local_id + "/component/");
        if(!file.exists()){//如果文件夹不存在
            file.mkdir();//创建文件夹
        }
        for (int comp: setCover) {
            String compFolder = PATHS.ProjectData+local_id + "/component/" + comp;
            Set<String> nodeSet = new HashSet<>();
            Map<String, Set<String>> invertedMap = new HashMap<>();
            for (String triple: ReadFile.readString(compFolder + "/graph.txt")) {
                String[] spo = triple.split(" ");
                if (triple2lp.containsKey(triple)) {
                    nodeSet.add(spo[0]);
                    String edp0 = entity2edp.get(spo[0]);
                    Set<String> tempSet = invertedMap.getOrDefault(edp0, new HashSet<>());
                    tempSet.add(spo[0]);
                    invertedMap.put(edp0, tempSet);

                    nodeSet.add(spo[2]);
                    String edp2 = entity2edp.get(spo[2]);
                    tempSet = invertedMap.getOrDefault(edp2, new HashSet<>());
                    tempSet.add(spo[2]);
                    invertedMap.put(edp2, tempSet);

                    nodeSet.add(triple);
                    String lp = triple2lp.get(triple);
                    tempSet = invertedMap.getOrDefault(lp, new HashSet<>());
                    tempSet.add(triple);
                    invertedMap.put(lp, tempSet);
                }
                else {
                    nodeSet.add(spo[0]);
                    String edp0 = entity2edp.get(spo[0]);
                    Set<String> tempSet = invertedMap.getOrDefault(edp0, new HashSet<>());
                    tempSet.add(spo[0]);
                    invertedMap.put(edp0, tempSet);
                }
            }
            try {
                Map<String, Integer> node2Id = new HashMap<>();
                PrintWriter subNameWriter = new PrintWriter(compFolder + "/subName.txt");
                int count = 0;
                for (String iter: nodeSet) {
                    subNameWriter.println(iter);
                    node2Id.put(iter, count);
                    count++;
                }
                subNameWriter.close();

                PrintWriter keyMapWriter = new PrintWriter(compFolder + "/keyMap.txt");
                for (String iter: ReadFile.readString(compFolder + "/edp.txt")) {
                    keyMapWriter.println(iter);
                }
                for (Integer iter: ReadFile.readInteger(compFolder + "/lp.txt")) {
                    keyMapWriter.println(lpId2LP.get(iter));
                }
                keyMapWriter.close();

                PrintWriter invTableWriter = new PrintWriter(compFolder + "/invertedTable.txt");
                for (Map.Entry<String, Set<String>> iter: invertedMap.entrySet()) {
                    Set<String> values = iter.getValue();
                    StringBuilder content = new StringBuilder();
                    for (String v: values) {
                        content.append(node2Id.get(v)).append(" ");
                    }
                    invTableWriter.println(iter.getKey() + ":" + content.toString().trim());
                }
                invTableWriter.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
            generateCHL(compFolder);
            System.out.println("Finish component " + comp + ". \n");
        }
    }

    private static long generateCHL(String baseFolder) {
        long startTime = System.currentTimeMillis();
        try {
            WeightedGraph ww = new WeightedGraph();
            ww.graphIndexRead2(baseFolder);
            WeightedPLL w2 = new WeightedPLL();
            w2.pllIndexDeal2(ww, baseFolder);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis() - startTime;
    }

    public static void main(String[] args) {
//        String folder = "D:\\workplace\\calForRankDashboard\\Data-and-Code\\code\\example\\"; // change to your local folder where there are graph.txt and label.txt files.
        connection = new DBUtil().conn;
        getPatternIndexes(2);
        getComponents(2);
        getSetCoverComponents(2);
        getHubLabel(2);

        //TODO 需修改PATHS里的路径   DBUtils里的配置
//        for(int i=1;i<=;i++){
//            getPatternIndexes(i);
//            getComponents(i);
//            getSetCoverComponents(i);
//            getHubLabel(i);
//        }
    }
}
