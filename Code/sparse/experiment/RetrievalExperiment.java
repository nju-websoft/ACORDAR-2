package sparse.experiment;

import javafx.util.Pair;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class RetrievalExperiment {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private IndexReader indexReader;
    private Directory directory;

    private final RelevanceRanking relevanceRanking = new RelevanceRanking();
    
    public void testResults5Folds() {
        String query_fields = "C";
        for (int si = 0; si < 5; si ++) {
            System.out.println("Fold: " + (si + 2) % 5);
            List<Map<String, Object>> res = jdbcTemplate.queryForList("SELECT * FROM query_5folds WHERE split=" + (si + 1) % 5 + " ORDER BY query_id");
            for (Map<String, Object> qi : res) {
                int query_id = Integer.parseInt(qi.get("query_id").toString());
                String query_text = qi.get("query_text").toString();
                List<Pair<Integer, Double>> BM25ScoreList = relevanceRanking.LuceneRankingList(query_text, new BM25Similarity(), until.GlobalVariances.BM25ContentBoostWeights5Folds[si]);
                String sql = "INSERT INTO result_5folds_with_score VALUES (?, ?, ?, ?, ?, ?, ?);";
                int len = BM25ScoreList.size();
                for (int i = 0; i < Math.min(10, len); i++) {
//                    System.out.println(BM25ScoreList.get(i).getValue());
                    jdbcTemplate.update(sql, null, query_id, BM25ScoreList.get(i).getKey(), "BM25_"+query_fields, i + 1, (si + 2) % 5, BM25ScoreList.get(i).getValue());
                }
                List<Pair<Integer, Double>> TFIDFScoreList = relevanceRanking.LuceneRankingList(query_text, new ClassicSimilarity(), until.GlobalVariances.TFIDFContentBoostWeights5Folds[si]);
                sql = "INSERT INTO result_5folds_with_score VALUES (?, ?, ?, ?, ?, ?, ?);";
                len = TFIDFScoreList.size();
                for (int i = 0; i < Math.min(10, len); i++) {
                    jdbcTemplate.update(sql, null, query_id, TFIDFScoreList.get(i).getKey(), "TFIDF_"+query_fields, i + 1, (si + 2) % 5, TFIDFScoreList.get(i).getValue());
                }
                List<Pair<Integer, Double>> LMDScoreList = relevanceRanking.LuceneRankingList(query_text, new LMDirichletSimilarity(), until.GlobalVariances.LMDContentBoostWeights5Folds[si]);
                sql = "INSERT INTO result_5folds_with_score VALUES (?, ?, ?, ?, ?, ?, ?);";
                len = LMDScoreList.size();
                for (int i = 0; i < Math.min(10, len); i++) {
                    jdbcTemplate.update(sql, null, query_id, LMDScoreList.get(i).getKey(), "LMD_"+query_fields, i + 1, (si + 2) % 5, LMDScoreList.get(i).getValue());
                }
                List<Pair<Integer, Double>> FSDMScoreList = relevanceRanking.FSDMRankingList(query_text, until.GlobalVariances.FSDMContentBoostWeights5Folds[si]);
                sql = "INSERT INTO result_5folds_with_score VALUES (?, ?, ?, ?, ?, ?, ?);";
                len = FSDMScoreList.size();
                for (int i = 0; i < Math.min(10, len); i++) {
//                    System.out.println(FSDMScoreList.get(i).getValue());
                    jdbcTemplate.update(sql, null, query_id, FSDMScoreList.get(i).getKey(), "FSDM_"+query_fields, i + 1, (si + 2) % 5, FSDMScoreList.get(i).getValue());
                }
                System.out.println("query: " + query_id);
            }
        }
    }
}
