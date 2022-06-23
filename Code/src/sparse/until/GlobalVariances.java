package sparse.until;

import com.alibaba.fastjson.JSONObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Store global variables
 */
public class GlobalVariances {
	public static Analyzer initAnalyzer() {
        String str;
        List<String> stopWordsList = new ArrayList();
        try {
            FileReader fileReader = new FileReader("ACORDAR-2\\Code\\src\\sparse\\until\\stopwords\\nltk-stopwords.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((str = bufferedReader.readLine()) != null) {
                stopWordsList.add(str);
            }
            //System.out.println(stopWordsList);
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        CharArraySet cas = new CharArraySet(0,true);
        cas.addAll(stopWordsList);
        return new StandardAnalyzer(cas);
    }
    
    public static Analyzer globalAnalyzer = initAnalyzer();
    public static Integer maxTripleNumber = 1000000;

    // Index Storage Directory
    public static String store_Dir = "ACORDAR-2\\Code\\src\\sparse\\dataset_index";
    public static String index_Dir = "ACORDAR-2\\Code\\src\\sparse\\dataset_index";


    public static Integer maxListNumber = 100000;

    public static String[] queryFields = {"title", "description", "author", "tags", "entity", "literal", "class", "property"};
    public static String[] snippetFields = {"author", "tags", "entity", "literal", "class", "property"};

    public static Integer HitSize = 100;
    public static Integer FSDMUWindowSize = 8;

    public static Integer[] metricsK = {5, 10};

   public static String[] methodList = {"BM25F", "BM25F [m]", "BM25F [d]", "TFIDF", "TFIDF [m]", "TFIDF [d]", "LMD", "LMD [m]", "LMD [d]", "FSDM", "FSDM [m]", "FSDM [d]", "DPR", "DPR [m]", "DPR [d]", "ColBERT", "ColBERT [m]", "ColBERT [d]"};

    // pooling weights
    public static float[] BM25BoostWeights = {1.0f, 0.9f, 0.9f, 0.6f, 0.2f, 0.3f, 0.1f, 0.1f};
    public static float[] TFIDFBoostWeights = {1.0f, 0.7f, 0.9f, 0.9f, 0.8f, 0.5f, 0.1f, 0.4f};
    public static float[] LMDBoostWeights = {1.0f, 0.9f, 0.1f, 1.0f, 0.2f, 0.3f, 0.2f, 0.1f};
    public static float[] FSDMBoostWeights = {1.0f, 0.1f, 0.5f, 0.9f, 0.1f, 0.1f, 0.4f, 0.6f};

   	public static float[] BM25MetadataBoostWeights = {0.5f, 0.3f, 0.2f, 0.2f};
    public static float[] TFIDFMetadataBoostWeights = {1.0f, 0.6f, 0.4f, 0.5f};
    public static float[] LMDMetadataBoostWeights = {1.0f, 0.8f, 0.9f, 0.7f};
    public static float[] FSDMMetadataBoostWeights = {1.0f, 0.1f, 0.2f, 0.6f};

   	public static float[] BM25ContentBoostWeights = {0.1f, 0.7f, 0.2f, 0.2f};
    public static float[] TFIDFContentBoostWeights = {0.3f, 1.0f, 0.6f, 0.3f};
    public static float[] LMDContentBoostWeights = {0.3f, 1.0f, 0.1f, 0.6f};
    public static float[] FSDMContentBoostWeights = {1.0f, 0.6f, 0.1f, 0.1f};

    // ISWC'22
    // ALL weights 5 folds
    public static float[][] BM25BoostWeights5Folds = {
            {0.4f, 0.9f, 0.5f, 0.4f, 0.1f, 0.3f, 0.1f, 0.1f},
            {0.6f, 0.7f, 0.4f, 0.3f, 0.1f, 0.3f, 0.3f, 0.1f},
            {0.6f, 0.7f, 0.4f, 0.3f, 0.1f, 0.3f, 0.2f, 0.1f},
            {0.4f, 0.7f, 0.3f, 0.3f, 0.1f, 0.2f, 0.2f, 0.1f},
            {0.6f, 0.7f, 0.4f, 0.3f, 0.1f, 0.3f, 0.3f, 0.1f}
    };
    public static float[][] TFIDFBoostWeights5Folds = {
            {0.5f, 0.8f, 0.3f, 0.4f, 0.7f, 0.9f, 0.3f, 0.2f},
            {0.4f, 0.6f, 0.2f, 0.3f, 0.4f, 1.0f, 0.2f, 0.1f},
            {0.5f, 0.8f, 0.3f, 0.4f, 0.7f, 0.9f, 0.3f, 0.4f},
            {0.5f, 0.8f, 0.3f, 0.4f, 0.7f, 0.9f, 0.3f, 0.2f},
            {0.5f, 0.8f, 0.3f, 0.4f, 0.7f, 0.9f, 0.2f, 0.2f}

    };
    public static float[][] LMDBoostWeights5Folds = {
            {0.9f, 0.9f, 0.9f, 1.0f, 0.1f, 0.3f, 0.1f, 0.1f},
            {1.0f, 1.0f, 0.9f, 0.6f, 0.1f, 0.3f, 0.3f, 0.1f},
            {1.0f, 1.0f, 0.8f, 0.6f, 0.1f, 0.3f, 0.1f, 0.1f},
            {1.0f, 1.0f, 0.7f, 1.0f, 0.1f, 0.3f, 0.3f, 0.1f},
            {1.0f, 1.0f, 1.0f, 1.0f, 0.1f, 0.3f, 0.3f, 0.1f}
    };
    public static float[][] FSDMBoostWeights5Folds = {
            {0.4f, 0.5f, 0.2f, 0.2f, 0.8f, 0.2f, 0.2f, 0.1f},
            {0.5f, 0.4f, 0.8f, 0.2f, 1.0f, 0.4f, 0.6f, 0.2f},
            {0.4f, 0.5f, 0.2f, 0.2f, 0.7f, 0.2f, 0.2f, 0.1f},
            {0.4f, 0.5f, 0.2f, 0.2f, 0.8f, 0.2f, 0.2f, 0.1f},
            {0.4f, 0.5f, 0.2f, 0.2f, 0.3f, 0.2f, 0.2f, 0.1f}
    };

    // Metadata weights 5 folds
    public static float[][] BM25MetadataBoostWeights5Folds = {
            {0.8f, 1.0f, 0.9f, 0.8f},
            {0.8f, 1.0f, 0.9f, 0.8f},
            {1.0f, 0.9f, 0.7f, 0.5f},
            {0.5f, 0.8f, 0.5f, 0.3f},
            {1.0f, 0.9f, 0.6f, 0.6f}
    };
    public static float[][] TFIDFMetadataBoostWeights5Folds = {
            {0.6f, 1.0f, 0.3f, 0.5f},
            {0.6f, 0.7f, 0.3f, 0.5f},
            {0.6f, 1.0f, 0.3f, 0.5f},
            {0.5f, 0.8f, 0.3f, 0.4f},
            {0.6f, 1.0f, 0.3f, 0.5f}
        };
    public static float[][] LMDMetadataBoostWeights5Folds = {
            {0.7f, 0.7f, 1.0f, 0.6f},
            {0.7f, 0.7f, 1.0f, 0.6f},
            {0.5f, 0.4f, 0.6f, 0.4f},
            {0.6f, 0.6f, 0.8f, 0.5f},
            {0.5f, 0.5f, 0.7f, 0.6f}
        };
    public static float[][] FSDMMetadataBoostWeights5Folds = {
            {0.5f, 0.9f, 0.4f, 0.3f},
            {0.9f, 0.6f, 0.5f, 0.1f},
            {0.6f, 1.0f, 0.3f, 0.6f},
            {0.9f, 0.7f, 0.2f, 0.2f},
            {0.8f, 0.4f, 0.3f, 0.7f}
    };

    // Content weights 5 folds
    public static float[][] BM25ContentBoostWeights5Folds = {
            {0.1f, 0.8f, 0.5f, 0.5f},
            {0.1f, 0.8f, 0.5f, 0.5f},
            {0.1f, 0.8f, 0.5f, 0.5f},
            {0.1f, 0.8f, 0.5f, 0.5f},
            {0.1f, 0.8f, 0.5f, 0.5f}
    };
    public static float[][] TFIDFContentBoostWeights5Folds = {
            {0.3f, 0.9f, 0.1f, 0.2f},
            {0.3f, 0.9f, 0.1f, 0.2f},
            {0.2f, 0.9f, 0.1f, 0.2f},
            {0.3f, 1.0f, 0.1f, 0.2f},
            {0.3f, 0.9f, 0.1f, 0.2f}
        };
    public static float[][] LMDContentBoostWeights5Folds = {
            {0.1f, 0.6f, 0.4f, 0.2f},
            {0.2f, 0.8f, 0.8f, 0.3f},
            {0.1f, 0.7f, 0.9f, 0.6f},
            {0.3f, 0.9f, 0.9f, 0.4f},
            {0.2f, 1.0f, 0.9f, 0.2f}
        };
    public static float[][] FSDMContentBoostWeights5Folds = {
            {0.2f, 0.1f, 1.0f, 0.2f},
            {0.2f, 0.1f, 0.9f, 0.1f},
            {0.2f, 0.1f, 1.0f, 0.2f},
            {0.2f, 0.1f, 1.0f, 0.1f},
            {0.3f, 0.1f, 1.0f, 0.2f}
    };

    public static Integer poolingSize = 10;
}
