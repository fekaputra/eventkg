package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikipedia.model.LinkedByCount;
import de.l3s.eventkg.source.wikipedia.model.LinksToCount;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class WikipediaLinkCountsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	private Set<LinksToCount> linksToCounts;
	private Set<LinkedByCount> linkedByCounts;

	private static final boolean WRITE_TO_FILES = false;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		WikipediaLinkCountsExtractor extr = new WikipediaLinkCountsExtractor(languages, allEventPagesDataSet);
		extr.run();
	}

	public WikipediaLinkCountsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("CurrentEventsRelationsExtraction", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract Wikipedia link counts between entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect links from and to event pages.");
		extractRelations();
	}

	private void extractRelations() {

		this.linksToCounts = new HashSet<LinksToCount>();
		this.linkedByCounts = new HashSet<LinkedByCount>();

		for (Language language : this.languages) {
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_COUNTS, language)) {
				processFileIterator(child, language);
			}
		}

		writeResults();
	}

	private void writeResults() {

		if (!WRITE_TO_FILES) {

			for (LinksToCount linkCount : this.linksToCounts) {
				DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
			}

			// for (LinkedByCount linkCount : this.linkedByCounts) {
			// DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
			// }
		} else {

			System.out.println("Write results: Link counts");
			PrintWriter writer = null;
			try {
				writer = FileLoader.getWriter(FileName.ALL_LINK_COUNTS);

				for (LinksToCount linkCount : this.linksToCounts) {

					DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());

					writer.write(linkCount.getEvent().getWikidataId());
					writer.write(Config.TAB);
					writer.write(linkCount.getEvent().getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(linkCount.getEntity().getWikidataId());
					writer.write(Config.TAB);
					writer.write(linkCount.getEntity().getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(String.valueOf(String.valueOf(linkCount.getCount())));
					writer.write(Config.TAB);
					writer.write(linkCount.getLanguage().getLanguageLowerCase());
					writer.write(Config.NL);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}

			System.out.println("Write results: Linked by counts");
			PrintWriter writer2 = null;
			try {
				writer2 = FileLoader.getWriter(FileName.ALL_LINKED_BY_COUNTS);

				for (LinkedByCount linkCount : this.linkedByCounts) {

					DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());

					writer2.write(linkCount.getEvent().getWikidataId());
					writer2.write(Config.TAB);
					writer2.write(linkCount.getEvent().getWikipediaLabelsString(this.languages));
					writer2.write(Config.TAB);
					writer2.write(linkCount.getEntity().getWikidataId());
					writer2.write(Config.TAB);
					writer2.write(linkCount.getEntity().getWikipediaLabelsString(this.languages));
					writer2.write(Config.TAB);
					writer2.write(String.valueOf(linkCount.getCount()));
					writer2.write(Config.TAB);
					writer2.write(linkCount.getLanguage().getLanguageLowerCase());
					writer2.write(Config.NL);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer2.close();
			}
		}
	}

	private void processFileIterator(File file, Language language) {

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				processLine(line, language, file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

	}

	private void processFile(File file, Language language) {

		System.out.println("Process file " + file.getName() + ".");

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				processLine(line, language, file);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println(this.linksToCounts.size());
		// System.out.println(this.linkedByCounts.size());
	}

	private void processLine(String line, Language language, File file) {
		String[] parts = line.split(Config.TAB);
		String pageTitle = parts[1].replaceAll(" ", "_");
		Entity pageEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, pageTitle);

		if (pageEntity != null) {

			for (int i = 2; i < parts.length; i++) {
				String linkedPageTitle = parts[i].split(" ")[0].replaceAll(" ", "_");
				Entity linkedEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
						linkedPageTitle);
				int count = 0;
				if (linkedEntity != null) {

					try {
						count = Integer.valueOf(parts[i].split(" ")[1]);
					} catch (Exception e) {
						System.out.println("Warning: Error in file " + file.getName() + " for " + pageTitle + ".");
						continue;
					}

					if (pageEntity.isEvent() || linkedEntity.isEvent()) {

						Entity pageEntity2 = pageEntity;
						Entity linkedEntity2 = linkedEntity;

						this.linksToCounts.add(new LinksToCount(pageEntity2, linkedEntity2, count, language, true));

						// this.linkedByCounts.add(
						// new LinkedByCount(linkedEntity2, pageEntity2,
						// count, language));
					} else {

						if (areConnectedViaRelation(pageEntity, linkedEntity)) {
							this.linksToCounts.add(new LinksToCount(pageEntity, linkedEntity, count, language, false));
						}

					}

				}
			}
		}
	}

	private boolean areConnectedViaRelation(Entity entity1, Entity entity2) {
		return DataStore.getInstance().getConnectedEntities().containsKey(entity1)
				&& DataStore.getInstance().getConnectedEntities().get(entity1).contains(entity2);
	}

}
