package sparse.fieldweighttuning;

import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Paths;
import java.util.*;

public class GridSearch {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final Logger logger = LoggerFactory.getLogger(GridSearchTest.class);

    private final RelevanceRanking relevanceRanking = new RelevanceRanking();

    private List<Pair<Integer, Double>> resultList = new ArrayList<>();
    private final List<Double> nDCGList = new ArrayList<>();
    private final Map<Integer, String> queryMap = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Integer> ratingMap = new HashMap<>();
    private final Map<Integer, List<Integer>> idealRatingMap = new HashMap<>();
    private final Map<Integer, List<Double>> qualityMetricsMap = new HashMap<>();
    private float[] bestBoosts;
    private double maxNDCG = 0.0;

    private final Map<Pair<Integer, Integer>, Map<String, Double>> scoreMap = new HashMap<>();
    private final Map<Integer, List<Pair<Integer, Double>>> relevanceScoreMap = new HashMap<>();

    private static Directory directory = null;
    private static IndexReader indexReader = null;
    private static IndexSearcher indexSearcher = null;
    private final Map<Integer, List<Integer>> datasetPool = new HashMap<>();

    public void init(int split_i) {
        List<Map<String, Object>> queryList = jdbcTemplate.queryForList("SELECT * FROM query_5folds WHERE split<>" + (split_i+4)%5 );
        for (Map<String, Object> qi : queryList) {
            int query_id = Integer.parseInt(qi.get("query_id").toString());
            String query_text = qi.get("query_text").toString();
            queryMap.put(query_id, query_text);
        }
       bestBoosts = new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        List<Map<String, Object>> pairList = jdbcTemplate.queryForList("SELECT * FROM query_dataset_5folds WHERE split<>" + (split_i+4)%5 );
        for (Map<String, Object> qi : pairList) {
            int query_id = Integer.parseInt(qi.get("query_id").toString());
            int dataset_id = Integer.parseInt(qi.get("dataset_id").toString());
            int rating = Integer.parseInt(qi.get("rel_score").toString());
            ratingMap.put(new Pair<>(query_id, dataset_id), rating);
        }

        for (Map<String, Object> qi : queryList) {
            int query_id = Integer.parseInt(qi.get("query_id").toString());
            List<Integer> idealRating = jdbcTemplate.queryForList("SELECT rel_score FROM query_dataset_5folds WHERE query_id=" + query_id + " ORDER BY rel_score DESC", Integer.class);
            idealRatingMap.put(query_id, idealRating);
        }
    }

    public double calculateDCG(List<Integer> ratingList, int k) {
        double res = 0.0;
        for (int i = 1; i <= k; i++) {
            res += ((double) ratingList.get(i - 1)) / (Math.log(i + 1.0) / Math.log(2.0));
        }
        return res;
    }

    public double calculateNDCG(int k, int query_id) {
        List<Integer> resultRating = new ArrayList<>();
        List<Integer> idealRating = idealRatingMap.get(query_id);
        for (Pair<Integer, Double> qi : resultList) {
            int dataset_id = qi.getKey();
            resultRating.add(ratingMap.getOrDefault(new Pair<>(query_id, dataset_id), 0));
        }
        while (resultRating.size() < k) {
            resultRating.add(0);
        }
        double DCG = calculateDCG(resultRating, k);
        double iDCG = calculateDCG(idealRating, k);
        return DCG / iDCG;
    }

    public void getScoreMap(Integer query_id, Similarity similarity) {
        try {
            String query = queryMap.get(query_id);
            String[] fields = until.GlobalVariances.queryFields;
            Analyzer analyzer = until.GlobalVariances.globalAnalyzer;
            QueryParser queryParser = new MultiFieldQueryParser(fields, analyzer);
            query = QueryParser.escape(query);
            Query parsedQuery = queryParser.parse(query);
            indexSearcher.setSimilarity(similarity);
            TopDocs docsSearch = indexSearcher.search(parsedQuery, until.GlobalVariances.HitSize);
            ScoreDoc[] scoreDocs = docsSearch.scoreDocs;
            List<Integer> datasetList = new ArrayList<>();
            for (ScoreDoc si : scoreDocs) {
                int docID = si.doc;
                Set<String> fieldsToLoad = new HashSet<>();
                fieldsToLoad.add("dataset_id");
                Document document = indexReader.document(docID, fieldsToLoad);
                Integer dataset_id = Integer.parseInt(document.get("dataset_id"));
//                System.out.println("dataset_id: " + document.get("dataset_id") + ", score: " + si.score);
                Explanation e = indexSearcher.explain(parsedQuery, si.doc);
//                System.out.println("Explanation： \n" + e);
                Map<String, Double> tmp = new HashMap<>();
                for (Explanation ei : e.getDetails()) {
                    String field = ei.getDescription();
                    field = field.substring(field.indexOf('(')+1, field.indexOf(':'));
                    double score = ei.getValue().doubleValue();
                    if (tmp.containsKey(field)) {
                        score += tmp.get(field);
                    }
                    tmp.put(field, score);
                }
                scoreMap.put(new Pair<>(query_id, dataset_id), tmp);
                datasetList.add(dataset_id);
            }
            datasetPool.put(query_id, datasetList);
        } catch(
        Exception e) {
            e.printStackTrace();
        }
    }

