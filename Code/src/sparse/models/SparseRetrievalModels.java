package sparse.models;

import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class SparseRetrievalModels {
    private static Directory directory = null;
    private static IndexReader indexReader = null;
    private static IndexSearcher indexSearcher = null;
    private Map<String, Double> wT;
    private Map<String, Double> wO;
    private Map<String, Double> wU;
    private Map<Pair<String, String>, Long> fieldTermFreq;
    private Map<String, List<String>> fieldContent;
    private Map<String, Long> fieldDocLength;

    public void init() {
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

    private static List<String> getTokens(String S) throws IOException {
        List<String> res = new ArrayList<>(); res.clear();
        Analyzer analyzer = until.GlobalVariances.globalAnalyzer;
        TokenStream tokenStream = analyzer.tokenStream("", new StringReader(S));
        tokenStream.reset();
        CharTermAttribute charTerm = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            res.add(charTerm.toString());
        }
        tokenStream.close();
        //System.out.println(res);
        return res;
    }

    private void getCollectionStatistics(List<String> tokens, float[] weights) {
        try {
            wT = new HashMap<>();
            wO = new HashMap<>();
            wU = new HashMap<>();
            fieldTermFreq = new HashMap<>();
            double base = 0.0;
            for (int i = 0; i < until.GlobalVariances.queryFields.length; i ++) {
                String field = until.GlobalVariances.queryFields[i];
                Double w = (double) weights[i];
                base += w;
                for (String token : tokens) {
                    fieldTermFreq.put(new Pair<>(field, token), indexReader.totalTermFreq(new Term(field, new BytesRef(token))));
                }
            }
            for (int i = 0; i < until.GlobalVariances.queryFields.length; i ++) {
                String field = until.GlobalVariances.queryFields[i];
                Double w = (double) weights[i] / base;
                wT.put(field, w);
                wO.put(field, w);
                wU.put(field, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDocumentStatistics(Integer doc_id) {
        try {
            fieldContent = new HashMap<>();
            fieldDocLength = new HashMap<>();

            Document document = indexReader.document(doc_id);
            for (String field : until.GlobalVariances.queryFields)  {
                if (document.get(field) == null)
                    fieldContent.put(field, new ArrayList<>());
                else {
                    String fieldText = Arrays.toString(document.getValues(field));
                    fieldContent.put(field, getTokens(fieldText));
                }
                Terms terms = indexReader.getTermVector(doc_id, field);
                if (terms != null) fieldDocLength.put(field, terms.getSumTotalTermFreq());
                else fieldDocLength.put(field, 0L);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Double getTF_T(Integer doc_id, String field, String qi) {
        double res = 0.0;
        try {
            Terms terms = indexReader.getTermVector(doc_id, field);
            BytesRef bytesRef = new BytesRef(qi);
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                if (termsEnum.seekExact(bytesRef))
                    res = (double) termsEnum.totalTermFreq();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(qi + " TF_T: " + res);
        return res;
    }

    private Double getTF_O(String field, String qi1, String qi2) {
        double res = 0.0;
        List<String> content = fieldContent.get(field);
        for (int i = 0; i + 1 < content.size(); i++) {
            if (qi1.equals(content.get(i)) && qi2.equals(content.get(i + 1)))
                res += 1.0;
        }
        //System.out.println(qi1 + " " + qi2 + " TF_O: " + res);
        return res;
    }

    private Double getTF_U(String field, String qi1, String qi2) {
        double res = 0.0;
        List<String> content = fieldContent.get(field);
        for (Integer i = 0; i + until.GlobalVariances.FSDMUWindowSize <= content.size(); i++) {
            Set<String> window = new HashSet<>();
            for (Integer j = 0; j < until.GlobalVariances.FSDMUWindowSize; j++) {
                window.add(content.get(i + j));
            }
            if (window.contains(qi1) && window.contains(qi2))
                res += 1.0;
        }
        //System.out.println(qi1 + " " + qi2 + " TF_U: " + res);
        return res;
    }

    private Double getFSDM_T(Integer doc_id, List<String> queries) {
        double res = 0.0;
        try {
            for (String qi : queries) {
                double tmp = 0.0;
                double eps = 1e-100;
                for (String field : until.GlobalVariances.queryFields) {
                    if (wT.get(field) == 0.0) continue;
                    double miu = (double) indexReader.getSumTotalTermFreq(field) / (double) indexReader.getDocCount(field);
                    double Cj = (double) indexReader.getSumTotalTermFreq(field);
                    double cf = 0.0;
                    if (fieldTermFreq.containsKey(new Pair<>(field, qi)))
                        cf = (double) fieldTermFreq.get(new Pair<>(field, qi));
                    double Dj = (double) fieldDocLength.get(field);
                    tmp += wT.get(field) * (getTF_T(doc_id, field, qi) + miu * cf / Cj) / (Dj + miu);
                }
                //System.out.println("T: " + tmp);
                res += Math.log(tmp + eps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("FSDM_T: " + res);
        return res;
    }

    private Double getFSDM_O(List<String> queries) {
        double res = 0.0;
        try {
            for (int i = 0; i + 1 < queries.size(); i++) {
                double tmp = 0.0;
                double eps = 1e-100;
                String qi1 = queries.get(i);
                String qi2 = queries.get(i + 1);
                for (String field : until.GlobalVariances.queryFields) {
                    if (wO.get(field) == 0.0) continue;
                    double miu = (double) indexReader.getSumTotalTermFreq(field) / (double) indexReader.getDocCount(field);
                    double Cj = (double) indexReader.getSumTotalTermFreq(field);
                    double cf = 0.0;
                    if (fieldTermFreq.containsKey(new Pair<>(field, qi1)))
                        cf = (double) fieldTermFreq.get(new Pair<>(field, qi1));
                    if (fieldTermFreq.containsKey(new Pair<>(field, qi2)))
                        cf = Math.min(cf, (double) fieldTermFreq.get(new Pair<>(field, qi2)));
                    double Dj = (double) fieldDocLength.get(field);
                    tmp += wO.get(field) * (getTF_O(field, qi1, qi2) + miu * cf / Cj) / (Dj + miu);
                }
                //System.out.println("O: " + tmp);
                res += Math.log(tmp + eps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("FSDM_O: " + res);
        return res;
    }

    private Double getFSDM_U(List<String> queries) {
        double res = 0.0;
        try {
            for (int i = 0; i + 1 < queries.size(); i++) {
                double tmp = 0.0;
                double eps = 1e-100;
                String qi1 = queries.get(i);
                String qi2 = queries.get(i + 1);
                for (String field : until.GlobalVariances.queryFields) {
                    if (wU.get(field) == 0.0) continue;
                    double miu = (double) indexReader.getSumTotalTermFreq(field) / (double) indexReader.getDocCount(field);
                    double Cj = (double) indexReader.getSumTotalTermFreq(field);
                    double cf = 0.0;
                    if (fieldTermFreq.containsKey(new Pair<>(field, qi1)))
                        cf = (double) fieldTermFreq.get(new Pair<>(field, qi1));
                    if (fieldTermFreq.containsKey(new Pair<>(field, qi2)))
                        cf = Math.min(cf, (double) fieldTermFreq.get(new Pair<>(field, qi2)));
                    double Dj = (double) fieldDocLength.get(field);
                    tmp += wU.get(field) * (getTF_U(field, qi1, qi2) + miu * cf / Cj) / (Dj + miu);
                }
                //System.out.println("U: " + tmp);
                res += Math.log(tmp + eps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("FSDM_U: " + res);
        return res;
    }

    /**
     * Calculating FSDM score
     * @param doc_id
     * @param tokens
     * @param weights
     * @return
     */
    public Double FSDM(Integer doc_id, List<String> tokens, float[] weights) {
        Double lambdaT = 0.8;
        Double lambdaO = 0.1;
        Double lambdaU = 0.1;
        getCollectionStatistics(tokens, weights);
        getDocumentStatistics(doc_id);
        return lambdaT * getFSDM_T(doc_id, tokens) +
                lambdaO * getFSDM_O(tokens) +
                lambdaU * getFSDM_U(tokens);
    }

    /**
     * Get the list sorted by Lucene score
     * @param query
     * @param similarity (ClassicSimilarity() for TF-IDF, BM25Similarity() for BM25F, LMDirichletSimilarity() for LMD)
     * @param weights
     * @return
     */
    public List<Pair<Integer, Double>> LuceneRankingList(String query, Similarity similarity, float[] weights) {
        init();
        List<Pair<Integer, Double>> LuceneRankingList = new ArrayList<>();
        try {
            String[] fields = until.GlobalVariances.queryFields;
            Analyzer analyzer = until.GlobalVariances.globalAnalyzer;
            Map<String,Float> boosts = new HashMap<>();
            for (int i = 0; i < fields.length; i ++) {
                boosts.put(fields[i], weights[i]);
            }
            QueryParser queryParser = new MultiFieldQueryParser(fields, analyzer, boosts);
            query=QueryParser.escape(query);
            Query parsedQuery = queryParser.parse(query);
            indexSearcher.setSimilarity(similarity);
            TopDocs docsSearch = indexSearcher.search(parsedQuery, until.GlobalVariances.HitSize);
            ScoreDoc[] scoreDocs = docsSearch.scoreDocs;
            for (ScoreDoc si : scoreDocs) {
                int docID = si.doc;
                Set<String> fieldsToLoad = new HashSet<>();
                fieldsToLoad.add("dataset_id");
                Document document = indexReader.document(docID, fieldsToLoad);
                Integer datasetID = Integer.parseInt(document.get("dataset_id"));
                LuceneRankingList.add(new Pair<>(datasetID, (double) si.score));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return LuceneRankingList;
    }

    /**
     * Get the list sorted by FSDM score
     * @param query
     * @param weights
     * @return
     */
    public List<Pair<Integer, Double>> FSDMRankingList(String query, float[] weights) {
        init();
        List<Pair<Integer, Double>> FSDMScoreList = new ArrayList<>();
        try {
            String[] fields = until.GlobalVariances.queryFields;
            Analyzer analyzer = until.GlobalVariances.globalAnalyzer;
            QueryParser queryParser = new MultiFieldQueryParser(fields, analyzer);
            query=QueryParser.escape(query);
            Query parsedQuery = queryParser.parse(query);
            TopDocs docsSearch = indexSearcher.search(parsedQuery, until.GlobalVariances.HitSize);
            ScoreDoc[] scoreDocs = docsSearch.scoreDocs;
            List<String> queryTokens = getTokens(query);
            for (ScoreDoc si : scoreDocs) {
                int docID = si.doc;
                Set<String> fieldsToLoad = new HashSet<>();
                fieldsToLoad.add("dataset_id");
                Document document = indexReader.document(docID, fieldsToLoad);
                Integer datasetID = Integer.parseInt(document.get("dataset_id"));
                Double score = 0.0;

                //System.out.println("dataset_id: " + datasetID);
                score = FSDM(docID, queryTokens, weights);
                FSDMScoreList.add(new Pair<>(datasetID, score));
            }
            FSDMScoreList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FSDMScoreList;
    }
}
