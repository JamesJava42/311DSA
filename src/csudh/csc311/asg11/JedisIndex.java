package csudh.csc311.asg11;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class JedisIndex {

	private Jedis jedis;

	/**
	 * Constructor.
	 * * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
	}

	/**
	 * Returns the Redis key for a given search term.
	 * Format: "URLSet:term"
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}

	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * Format: "TermCounter:url"
	 */
	private String termCounterKey(String url) {
		return "TermCounter:" + url;
	}

	/**
	 * Adds a Web page to the index.
	 *
	 * @param url
	 * @param paragraphs
	 */
	public void indexPage(String url, Elements paragraphs) {
		System.out.println("Indexing " + url);
		
		// 1. Create a local TermCounter to count words in the paragraphs
		// (This uses the TermCounter.java file you already have)
		TermCounter tc = new TermCounter(url);
		tc.processElements(paragraphs);

		// 2. Start a Redis Transaction
		// We use a transaction so all updates happen at once (faster and safer)
		Transaction t = jedis.multi();

		// 3. Delete the old TermCounter for this URL if it exists
		// The assignment says: "the new results should replace the old ones"
		String hashKey = termCounterKey(url);
		t.del(hashKey);

		// 4. Iterate over every word found on the page
		for (String term : tc.keySet()) {
			Integer count = tc.get(term);

			// A. Update the Redis Hash (TermCounter)
			// Key: "TermCounter:http://..." Field: "the" Value: "339"
			t.hset(hashKey, term, count.toString());

			// B. Update the Redis Set (URLSet)
			// Key: "URLSet:the" Member: "http://..."
			// This adds the current URL to the set of pages that contain "term"
			t.sadd(urlSetKey(term), url);
		}

		// 5. Execute the transaction
		t.exec();
	}

	/**
	 * Returns the number of times the given term appears at the given URL.
	 * * @return
	 */
	public Integer getCount(String url, String term) {
		// Look up the count in the Redis Hash
		String hashKey = termCounterKey(url);
		String count = jedis.hget(hashKey, term);
		
		if (count == null) {
			return 0;
		} else {
			return Integer.parseInt(count);
		}
	}

	/**
	 * Returns a map from URL to the number of times the term appears.
	 * * @param term
	 * @return
	 */
	public Map<String, Integer> getCounts(String term) {
		Map<String, Integer> map = new HashMap<String, Integer>();

		// 1. Find the Set of all URLs that contain this term
		// Key: "URLSet:term"
		Set<String> urls = jedis.smembers(urlSetKey(term));

		// 2. Iterate over those URLs and get the specific count for each
		for (String url : urls) {
			Integer count = getCount(url, term);
			map.put(url, count);
		}

		return map;
	}
	
	/**
	 * Removes all data from the database.
	 * Useful for testing.
	 */
	public void deleteTermCounters() {
		jedis.flushDB();
	}


	public static void main(String[] args) throws java.io.IOException {
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);
        
        // Clear database so we start fresh
        index.deleteTermCounters(); 

        String source1 = "https://raw.githubusercontent.com/behollister/fixed-wiki-pages/main/Java_(programming_language)";
        String source2 = "https://raw.githubusercontent.com/behollister/fixed-wiki-pages/main/Programming_language";

        WikiFetcher wf = new WikiFetcher();
        
        // Index first page
        System.out.println("Indexing " + source1);
        Elements paragraphs1 = wf.fetchWikipedia(source1);
        index.indexPage(source1, paragraphs1);

        // Index second page
        System.out.println("Indexing " + source2);
        Elements paragraphs2 = wf.fetchWikipedia(source2);
        index.indexPage(source2, paragraphs2);

        // Search for the word "the" and print results
        Map<String, Integer> map = index.getCounts("the");
        
        System.out.println("Results for 'the':");
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
        } else {
            System.out.println("Map is null! Check getCounts method.");
        }
    }
}
