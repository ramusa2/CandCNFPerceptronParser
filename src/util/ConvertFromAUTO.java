package util;

import illinoisParser.CCGbankReader;
import illinoisParser.Grammar;
import illinoisParser.Model;
import illinoisParser.Parse;
import illinoisParser.Sentence;
import illinoisParser.Util;
import illinoisParser.models.HWDepModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * A simple class for converting AUTO parses into dependencies or TeX
 */
public class ConvertFromAUTO {

	//private static final String AUTO_FILENAME = "data/auto/wsj02-21.auto";
	//private static final String OUT_FILENAME = "data/auto/wsj02-21";
	private static final String AUTO_FILENAME = "temp/wsj0.parsed.auto";
	private static final String OUT_FILENAME = "temp/wsj0.parsed";

	private static final int MAX_LENGTH = 40;

	private static final boolean IS_CHINESE = false;

	public static void main(String[] args) throws Exception {
		/*
    if (args.length == 0) {
      System.out.println("Dear user, please provide some additional information:");
     // System.out.println("java -jar hdpccg.jar edu.illinois.cs.nlp.hdpccg.utils.ConvertFromAUTO Config.txt");
      System.out.println("Params to set:");
      System.out.println("AUTOFileToConvert=<FileOfStrings>");
      System.out.println("ConvertAUTO=PARG/CONLL/TEX");
      return;
    }
    Configuration config = new Configuration(args);
    TAGSET.readTagMapping(Configuration.TAG_SET);

    InductionGrammar grammar = new InductionGrammar(config);
    ArgumentModel model = new ArgumentModel(grammar,config);
    InductionParser parser = new InductionParser(model, Action.B3Mod_B2TR_B0Else);

    String filename = Configuration.AUTOFileToConvert;
		 */
		Grammar grammar = new Grammar();
		Model model = new HWDepModel(grammar);
		Collection<Sentence> autoSens = CCGbankReader.getSentencesFromAutoFile(AUTO_FILENAME);
		BufferedWriter pargWriter = Util.TextFileWriter(OUT_FILENAME+".parg");
		BufferedWriter bracketWriter = Util.TextFileWriter(OUT_FILENAME+".bracketed.txt");
		BufferedWriter texWriter = Util.TextFileWriter(OUT_FILENAME+".tex");
		createTeXHeader(texWriter, IS_CHINESE);
		for(Sentence sen : autoSens) {
			if(sen.length() <= MAX_LENGTH
					&& !(sen.getCCGbankParse().equals("TOO_LONG") || sen.getCCGbankParse().equals("PARSE_FAILURE"))) {
				Parse parse = new Parse(sen.getCCGbankParse(), model, sen);
				pargWriter.write(parse.getPargString() + "\n");
				texWriter.write(parse.getLaTeXString(IS_CHINESE) + "\n");
				bracketWriter.write(parse.getTree().toBracketedString(sen, grammar)+"\n");
			}
		}
		closeTeXFile(texWriter, IS_CHINESE);
		pargWriter.close();
		texWriter.close();
		bracketWriter.close();
		Util.logln("Done converting " + AUTO_FILENAME);
	}

	static void createTeXHeader(BufferedWriter writer, boolean isChinese) throws IOException {
		writer.write("\\documentclass[11pt]{beamer}\n");
		writer.write("\\usetheme{default}\n");
		writer.write("\\usepackage{ccg}\n");
		writer.write("\\usepackage[utf8]{inputenc}\n");
		//writer.write("\\usepackage[utf8x]{inputenc}\n"); utf8x doesn't work?
		writer.write("\\usepackage[T1]{fontenc}\n");
		if (isChinese) {
			writer.write("\\usepackage{CJK}\n");
			writer.write("\\newcommand{\\chinese}{\\begin{CJK}{UTF8}{gbsn}}\n");
			writer.write("\\newcommand{\\stopchinese}{\\end{CJK}}\n");
		}
		writer.write("\\geometry{top=1mm, bottom=1mm, left=1mm, right=1mm}\n");
		writer.write("\\usepackage{adjustbox}\n");
		writer.write("\\begin{document}\n");
	}

	static void closeTeXFile(BufferedWriter writer, boolean isChinese) throws IOException {
		writer.write("\\end{document}");
	}
}
