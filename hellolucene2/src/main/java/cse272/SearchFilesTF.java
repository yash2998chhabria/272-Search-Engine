package cse272;


import java.io.*;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.index.*;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class SearchFilesTF {
    public static void main(String[] args) throws Exception {
        String index = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/Index";
        String results_path = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/results/results-tf.txt";
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(results_path);


            IndexSearcher searcher = new IndexSearcher(reader);

            Analyzer analyzer = new StandardAnalyzer();
            searcher.setSimilarity(new TermSimilarity());

            String inputJsonFilePath = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/data/querydata.json";
            JSONArray jsonObjects = parseInputJsonFile(inputJsonFilePath);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{"title", "abstract", "mesh_terms"}, analyzer);

            String queryString = "";
            String queryNumber = "";

            long startTime = System.currentTimeMillis();

            for (Object O : jsonObjects) {
                JSONObject doc = (JSONObject) O;
                queryString = (String) doc.get("title") + " " + (String) doc.get("description");
                queryNumber = (String) doc.get("query_number");
                Query query = parser.parse(QueryParser.escape(queryString));
                performSearch(searcher, writer, queryNumber, query);
            }

            long endTime = System.currentTimeMillis();

            System.out.println("Total Time taken " + (endTime - startTime) + "ms");

        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }

    }
    public static void performSearch(IndexSearcher searcher, PrintWriter writer, String queryNumber, Query query) throws IOException {
        TopDocs results = searcher.search(query, 50);
        ScoreDoc[] hits = results.scoreDocs;

        for(int i=0;i<hits.length;i++){
            Document doc = searcher.doc(hits[i].doc);

            Integer x = i+1;
            writer.println(queryNumber + " Q0 " + doc.get("medline_ui") + " " + x + " " + hits[i].score + " TF");
        }
    }
    public static JSONArray parseInputJsonFile(String inputJsonFilePath){
        JSONParser parser = new JSONParser();
        JSONArray arrayObjects = null;
        try {
            arrayObjects = (JSONArray) parser.parse(new FileReader(inputJsonFilePath));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return arrayObjects;
    }

}
