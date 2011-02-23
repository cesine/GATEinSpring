package com.ontotext.kim.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openrdf.query.QueryLanguage;

import com.ontotext.kim.KIMConstants;
import com.ontotext.kim.client.GetService;
import com.ontotext.kim.client.KIMRuntimeException;
import com.ontotext.kim.client.KIMService;
import com.ontotext.kim.client.query.KIMQueryException;
import com.ontotext.kim.client.semanticrepository.QueryResultListener;
import com.ontotext.kim.client.semanticrepository.SemanticRepositoryAPI;
import com.ontotext.kim.client.semanticrepository.QueryResultListener.Feed;
import com.ontotext.kim.gate.SettingsHashBuilder;
import com.ontotext.kim.util.datastore.PrivateRepositoryFeed;

/**
 * 
 * @author mnozchev
 */
public class DataFeedFactory {

	private static Logger log = Logger.getLogger(DataFeedFactory.class);

	/**
	 * The DummyFeed allows to return a valid feed even there's no configuration for one.
	 * That way, the dictionary can be initialized successfully if the feed is not 
	 * required because there is a cache file already.
	 * 
	 * @author mnozchev
	 */
	private static class DummyFeed implements Feed {

		private final File dictionaryPath;

		public DummyFeed(File dictionaryPath) {
			this.dictionaryPath = dictionaryPath;
		}

		public void feedTo(QueryResultListener listener) throws KIMQueryException {
			String configPath = new File(dictionaryPath, "config.ttl").getAbsolutePath();
			throw new KIMQueryException("Could not find a valid configuration file. Please check if " + configPath + " exists.");			
		}

	}
	public Feed createFeed(File dictionaryPath) {
		final KIMService kimSvc = GetService.getKIMService();
		Feed result = null;
		if (kimSvc != null)
			result = createFeed(kimSvc);

		if (result == null) {
			result = createSesameFeed(dictionaryPath);
		}

		if (result == null) {
			result = new DummyFeed(dictionaryPath);
		}
		return result;
	}

	private QueryResultListener.Feed createSesameFeed(File dictionaryPath) {
		File queryFile = new File(dictionaryPath, "query.txt").getAbsoluteFile();
		try {			
			URL configFileUrl = new File(dictionaryPath, "config.ttl").getAbsoluteFile().toURI().toURL();				
			String queryString = FileUtils.readFileToString(queryFile);
			log.info("Query loaded from " + queryFile);
			int settingsHash = new SettingsHashBuilder().getHash(configFileUrl, queryString);
			return new PrivateRepositoryFeed(configFileUrl, queryString, settingsHash);
		} 
		catch (IOException e) {
			log.warn("Error while reading " + queryFile.getAbsolutePath(), e);				
		}	
		return null;
	}

	private QueryResultListener.Feed createFeed(final KIMService kimSvc) {

		SemanticRepositoryAPI semRep;

		try {
			semRep = kimSvc.getSemanticRepositoryAPI();
		} catch (RemoteException e) {						
			log.info("Semantic repository is not available.: " + e.getMessage());
			return null;
		}

		String query;
		try {
			query = LabelsModel.get().getAllTrustedEntitiesSeRQL() + " UNION " + KIMConstants.QUERY_LEX_RES_VS_DIRECT_TYPE;
		}
		catch (KIMRuntimeException e) { // in case the labels model hasn't been initialized yet
			query = KIMConstants.QUERY_LEX_RES_VS_DIRECT_TYPE;
		}
		return new KIMDataFeed(semRep, QueryLanguage.SERQL.getName(), query);
	}	
}
