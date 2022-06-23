package datasetretrieval2021.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class Statistics {
    public static List<String> getTokens(String S) throws IOException {
        List<String> res = new ArrayList<>(); res.clear();
        Analyzer analyzer = GlobalVariances.globalAnalyzer;
        //第一个参数fieldName没有实际用处
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
    
}
