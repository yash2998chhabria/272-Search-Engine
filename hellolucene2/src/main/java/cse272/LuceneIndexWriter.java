package cse272;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;

import org.apache.lucene.index.IndexOptions;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Path;

public class LuceneIndexWriter {

    private static final String String = null;
    IndexWriter indexWriter = null;
    private String inputJsonFilePath;
    private static String outputLuceneIndexPath;

    //Constructor to initialize the file path
    public LuceneIndexWriter(String outputLuceneIndexPath, String inputJsonFilePath) {
        this.outputLuceneIndexPath = outputLuceneIndexPath;
        this.inputJsonFilePath = inputJsonFilePath;
    }

    //Create the Lucene Index
    public  void createLuceneIndex() throws IOException {

        JSONArray jsonObjects = parseInputJsonFile();

        openLuceneIndex();
        addLuceneDocuments(jsonObjects);

        wrapupAndFinish();

    }

    public JSONArray parseInputJsonFile(){

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

    public boolean openLuceneIndex(){
        try {

            Path indexPath = Paths.get(outputLuceneIndexPath);
            Directory dir = FSDirectory.open(indexPath);
            Analyzer analyzer = new StandardAnalyzer();

            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

            System.out.println(indexPath);

            indexWriterConfig.setOpenMode(OpenMode.CREATE);
            indexWriter = new IndexWriter(dir, indexWriterConfig);

            return true;

        } catch (Exception e) {
            System.err.println("Error - Opening the index. " + e.getMessage());

        }
        return false;

    }

    //Add to Lucene documents
    public void addLuceneDocuments(JSONArray jsonObjects){


        FieldType textIndexedTypeTrue = new FieldType();
        textIndexedTypeTrue.setStored(true);
        textIndexedTypeTrue.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

        for (Object O: jsonObjects) {

            Document document = new Document();

            JSONObject news = (JSONObject) O;

            String seq_id = "";
            if (news.get("seq_id") != null) {
                seq_id = (String)news.get("seq_id");
            }

            String medline_ui = "";
            if (news.get("medline_ui") != null) {
                medline_ui = (String)news.get("medline_ui");
            }

            String mesh_terms = "";
            if (news.get("mesh_terms") != null) {
                mesh_terms = (String)news.get("mesh_terms");
            }

            String title = "";
            if (news.get("title") != null) {
                title = (String)news.get("title");
            }

            String publication_type = "";
            if (news.get("publication_type") != null) {
                publication_type = (String)news.get("publication_type");
            }

    	    String abstract_of_pub = "";
    	    if (news.get("abstract") != null) {
                abstract_of_pub = (String)news.get("abstract");
    	    }

            String author = "";
            if (news.get("author") != null) {
                author = (String)news.get("author");
            }


            String source = "";
            if (news.get("source") != null) {
                source = (String)news.get("source");
            }

            FieldType ft = new FieldType();
            ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
            ft.setTokenized(true);
            ft.setStored(true);
            ft.setStoreTermVectors(true);  //Store Term Vectors
            ft.freeze();
            StoredField abstract_field = new StoredField("abstract",abstract_of_pub,ft);
            document.add(abstract_field);
            StoredField mesh_terms_field = new StoredField("mesh_terms",mesh_terms,ft);
            document.add(mesh_terms_field);
            StoredField title_field = new StoredField("title",title,ft);
            document.add(title_field);



            document.add(new StringField("seq_id",seq_id,Field.Store.YES));
            document.add(new StringField("medline_ui",medline_ui,Field.Store.YES));
//            document.add(new TextField("mesh_terms",mesh_terms,Field.Store.YES));
//            document.add(new TextField("title",title,Field.Store.YES));


            document.add(new StringField("publication_type",publication_type,Field.Store.YES));
//            document.add(new TextField("abstract",abstract_of_pub,Field.Store.YES,Field.TermVector.YES));

            document.add(new StringField("author",author,Field.Store.YES));
            document.add(new StringField("source",source,Field.Store.YES));


            try {
                indexWriter.addDocument(document);
            } catch (IOException ex) {
                System.err.println("Error adding documents to the index. " +  ex.getMessage());
            }
        }

    }

    public void wrapupAndFinish(){
        try {
            indexWriter.commit();
            indexWriter.close();
        } catch (IOException ex) {
            System.err.println("We had a problem closing the index: " + ex.getMessage());
        }
    }

    public static void cleanupIndexFolder(File outputLuceneIndexFolder)
    {
        final File[] indexFolder = outputLuceneIndexFolder.listFiles();
        if (indexFolder != null)
        {
            for (File f: indexFolder) f.delete();
        }

    }


    public static void main(String[] args) throws IOException
    {
        //Folder to create index files
        String outputLuceneIndexPath = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/Index";
        //Folder containing input JSON files
        String inputJsonFilePath = "/Users/yashchhabria/Mini Projects/cse272/272-Search-Engine/data/json_files";

        //Index folder Clean up before commencing the process
        File outputLuceneIndexFolder = new File(outputLuceneIndexPath);
        cleanupIndexFolder(outputLuceneIndexFolder);

        File[] files = new File(inputJsonFilePath).listFiles();

        //Start time for Indexing
        long startTime = System.currentTimeMillis();

        int Filescount = new File(inputJsonFilePath).listFiles().length;

        for (File file : files)
        {
            if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead() )
            {
                System.out.println(file.getAbsolutePath());
                String filePath = file.getAbsolutePath();
                LuceneIndexWriter lw = new LuceneIndexWriter(outputLuceneIndexPath,filePath);
                lw.createLuceneIndex();

            }
        }

        long endTime = System.currentTimeMillis();

        System.out.println("");
        System.out.println("************************************************************************");
        //Total time taken to index the JSON files
        System.out.println(Filescount + " Input files have been indexed: Total time taken: " +(endTime-startTime)+" ms");
        System.out.println("************************************************************************");
        System.out.println("");


    }

}