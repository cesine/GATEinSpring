// This script builds a TF.IDF index for a particular annotation type in a
// corpus.  It is intended to demonstrate the use of the beforeCorpus and
// afterCorpus callbacks.  The terms are the strings covered by a particular
// annotation type in the documents.  The annotation type to use is provided
// through the scriptParams map.
//
// The index is stored as a feature named freqTable on the corpus, the value of
// the feature is a Map<String, Map<Integer, Double>> - each term is mapped to
// a Map from document positions in the corpus to the TF.IDF score for the term
// in the document.

// reset variables - this method is called before processing the first document
void beforeCorpus(c) {
  // list of maps (one for each doc) from term to frequency
  frequencies = [] 
  // sorted map from term to docs that contain it
  docMap = new TreeMap()
  // index of the current doc in the corpus
  docNum = 0
}

// start frequency list for this document
frequencies << [:]

// iterate over the requested annotations
inputAS[scriptParams.annotationType].each {
  def str = doc.stringFor(it)
  // increment term frequency for this term
  frequencies[docNum][str] =
    (frequencies[docNum][str] ?: 0) + 1
  
  // keep track of which documents this term appears in
  if(!docMap[str]) {
    docMap[str] = new LinkedHashSet()
  }
  docMap[str] << docNum
}

// normalize counts by doc length
def docLength = inputAS[scriptParams.annotationType].size()
frequencies[docNum].each { freq ->
  freq.value = ((double)freq.value) / docLength
}

// increment the counter for the next document
docNum++

// compute the IDFs and store the table as a corpus feature - this method is
// called after the last document has been processed.
void afterCorpus(c) {
  def tfIdf = [:]
  docMap.each { term, docsWithTerm ->
    def idf = Math.log((double)docNum / docsWithTerm.size())
    tfIdf[term] = [:]
    docsWithTerm.each { docId ->
      tfIdf[term][docId] = frequencies[docId][term] * idf
    }
  }
  c.features.freqTable = tfIdf
}