    public void getRelevanceScoreMap(int query_id, Similarity similarity, float[] weights) {
        String query = queryMap.get(query_id);
        relevanceScoreMap.put(query_id, relevanceRanking.LuceneRankingList(query, similarity, weights));
    }

    public void initIndex() {
        try {
            if (directory == null) {
                directory = MMapDirectory.open(Paths.get(until.GlobalVariances.index_Dir));
                indexReader = DirectoryReader.open(directory);
                indexSearcher = new IndexSearcher(indexReader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initRanking(Similarity similarity) {
        initIndex();
        for (int i : queryMap.keySet()) {
            getScoreMap(i, similarity);
//            getRelevanceScoreMap(i, new BM25Similarity(), until.GlobalVariances.BM25BoostWeights);
//            logger.info("query_id: " + i);
        }
        logger.info("Completed initRanking.");
    }

    public List<Pair<Integer, Double>> getRelevanceScoreList(int query_id, float[] weights) {
        List<Integer> datasetList = datasetPool.get(query_id);
        List<Pair<Integer, Double>> scoreList = new ArrayList<>();
        for(int i : datasetList) {
            Map<String, Double> fieldScoreMap = scoreMap.get(new Pair<>(query_id, i));
            double score = 0.0;
            String[] fields = until.GlobalVariances.queryFields;
            for (int j = 0; j < fields.length; j ++) {
                String field = fields[j];
                if (fieldScoreMap.containsKey(field)) {
                    score += weights[j] * fieldScoreMap.get(field);
                }
            }
            scoreList.add(new Pair<>(i, score));
        }
        scoreList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        if (scoreList.size() > 0) {
            double base = scoreList.get(0).getValue();
            for (int i = 0; i < scoreList.size(); i++) {
                scoreList.set(i, new Pair<>(scoreList.get(i).getKey(), scoreList.get(i).getValue() / base));
            }
        }
        return scoreList;
    }

    public List<Pair<Integer, Double>> getResultList(int query_id, float[] weights) {

        List<Pair<Integer, Double>> datasetList = relevanceScoreMap.get(query_id);
        List<Pair<Integer, Double>> scoreList = new ArrayList<>();
        for(Pair<Integer, Double> di : datasetList) {
            int dataset_id = di.getKey();
            double relevanceScore = di.getValue();
            double score = weights[0] * relevanceScore;
            List<Double> qualityList = qualityMetricsMap.get(dataset_id);
            for (int i = 1; i < weights.length; i ++) {
                score += weights[i] * qualityList.get(i-1);
            }
            scoreList.add(new Pair<>(dataset_id, score));
        }
        scoreList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        double base = scoreList.get(0).getValue();
        for (int i = 0; i < scoreList.size(); i ++) {
            scoreList.set(i, new Pair<>(scoreList.get(i).getKey(), scoreList.get(i).getValue() / base));
        }
        return scoreList;
    }


    public void gridSearch() {
        Queue< Pair<float[], Integer> > que = new LinkedList<>();
        int currentP = 0;
        que.offer(new Pair<>(bestBoosts,currentP));
        while(!que.isEmpty()) {
            Pair<float[], Integer> tmp = que.poll();
            int p = tmp.getValue();
            float[] now = tmp.getKey().clone();
            if (p == bestBoosts.length)
                continue;
            if (p != currentP) {
                logger.info("p: " + p + ", max: " + maxNDCG + ", best: " + Arrays.toString(bestBoosts));
                currentP = p;
            }
            for (int i = 1; i <= 10; i++) {
                now[p] = i / 10.0f;
                double avg = 0.0;
                for  (int j : queryMap.keySet()) {
                    resultList = getRelevanceScoreList(j, now);
//                    resultList = FSDMRankingList(j, now);
                    avg += calculateNDCG(10, j);
                }
                avg /= queryMap.size();
                if (avg > maxNDCG) {
                    bestBoosts = now.clone();
                    maxNDCG = avg;
                }
                que.offer(new Pair<>(now.clone(), p + 1));
            }
        }
    }

    public void BoostSearch() {
        init(0);
        initRanking(new BM25Similarity());
        gridSearch();
        logger.info(Arrays.toString(bestBoosts));
        logger.info(maxNDCG+"");
    }
}
