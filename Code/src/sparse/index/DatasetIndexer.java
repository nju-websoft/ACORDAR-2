package datasetretrieval2021.demo;

import datasetretrieval2021.demo.Bean.TripleID;
import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.sql.In;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Repository
public class DBIndexer {

    private final JdbcTemplate jdbcTemplate;

    private final Logger logger = LoggerFactory.getLogger(DBIndexer.class);

    private Set<String> msgSet;
    private Set<TripleID> tripleSet;

    private Set<Pair<Integer, Integer>> classSet;

    int totalTripleCount;

    private final IndexFactory indexFactory = new IndexFactory();

    public DBIndexer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Map<Pair<Integer, Integer>, Pair<Pair<String, String>, Integer>> id2label;
    private Map<Pair<String, Integer>, Pair<Integer, Integer>> textKind2id;

    public void MapId2Text(int file_id) {
        String sql = "";
        sql = "SELECT COUNT(1) FROM rdf_term WHERE file_id=" + file_id;
        int totalLabelCount = jdbcTemplate.queryForObject(sql, Integer.class);
        for (int i = 0; i <= totalLabelCount / GlobalVariances.maxListNumber; i++) {
            List<Map<String, Object>> queryList = jdbcTemplate.queryForList(String.format("SELECT id,iri,label,kind FROM rdf_term WHERE file_id=%d LIMIT %d,%d", file_id, i * GlobalVariances.maxListNumber, GlobalVariances.maxListNumber));
            for (Map<String, Object> qi : queryList) {
                Integer id = Integer.parseInt(qi.get("id").toString());
                Integer kind = Integer.parseInt(qi.get("kind").toString());
                String label = "";
                if (qi.get("label") != null)
                    label = qi.get("label").toString();
                String text = "";
                if (qi.get("iri") != null)
                    text = qi.get("iri").toString();
                id2label.put(new Pair<>(file_id, id), new Pair<>(new Pair<>(label, text), kind));
            }
            logger.info("Completed mapping Id to text: " + Math.min((i + 1) * GlobalVariances.maxListNumber, totalLabelCount) + "/" + totalLabelCount);
        }
    }
    public String queryLabel(Pair<Integer, Integer> file_term_id) {
        return id2label.get(file_term_id).getKey().getKey();
    }

    public String queryText(Pair<Integer, Integer> file_term_id) {
        return id2label.get(file_term_id).getKey().getValue();
    }

    public Integer queryKind(Pair<Integer, Integer> file_term_id) {
        return id2label.get(file_term_id).getValue();
    }

    private void getClassSet() {
        classSet = new HashSet<>();
        List<Map<String, Object>> queryList = new ArrayList<>();
        queryList = jdbcTemplate.queryForList("SELECT file_id,class_id FROM class");
        for (Map<String, Object> qi : queryList) {
            int file_id = (Integer) qi.get("file_id");
            int class_id = (Integer) qi.get("class_id");
            classSet.add(new Pair<>(file_id, class_id));
        }
        logger.info("Completed getting classSet.");
    }

    private void addCount(Map<Integer, Integer> termCount, int id) {
        if(!termCount.containsKey(id)) termCount.put(id, 0);
        int val = termCount.get(id);
        termCount.put(id, val + 1);
    }

