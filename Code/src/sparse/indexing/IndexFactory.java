package sparse.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class IndexFactory {

    private final Logger logger = LoggerFactory.getLogger(IndexFactory.class);
    public IndexWriter indexWriter = null;
    public Integer commit_cnt = 0;

    public void commitDocument(Document document) {
        try {
            indexWriter.addDocument(document);
            commit_cnt ++;
            indexWriter.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeIndexWriter() {
        try {
            indexWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String storePath, Analyzer analyzer) {
        try {
            Directory directory = MMapDirectory.open(Paths.get(storePath));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            indexWriter = new IndexWriter(directory, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
