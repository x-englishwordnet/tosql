/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.sql.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class Senses
{
	private Senses()
	{
	}

	public static Map<String, Integer> generateSenses(final PrintStream ps, final Map<String, Sense> sensesById, final Map<String, Integer> synsetIdToNIDMap, final Map<Lex, Integer> lexToNIDMap, final Map<String, Integer> wordIdToNIDMap, final Map<String, Integer> casedWordIdToNIDMap)
	{
		// stream of sensekeys
		Stream<String> senseKeyStream = sensesById.keySet()    //
				.stream() //
				.sorted();

		// make sensekey-to-nid map
		Map<String, Integer> sensekeyToNID = Utils.makeMap(senseKeyStream);

		// insert map
		final String columns = String.join(",", Names.SENSES.senseid, Names.SENSES.sensekey, Names.SENSES.synsetid, Names.SENSES.luid, Names.SENSES.wordid, Names.SENSES.casedwordid, Names.SENSES.lexid, Names.SENSES.sensenum, Names.SENSES.tagcount);
		final Function<Sense, String> toString = sense -> {

			Lex lex = sense.getLex();
			String casedWord = lex.getLemma();
			String word = casedWord.toLowerCase(Locale.ENGLISH);
			String synsetId = sense.getSynsetId();
			String sensekey = sense.getSensekey();
			int lexid = sense.findLexid();
			TagCount tagCount = sense.getTagCount();
			int wordNID = NIDMaps.lookup(wordIdToNIDMap, word);
			int synsetNID = NIDMaps.lookup(synsetIdToNIDMap, synsetId);
			int lexNID = NIDMaps.lookup(lexToNIDMap, lex);
			String casedWordNID = NIDMaps.lookupNullable(casedWordIdToNIDMap, casedWord);
			String tagCnt = tagCount == null ? "NULL" : Integer.toString(tagCount.getCount());
			String senseNum = tagCount == null ? "NULL" : Integer.toString(tagCount.getSenseNum());
			return String.format("'%s',%d,%d,%d,%s,%d,%s,%s", Utils.escape(sensekey), synsetNID, lexNID, wordNID, casedWordNID, lexid, senseNum, tagCnt);
		};
		if (!Printers.withComment)
		{
			Printers.printInsert(ps, Names.SENSES.TABLE, columns, sensesById, sensekeyToNID, toString);
		}
		else
		{
			final Function<Sense, String[]> toStringWithComment = sense -> {

				Lex lex = sense.getLex();
				String casedWord = lex.getLemma();
				String synsetId = sense.getSynsetId();
				String sensekey = sense.getSensekey();
				return new String[]{ //
						toString.apply(sense), //
						String.format("%s %s '%s'", sensekey, synsetId, casedWord),};
			};
			Printers.printInsertWithComment(ps, Names.SENSES.TABLE, columns, sensesById, sensekeyToNID, toStringWithComment);
		}
		return sensekeyToNID;
	}

	public static void generateSenseRelations(final PrintStream ps, final Map<String, Sense> sensesById, final Map<String, Integer> synsetIdToNIDMap, final Map<Lex, Integer> lexToNIDMap, final Map<String, Integer> wordIdToNIDMap)
	{
		// stream of senses
		Stream<Sense> senseStream = sensesById.values() //
				.stream() //
				.filter(s -> {
					var relations = s.getRelations();
					return relations != null && relations.size() > 0;
				});

		// insert map
		final String columns = String.join(",", Names.SENSES_SENSES.synset1id, Names.SENSES_SENSES.lu1id, Names.SENSES_SENSES.word1id, Names.SENSES_SENSES.synset2id, Names.SENSES_SENSES.lu2id, Names.SENSES_SENSES.word2id, Names.SENSES_SENSES.relationid);
		Function<Sense, List<String>> toString = (sense) -> {

			var strings = new ArrayList<String>();
			String synsetId1 = sense.getSynsetId();
			Lex lex1 = sense.getLex();
			String casedword1 = lex1.getLemma();
			String word1 = casedword1.toLowerCase(Locale.ENGLISH);
			int lu1NID = NIDMaps.lookup(lexToNIDMap, lex1);
			int wordNID1 = NIDMaps.lookup(wordIdToNIDMap, word1);
			int synsetNID1 = NIDMaps.lookup(synsetIdToNIDMap, synsetId1);
			var relations = sense.getRelations();
			for (String relation : relations.keySet())
			{
				if (!BuiltIn.OEWN_RELATIONTYPES.containsKey(relation))
				{
					throw new IllegalArgumentException(relation);
				}
				int relationId = BuiltIn.OEWN_RELATIONTYPES.get(relation);
				for (String senseId2 : relations.get(relation))
				{
					Sense sense2 = sensesById.get(senseId2);
					String synsetId2 = sense2.getSynsetId();
					Lex lex2 = sense2.getLex();
					String casedword2 = lex2.getLemma();
					String word2 = casedword2.toLowerCase(Locale.ENGLISH);

					int lu2NID = NIDMaps.lookup(lexToNIDMap, lex2);
					int wordNID2 = NIDMaps.lookup(wordIdToNIDMap, word2);
					int synsetNID2 = NIDMaps.lookup(synsetIdToNIDMap, synsetId2);
					strings.add(String.format("%d,%d,%d,%d,%d,%d,%d", synsetNID1, lu1NID, wordNID1, synsetNID2, lu2NID, wordNID2, relationId));
				}
			}
			return strings;
		};
		if (!Printers.withComment)
		{
			Printers.printInserts(ps, Names.SENSES_SENSES.TABLE, columns, senseStream, toString, false);
		}
		else
		{
			Function<Sense, List<String[]>> toStrings = (sense) -> {

				var strings = toString.apply(sense);
				var stringWithComments = new ArrayList<String[]>();
				String synsetId1 = sense.getSynsetId();
				Lex lex1 = sense.getLex();
				String casedword1 = lex1.getLemma();
				var relations = sense.getRelations();
				int i = 0;
				for (String relation : relations.keySet())
				{
					for (String senseId2 : relations.get(relation))
					{
						Sense sense2 = sensesById.get(senseId2);
						String synsetId2 = sense2.getSynsetId();
						Lex lex2 = sense2.getLex();
						String casedword2 = lex2.getLemma();
						stringWithComments.add(new String[]{ //
								strings.get(i), //
								String.format("%s %s -%s-> %s %s", synsetId1, casedword1, relation, synsetId2, casedword2), //
						});
						i++;
					}
				}
				return stringWithComments;
			};
			Printers.printInsertsWithComment(ps, Names.SENSES_SENSES.TABLE, columns, senseStream, toStrings, false);

		}
	}

	public static void generateAdjPositions(final PrintStream ps, final Map<String, Sense> sensesById, final Map<String, Integer> synsetIdToNIDMap, final Map<Lex, Integer> lexToNIDMap, final Map<String, Integer> wordIdToNIDMap)
	{
		// stream of senses
		Stream<Sense> senseStream = sensesById.values() //
				.stream() //
				.filter(s -> {
					var adjPosition = s.getAdjPosition();
					return adjPosition != null;
				});

		// insert map
		final String columns = String.join(",", Names.SENSES_ADJPOSITIONS.synsetid, Names.SENSES_ADJPOSITIONS.luid, Names.SENSES_ADJPOSITIONS.wordid, Names.SENSES_ADJPOSITIONS.positionid);
		Function<Sense, String> toString = (sense) -> {

			String synsetId = sense.getSynsetId();
			Lex lex = sense.getLex();
			String casedword = lex.getLemma();
			String word = casedword.toLowerCase(Locale.ENGLISH);
			int synsetNID = NIDMaps.lookup(synsetIdToNIDMap, synsetId);
			int luNID = NIDMaps.lookup(lexToNIDMap, lex);
			int wordNID = NIDMaps.lookup(wordIdToNIDMap, word);
			return String.format("%d,%d,%d,'%s'", synsetNID, luNID, wordNID, sense.getAdjPosition());
		};
		Printers.printInsert(ps, Names.SENSES_ADJPOSITIONS.TABLE, columns, senseStream, toString, false);
	}

	public static void generateVerbFrames(final PrintStream ps, final Map<String, Sense> sensesById, final Map<String, Integer> synsetIdToNIDMap, final Map<Lex, Integer> lexToNIDMap, final Map<String, Integer> wordIdToNIDMap)
	{
		// stream of senses
		Stream<Sense> senseStream = sensesById.values() //
				.stream() //
				.filter(s -> {
					var frames = s.getVerbFrames();
					return frames != null && frames.length > 0;
				});

		// insert map
		final String columns = String.join(",", Names.SENSES_VFRAMES.synsetid, Names.SENSES_VFRAMES.luid, Names.SENSES_VFRAMES.wordid, Names.SENSES_VFRAMES.frameid);
		Function<Sense, List<String>> toString = (sense) -> {

			var strings = new ArrayList<String>();
			String synsetId = sense.getSynsetId();
			String wordId = sense.getWordId().toLowerCase(Locale.ENGLISH);
			int synsetNID = NIDMaps.lookup(synsetIdToNIDMap, synsetId);
			int wordNID = NIDMaps.lookup(wordIdToNIDMap, wordId);
			Lex lex = sense.getLex();
			int luNID = NIDMaps.lookup(lexToNIDMap, lex);

			for (var frameId : sense.getVerbFrames())
			{
				int frameNID = BuiltIn.VERBFRAMEID2NIDS.get(frameId);
				strings.add(String.format("%d,%d,%d,%d", synsetNID, luNID, wordNID, frameNID));
			}
			return strings;
		};
		Printers.printInserts(ps, Names.SENSES_VFRAMES.TABLE, columns, senseStream, toString, false);
	}

	public static void generateVerbTemplates(final PrintStream ps, final Map<String, Sense> sensesById, final Map<String, Integer> synsetIdToNIDMap, final Map<Lex, Integer> lexToNIDMap, final Map<String, Integer> wordIdToNIDMap)
	{
		// stream of senses
		Stream<Sense> senseStream = sensesById.values() //
				.stream() //
				.filter(s -> {
					var templates = s.getVerbTemplates();
					return templates != null && templates.length > 0;
				});

		// insert map
		final String columns = String.join(",", Names.SENSES_VTEMPLATES.synsetid, Names.SENSES_VTEMPLATES.luid, Names.SENSES_VTEMPLATES.wordid, Names.SENSES_VTEMPLATES.templateid);
		Function<Sense, List<String>> toString = (sense) -> {

			var strings = new ArrayList<String>();
			String synsetId = sense.getSynsetId();
			String wordId = sense.getWordId().toLowerCase(Locale.ENGLISH);
			int synsetNID = NIDMaps.lookup(synsetIdToNIDMap, synsetId);
			int wordNID = NIDMaps.lookup(wordIdToNIDMap, wordId);
			Lex lex = sense.getLex();
			int luNID = NIDMaps.lookup(lexToNIDMap, lex);

			for (var templateId : sense.getVerbTemplates())
			{
				strings.add(String.format("%d,%d,%d,%d", synsetNID, luNID, wordNID, templateId));
			}
			return strings;
		};
		Printers.printInserts(ps, Names.SENSES_VTEMPLATES.TABLE, columns, senseStream, toString, false);
	}
}