    private void scanTriplesOfDatasetWithOneFile(int file_id, Document doc, FieldType fieldType) throws IOException {
        List<Map<String, Object>> queryList = new ArrayList<>();

        logger.info("Start MapId2Text: " + file_id);
        MapId2Text(file_id);
        logger.info("Completed MapId2Text: " + file_id);

        int tripleCount = 0;

        String tableName = "triple";
        String sql = "";
        sql = "SELECT COUNT(1) FROM " + tableName + " WHERE file_id=" + file_id;
        totalTripleCount = jdbcTemplate.queryForObject(sql, Integer.class);
        if (totalTripleCount == 0) {
            tableName = "triple_socrata";
            sql = "SELECT COUNT(1) FROM " + tableName + " WHERE file_id=" + file_id;
            totalTripleCount = jdbcTemplate.queryForObject(sql, Integer.class);
        }

        logger.info("file id: " + file_id + ", total triple number: " + totalTripleCount);

        Integer subjectKind = 0;
        Integer objectKind = 0;

        for (int i = 0; i <= totalTripleCount / GlobalVariances.maxListNumber; i++) {
            if (tripleCount >= GlobalVariances.maxTripleNumber)
                break;
            sql = "SELECT subject,predicate,object FROM " + tableName + " WHERE file_id=" + file_id +" LIMIT "+ i * GlobalVariances.maxListNumber + "," + GlobalVariances.maxListNumber + ";";
            queryList = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> qi : queryList) {
                int subject_id = (Integer) qi.get("subject");
                int predicate_id = (Integer) qi.get("predicate");
                int object_id = (Integer) qi.get("object");

                Pair<Integer, Integer> sub = new Pair<>(file_id, subject_id);
                Pair<Integer, Integer> pre = new Pair<>(file_id, predicate_id);
                Pair<Integer, Integer> obj = new Pair<>(file_id, object_id);

                subjectKind = queryKind(sub);
                objectKind = queryKind(obj);

                String termText;

                if (subjectKind == 0) {
                    termText = queryLabel(sub);
                    doc.add(new Field("entity", termText, fieldType));
                }

                termText = queryLabel(pre);
                doc.add(new Field("property", termText, fieldType));

                if (classSet.contains(new Pair<>(file_id, object_id))) {
                    termText = queryLabel(obj);
                    doc.add(new Field("class", termText, fieldType));
                } else if (objectKind == 0) {
                    termText = queryLabel(obj);
                    doc.add(new Field("entity", termText, fieldType));
                } else if (objectKind == 2) {
                    termText = queryLabel(obj);
                    doc.add(new Field("literal", termText, fieldType));
                }
                tripleCount ++;
            }
            logger.info("Completed mapping triple to text: " + Math.min((i + 1) * GlobalVariances.maxListNumber, totalTripleCount) + "/" + totalTripleCount);
        }
    }

    private void scanTriplesOfDatasetWithMultiFile(List<Integer> fileIdList, Document doc, FieldType fieldType) throws IOException {
        List<Map<String, Object>> queryList = new ArrayList<>();

        Set<String> currentMsgSet;

        Integer subjectKind = 0;
        Integer predicateKind = 0;
        Integer objectKind = 0;

        for (int file_id : fileIdList) {

            logger.info("Start MapId2Text: " + file_id);
            MapId2Text(file_id);
            logger.info("Completed MapId2Text: " + file_id);

            int totalTripleCount;
            int tripleCount = 0;

            String tableName = "triple";
            String sql = "";
            sql = "SELECT COUNT(1) FROM " + tableName + " WHERE file_id=" + file_id;
            totalTripleCount = jdbcTemplate.queryForObject(sql, Integer.class);
            if (totalTripleCount == 0) {
                tableName = "triple_socrata";
                sql = "SELECT COUNT(1) FROM " + tableName + " WHERE file_id=" + file_id;
                totalTripleCount = jdbcTemplate.queryForObject(sql, Integer.class);
            }

            logger.info("file id: " + file_id + ", total triple number: " + totalTripleCount);

            currentMsgSet = new HashSet<>();

            String subjectText = "";
            String predicateText = "";
            String objectText = "";

            for (int i = 0; i <= totalTripleCount / GlobalVariances.maxListNumber; i++) {
                if (tripleCount >= GlobalVariances.maxTripleNumber)
                    break;
                sql = "SELECT msg_code,subject,predicate,object FROM " + tableName + " WHERE file_id=" + file_id + " LIMIT " + i * GlobalVariances.maxListNumber + "," + GlobalVariances.maxListNumber + ";";
                queryList = jdbcTemplate.queryForList(sql);
                for (Map<String, Object> qi : queryList) {
                    int subject_id = (Integer) qi.get("subject");
                    int predicate_id = (Integer) qi.get("predicate");
                    int object_id = (Integer) qi.get("object");
                    Pair<Integer, Integer> sub = new Pair<>(file_id, subject_id);
                    Pair<Integer, Integer> pre = new Pair<>(file_id, predicate_id);
                    Pair<Integer, Integer> obj = new Pair<>(file_id, object_id);

                    String msg_code = qi.get("msg_code").toString();

                    if (msgSet.contains(msg_code))
                        continue;
                    else {
                        currentMsgSet.add(msg_code);

                        subjectKind = queryKind(sub);
                        predicateKind = queryKind(pre);
                        objectKind = queryKind(obj);

                        subjectText = queryText(sub);
                        predicateText = queryText(pre);
                        objectText = queryText(obj);

                        Pair<String, Integer> termPair = new Pair<>(subjectText, subjectKind);
                        if (textKind2id.containsKey(termPair)) {
                            sub = textKind2id.get(termPair);
                        } else {
                            textKind2id.put(termPair, sub);
                        }
                        termPair = new Pair<>(predicateText, predicateKind);
                        if (textKind2id.containsKey(termPair)) {
                            pre = textKind2id.get(termPair);
                        } else {
                            textKind2id.put(termPair, pre);
                        }
                        termPair = new Pair<>(objectText, objectKind);
                        if (textKind2id.containsKey(termPair)) {
                            obj = textKind2id.get(termPair);
                        } else {
                            textKind2id.put(termPair, obj);
                        }
                        tripleSet.add(new TripleID(sub, pre, obj));
                    }
                    tripleCount++;
                }
                logger.info("Completed scanning triple: " + Math.min((i + 1) * GlobalVariances.maxListNumber, totalTripleCount) + "/" + totalTripleCount);
            }
            msgSet.addAll(currentMsgSet);
        }

        totalTripleCount = tripleSet.size();
        int tripleCount = 0;

        for (TripleID tri : tripleSet) {

            Pair<Integer, Integer> sub = tri.getSub();
            Pair<Integer, Integer> pre = tri.getPre();
            Pair<Integer, Integer> obj = tri.getObj();

            //System.out.println(sub + " " + pre + " " +obj);

            subjectKind = queryKind(sub);
            objectKind = queryKind(obj);

            String termText;

            if (subjectKind == 0) {
                termText = queryLabel(sub);
                doc.add(new Field("entity", termText, fieldType));
            }

            termText = queryLabel(pre);
            doc.add(new Field("property", termText, fieldType));

            if (classSet.contains(obj)) {
                termText = queryLabel(obj);
                doc.add(new Field("class", termText, fieldType));
            } else if (objectKind == 0) {
                termText = queryLabel(obj);
                doc.add(new Field("entity", termText, fieldType));
            } else if (objectKind == 2) {
                termText = queryLabel(obj);
                doc.add(new Field("literal", termText, fieldType));
            }
            tripleCount ++;
            if (tripleCount % GlobalVariances.maxListNumber == 0) {
                logger.info("Completed mapping triple to text: " + Math.min(tripleCount, totalTripleCount) + "/" + totalTripleCount);
            }
        }
    }

    private void generateDocument() throws IOException {
        int datasetCount = 0;
        List<Map<String, Object>> queryList = new ArrayList<>();
        List<Integer> datasetIdList = new ArrayList<>();

        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

        datasetIdList = jdbcTemplate.queryForList("SELECT DISTINCT(dataset_id) FROM pid", Integer.class);
        //System.out.println(datasetIdList);

        int totalCount = datasetIdList.size();

        for (int dataset_id : datasetIdList) {
            logger.info("Start generating document: " + dataset_id);
            datasetCount ++;
            Document document = new Document();
            List<Integer> fileIdList = jdbcTemplate.queryForList(String.format("SELECT file_id FROM pid WHERE dataset_id=%d", dataset_id), Integer.class);
            queryList = jdbcTemplate.queryForList(String.format("SELECT * FROM dataset_summary WHERE file_id=%d", fileIdList.get(0)));
            for (Map.Entry<String, Object> entry : queryList.get(0).entrySet()) {
                String name = entry.getKey();
                String value = "";
                if (entry.getValue() != null)
                    value = entry.getValue().toString();
                if (name.equals("tags")) {
                    String[] tags = value.split(";");
                    for (String si : tags) {
                        document.add(new Field(name, si, fieldType));
                    }
                } else {
                    document.add(new Field(name, value, fieldType));
                }
            }
            queryList.clear();
            msgSet = new HashSet<>();
            tripleSet = new HashSet<>();
            textKind2id = new HashMap<>();
            id2label = new HashMap<>();
            if (fileIdList.size() == 1) {
                scanTriplesOfDatasetWithOneFile(fileIdList.get(0), document, fieldType);
            } else {
                scanTriplesOfDatasetWithMultiFile(fileIdList, document, fieldType);
            }

            document.add(new Field("triple_count", String.valueOf(totalTripleCount), fieldType));
            // commit document
            indexFactory.commitDocument(document);
            logger.info("Generated document: " + dataset_id + " , count: " + datasetCount + "/" + totalCount);
        }
        logger.info("Completed generating document, total: " + datasetCount);
    }

    public void main() throws IOException {
        logger.info("Start.");
        Analyzer analyzer = GlobalVariances.globalAnalyzer;
        indexFactory.init(GlobalVariances.store_Dir, analyzer);
        getClassSet();
        generateDocument();
        indexFactory.closeIndexWriter();
        logger.info("Completed.");
    }
}
