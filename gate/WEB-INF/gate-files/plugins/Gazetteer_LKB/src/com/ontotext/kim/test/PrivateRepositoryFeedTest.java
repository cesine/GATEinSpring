package com.ontotext.kim.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.ontotext.kim.client.query.KIMQueryException;
import com.ontotext.kim.gate.SettingsHashBuilder;
import com.ontotext.kim.query.QueryResultCounter;
import com.ontotext.kim.util.datastore.PrivateRepositoryFeed;

public class PrivateRepositoryFeedTest extends TestCase {

	public void testWithLLD() throws IOException, KIMQueryException {
		URL configReader = this.getClass().getClassLoader().getResource("config.ttl");
		String query = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("query.txt"));
		int settingsHash = new SettingsHashBuilder().getHash(configReader, query);
		PrivateRepositoryFeed feed = new PrivateRepositoryFeed(configReader, query, settingsHash);
		QueryResultCounter counter = new QueryResultCounter();
		feed.feedTo(counter);
		assertEquals(100, counter.getCount());
	}
	
	public void testWithLocal() throws IOException, KIMQueryException {
		File configFile = new File("target/lkb_gazetteer/samples/dictionary_from_local_ontology/config.ttl").getCanonicalFile();
		URL configReader = configFile.toURI().toURL();
		String query = FileUtils.readFileToString(new File(configFile.getParentFile(), "query.txt"));
		int settingsHash = new SettingsHashBuilder().getHash(configReader, query);
		PrivateRepositoryFeed feed = new PrivateRepositoryFeed(configReader, query, settingsHash);
		QueryResultCounter counter = new QueryResultCounter();
		feed.feedTo(counter);
		assertEquals(79, counter.getCount());
	}	
}
