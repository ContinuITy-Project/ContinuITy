package org.continuity.jmeter.transform;

import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;

/**
 *
 * @author Henning Schulz
 *
 */
public class CookiesAnnotator {

	public void configureCookieManagement(ListedHashTree testPlan) {
		SearchByClass<CookieManager> search = new SearchByClass<>(CookieManager.class);
		testPlan.traverse(search);

		for (CookieManager cookieManager : search.getSearchResults()) {
			cookieManager.setClearEachIteration(true);
		}
	}

}
