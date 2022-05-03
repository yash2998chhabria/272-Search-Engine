package cse272;

import org.apache.lucene.search.similarities.TFIDFSimilarity;

public class TermSimilarity extends TFIDFSimilarity {

    public TermSimilarity() {}

    //Higher frequency terms (more matches) scored higher
    @Override
    public float tf(float freq) {
        return freq;
    }

    //Weigh matches on rarer terms more heavily.
    @Override
    public float idf(long docFreq, long numDocs) {
        return 1f;
    }

    @Override
    public float lengthNorm(int i) {
        return 1f;
    }

    @Override
    public String toString() {
        return "SimpleSimilarity";
    }
}