package com.ontotext.kim.model;

import gate.Document;

import java.util.Map;

/** This interface allows for an external implementation to feed Entity data
 * into the KIMGazetteer's Dictionary.
 * @author danko@sirma.bg
 *
 */
public interface DictionaryFeeder {
    //=====================================================================
    // Method allowing custom initialization of the implementation  
    //=====================================================================
    /** This method allows for the KIMGazetteer to pass custom specific
     * configuration data to the feeder implementation. This data will be
     * provided as additional configuration parameter for the KIMGazetteer
     * plug-in. The the gazetteer will pass it to the DictionaryFeeder
     * implementation at initialization time.
     * 
     * @param keyValuePairs - a map of key and value string couples. This
     * set of parameters will be interpreted only by the DictionaryFeeder
     * implementation.
     */
    void init(Map<String, String> keyValuePairs);
    
    //=====================================================================
    // Method serving the feeding of the complete fixed content Dictionary 
    //=====================================================================
    /** This method will be used by the KIMGazetteer to fill its main fixed
     * dictionary at start-up. If just blank implementation is provided
     * then no filling will be performed.
     * 
     * @param entListener - entry point to pass the data for the main fixed
     * dictionary
     */
    void feedAll(EntityListener entListener);

    //=====================================================================
    // Methods serving the feeding of the local temporary Dictionaries  
    //=====================================================================
    /** This method marks the start of the current document processing by
     * the KIMGazetteer. It allows the implementation to perform any document
     * specific preparations and also feed caching activities.
     *  
     * @param document - reference to the document which the gazetteer starts
     * processing.
     * @return <b>true</b> - if supports local dictionary data;
     * <b>false</b> - usage of local dictionaries is disabled;
     */
    boolean localFeedInit(Document document);

    /** This method will be called by the KIMGazetteer's parser. It will be
     * called after the current parsing step is done, but before a look-up
     * is performed in the local dictionary.
     * 
     * @param document - reference to the document being processed. This is
     * passed as a reference to the local feeding session in the feeder
     * implementation.
     * @param anlList - this list represent the current state of the parsing
     * mechanism. It should be used to reduce the amount of data that the
     * feeder will deliver 
     * @param entListener - entry point to pass the data for the local
     * dictionary
     */
    void localFeedNeeded(Document document, ANLList anlList, EntityListener entListener);

    /** This method marks the end of the current document processing by
     * the KIMGazetteer. It allows the implementation to perform any clean-up
     * activities.
     * 
     * @param document - reference to the processed document
     */
    void localFeedEnd(Document document);

    //=====================================================================
    // Auxiliary interfaces
    //=====================================================================
    /** This interface provides entry point for passing Entity data.
     * @author danko@sirma.bg
     *
     */
    public interface EntityListener {
        /** Passes the Entity data to the dictionary
         * 
         * @param instUri - the instance URI of the entity
         * @param classUri - URI of the semantical class of the Entity 
         * @param aliasLabel - one of the textual representations of the Entity
         */
        void addEntity(String instUri, String classUri, String aliasLabel);
    }

    
    /** This interface allows access to a list of Alpha-Numeric Lexemes 
     * @author danko@sirma.bg
     *
     */
    public interface ANLList {
        /** Retrieves the count of ANLs in the frame
         * 
         * @return ANL count
         */
        int getLexemeCount();
        /** Retrieves the i-th ANL from those covered by the frame.
         * The index is 0 based.
         * 
         * @param i - position within the ANL list.
         * It have to be between 0 and ( getLexemeCount()-1 )
         * @return i-th ANL string
         */
        public String getLexeme(int i);
    }
}
