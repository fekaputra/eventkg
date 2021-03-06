package de.l3s.eventkg.source.wikidata;

public enum WikidataResource {

	WIKINEWS_ARTICLE("Q17633526"),
	SCIENTIFIC_ARTICLE("Q13442814"),
	OCCURRENCE("Q1190554"),
	EVENT("Q1656682"),
	DETERMINATOR_FOR_DATE_OF_PERIODIC_OCCURRENCE("Q14795564"),
	HUMAN("Q5"),
	FICTIONAL_HUMAN("Q15632617"),
	WIKIMEDIA_INTERNAL_STUFF("Q17442446");

	private String id;

	WikidataResource(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
