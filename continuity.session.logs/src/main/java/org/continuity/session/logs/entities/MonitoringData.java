package org.continuity.session.logs.entities;

/**
 * @author Henning Schulz
 *
 */
public class MonitoringData {

	private String link;

	/**
	 * Gets {@link #link}.
	 *
	 * @return {@link #link}
	 */
	public String getLink() {
		return this.link;
	}

	/**
	 * Sets {@link #link}.
	 *
	 * @param link
	 *            New value for {@link #link}
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ link: \"" + link + "\" }";
	}

}
