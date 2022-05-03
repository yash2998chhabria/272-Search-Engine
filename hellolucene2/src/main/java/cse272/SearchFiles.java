package cse272;


import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.lucene.analysis.en.EnglishAnalyzer;



public class SearchFiles {

    private SearchFiles() {}

    public static void main(String[] args) throws Exception {

        //--------- Create reader, writer & searcher ----------

        String index = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/Index";
        String results_path = "my-results.txt";
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        PrintWriter writer = new PrintWriter(results_path, "UTF-8");
        IndexSearcher searcher = new IndexSearcher(reader);

        //---------------- Choose Analyzer ----------------

        //WhitespaceAnalyzer – Splits tokens at whitespace
        //Analyzer analyzer = new WhitespaceAnalyzer();

        //SimpleAnalyzer – Divides text at non letter characters and lowercases
        //Analyzer analyzer = new SimpleAnalyzer();

        //StopAnalyzer – Divides text at non letter characters, lowercases, and removes stop words
        //Analyzer analyzer = new StopAnalyzer();

        //StandardAnalyzer - Tokenizes based on sophisticated grammar that recognizes e-mail addresses, acronyms, etc.; lowercases and removes stop words (optional)
        Analyzer analyzer = new StandardAnalyzer();

        //EnglishAnalyzer - Analyzer with enhancements for stemming English words
        //Analyzer analyzer = new EnglishAnalyzer();

        //CustomAnalyzer - Defined in CustomAnalyzer.java
//        Analyzer analyzer = new CustomAnalyzer();

        //---------------- Choose Scoring method ----------------

        //Vector Space Model
        //searcher.setSimilarity(new ClassicSimilarity());

        //BM25
        searcher.setSimilarity(new ClassicSimilarity());

        //---------------- Read in and parse queries ----------------

        String inputJsonFilePath = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/data/querydata.json";

        JSONArray jsonObjects = parseInputJsonFile(inputJsonFilePath);


//        BufferedReader buffer = Files.newBufferedReader(Paths.get(queriesPath), StandardCharsets.UTF_8);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] {"title","abstract","mesh_terms"}, analyzer);

        String queryString = "";
        String queryNumber = "";

        for (Object O: jsonObjects) {
            JSONObject doc = (JSONObject) O;
            queryString = (String)doc.get("title") + " " + (String)doc.get("description");
            queryNumber = (String)doc.get("query_number");
            Query query = parser.parse(QueryParser.escape(queryString));
            performSearch(searcher,writer,queryNumber,query);
        }

        System.out.println("Reading in queries and creating search results.");




//        Query query = parser.parse(QueryParser.escape(queryString));
//        performSearch(searcher,writer,queryNumber,query);

        writer.close();
        reader.close();
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
    // Performs search and writes results to the writer
    public static void performSearch(IndexSearcher searcher, PrintWriter writer, String queryNumber, Query query) throws IOException {
        TopDocs results = searcher.search(query, 50);
        ScoreDoc[] hits = results.scoreDocs;

        String index = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/Index";
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        CharArraySet enStopSet = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
        Map<String,Integer> totalTfv = new HashMap<String,Integer>(1024);



        // Write the results for each hit
        for(int i=0;i<hits.length;i++) {
            Document doc = searcher.doc(hits[i].doc);
            List<String> fields = Arrays.asList("abstract", "title", "mesh_terms");
            for (String field : fields) {
                Terms termVector = reader.getTermVector(hits[i].doc, field);
                TermsEnum itr = termVector.iterator();
                BytesRef term = null;
                PostingsEnum postings = null;
                while ((term = itr.next()) != null) {

                    try {
                        String termText = term.utf8ToString();
                        postings = itr.postings(postings, PostingsEnum.FREQS);
                        postings.nextDoc();
                        int freq = postings.freq();

                        if (termText.length() < 2 ||
                                enStopSet.contains(termText)) {
                            continue;
                        }
                        Integer totalFreq = totalTfv.get(termText);
                        totalFreq = (totalFreq == null) ? freq : freq + totalFreq;
                        totalTfv.put(termText, totalFreq);

//                        System.out.println("doc:" + i + ", term: " + termText + ", termFreq = " + freq);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }

        }
            LinkedHashMap<String, Integer> totalTfv_ordered = new LinkedHashMap<>();

            totalTfv.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEachOrdered(x -> totalTfv_ordered.put(x.getKey(), x.getValue()));

//            System.out.println("Reverse Sorted Map   : " + totalTfv_ordered);

            Integer Count = 0;
            String query_string = "";
            for (String key : totalTfv_ordered.keySet()){
                Count++;

                if (Count > 30) {
                    break;
                }
                query_string = query_string + " " + key;
            }

//            System.out.println(query_string);

            Analyzer analyzer = new StandardAnalyzer();
            MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] {"title","abstract","mesh_terms"}, analyzer);
            Query query_relevance = null;
            try {
                query_relevance = parser.parse(QueryParser.escape(query_string));
            } catch (org.apache.lucene.queryparser.classic.ParseException e) {
                throw new RuntimeException(e);
            }
            TopDocs results_relevance = searcher.search(query_relevance, 50);
            ScoreDoc[] hits_relevance = results_relevance.scoreDocs;


            for(int j=0;j<hits_relevance.length;j++){
                Document doc_relevance = searcher.doc(hits[j].doc);

                writer.println(queryNumber + " 0 " + doc_relevance.get("medline_ui") + " " + j + " " + hits[j].score + " EXP");
            }







    }


}