package ccgparser.util;

import illinoisParser.Grammar;
import illinoisParser.variables.ConditioningVariables;
import illinoisParser.variables.VariablesFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class OfficialCandCFeature {

	private char templateID;

	private int trainingFrequency;

	private String firstHead;

	private String secondHead;

	private String A;

	private String B;

	private String C;

	private Integer distance;

	String desc = "";

	private OfficialCandCFeature() {
		this.templateID = '#';
		this.trainingFrequency = -999;
		this.firstHead = null;
		this.secondHead = null;
		this.A = null;
		this.B = null;
		this.C = null;
		this.distance = -999;
	}

	private static OfficialCandCFeature getFeatureFromStringDesc(String description) {
		OfficialCandCFeature feature = new OfficialCandCFeature();
		String[] tokens = description.split("\\s+");
		feature.trainingFrequency = Integer.parseInt(tokens[0]);
		feature.templateID = tokens[1].charAt(0);
		feature.setFields(tokens);
		//TODO: delete description field from this class
		feature.desc = description;


		return feature;
	}

	private void setFields(String[] tokens) {
		switch(this.templateID) {

		/** Shared feature templates **/
		// RootCat/LexCat (+ Word/POS)
		case 'a': 
		case 'd':
		case 'b':
		case 'e':
			this.firstHead = tokens[3];
		case 'c':
			this.A = tokens[2];
			break;

			// Rule (Unary)tokens
		case 'p':
		case 'r':
			this.firstHead = tokens[5];
		case 'm':
			this.B = tokens[2];
			this.A = tokens[3];
			break;

			// Rule (Binary)
		case 'q':
		case 's':
			this.firstHead = tokens[5];
		case 'n':
			this.B = tokens[2];
			this.C = tokens[3];
			this.A = tokens[4];
			break;


			/** Normal-form model features **/
			// 
		case 't':
		case 'u':
		case 'v':
		case 'w':
			this.B = tokens[2];
			this.C = tokens[3];
			this.A = tokens[4];
			this.firstHead = tokens[5];
			this.secondHead = tokens[6];
			break;

		case 'F':
		case 'G':
		case 'H':
		case 'I':
		case 'J':
		case 'K':
			this.B = tokens[2];
			this.C = tokens[3];
			this.A = tokens[4];
			this.firstHead = tokens[5];
			this.distance = Integer.parseInt(tokens[6]);
			break;

			/** Dependency model features **/
			// TODO: add dependency model feature template conversions (after implementing dependency model)
		case 'f':
			//return ;
		case 'g':
			//return ;
		case 'h':
			//return ;
		case 'i':
			//return ;
		case 'L':
			//return ;
		case 'M':
			//return ;
		case 'N':
			//return ;
		case 'P':
			//return ;
		case 'Q':
			//return ;
		case 'R':
			//return ;

			/** Unrecognized feature template **/
		default:
			break;
		}
		// Clean categories
		this.A = cleanCategory(this.A);
		this.B = cleanCategory(this.B);
		this.C = cleanCategory(this.C);

		// Check for conj
		if(this.B != null && this.C != null) {
			if(isConj(this.B) && this.A.length()%2 == 1) {
				String half = this.A.substring(0, this.A.length()/2);
				if(this.A.equals(half+"\\"+half)
						&& (half.equals(this.C) || half.equals("("+this.C+")"))) {
					this.B = "conj";
					this.A = this.C+"[conj]";
				}
			}
		}

		// Handle N[num] modifiers
		if(this.B != null && this.C != null) {
			if(this.C.equals("N[num]") && this.B.equals("N/N")) {
				this.A = "N[num]";
			}
			else if(this.B.equals("N/N[num]") && this.C.equals("N")) {
				this.A = "N";
				this.C = "N[num]";
			}
			else if(this.B.equals("(N/N)/N[num]") && this.C.equals("N")) {
				this.C = "N[num]";
			}
		}
	}

	private boolean isConj(String cat) {
		return cat.equals("conj")
				|| cat.equals(",")
				|| cat.equals(";");
	}

	private String cleanCategory(String cat) {
		if(cat==null) {
			return cat;
		}
		if(cat.startsWith("(") && cat.endsWith(")")) {
			if(extraneousParens(cat)) {
				cat = cat.substring(1, cat.length()-1);
			}
		}
		if(cat.equals("NP[nb]")) {
			return "NP";
		}
		if(cat.equals("NP[nb]\\NP[nb]")) {
			cat = "NP\\NP";
		}

		//TODO: handle S[X] categories better?
		cat = cat.replaceAll("\\{X\\}", "");
		cat = cat.replaceAll("S\\[X\\]", "S");

		// TODO: Do we need to restrict N/N[num] or N[num]?
		//cat = cat.replaceAll("N\\[num\\]", "N");


		return cat;
	}

	private boolean extraneousParens(String cat) {
		// TODO Auto-generated method stub
		return true;
	}

	public static ArrayList<OfficialCandCFeature> readFeaturesFromFile(File file) {
		ArrayList<OfficialCandCFeature> features = new ArrayList<OfficialCandCFeature>();
		try {
			Scanner sc = new Scanner(file);
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(!(line.isEmpty() || line.startsWith("#") || line.startsWith(" ") || line.startsWith("\t"))) {
					features.add(getFeatureFromStringDesc(line.trim()));
				}
			}
			sc.close();
		}
		catch(FileNotFoundException e) {
			System.err.println("Failed to open C&C feature file: "+file.getPath());
		}
		return features;
	}

	public char getTemplateID() {
		return this.templateID;
	}

	public int getTrainingFrequency() {
		return this.trainingFrequency;
	}

	public String getParentCategory() {
		return this.A;
	}

	public String getLeftChild() {
		return this.B;
	}

	public String getRightChild() {
		return this.C;
	}

	public String getHeadWord() {
		if(this.storesLexicalHeadWord()) {
			return this.firstHead;
		}
		return null;
	}

	public String getHeadPOS() {
		if(this.storesLexicalHeadPOS()) {
			return this.firstHead;
		}
		return null;
	}

	public String getLeftWord() {
		if(this.storesLeftWord()) {
			return this.firstHead;
		}
		return null;		
	}

	private boolean storesLeftWord() {
		return this.templateID == 'f' || this.templateID == 'g' || this.templateID == 't' || this.templateID == 'u';
	}

	public String getRightWord() {
		if(this.storesRightWord()) {
			return this.secondHead;
		}
		return null;		
	}

	private boolean storesRightWord() {
		return this.templateID == 'f' || this.templateID == 'h' || this.templateID == 't' || this.templateID == 'v';
	}

	public String getLeftPOS() {
		if(this.storesLeftPOS()) {
			return this.firstHead;
		}
		return null;		
	}

	private boolean storesLeftPOS() {
		return this.templateID == 'h' || this.templateID == 'i' || this.templateID == 'v' || this.templateID == 'w';
	}

	public String getRightPOS() {
		if(this.storesRightPOS()) {
			return this.secondHead;
		}
		return null;
	}

	private boolean storesRightPOS() {
		return this.templateID == 'g' || this.templateID == 'i' || this.templateID == 'u' || this.templateID == 'w';
	}

	public String getLexicalHead() {
		if(isNormalFormSurfaceDependencyFeature()) {
			return null;
		}
		return this.firstHead;
	}
	public String getLexicalHeadOfLeftChild() {
		if(this.isNormalFormSurfaceDependencyFeature()) {
			return this.firstHead;
		}
		return null;
	}

	public String getLexicalHeadOfRightChild() {
		if(this.isNormalFormSurfaceDependencyFeature()) {
			return this.secondHead;
		}
		return null;
	}

	public Integer getDistance() {
		if(this.isDistanceFeature()) {
			return this.distance;
		}
		return null;
	}

	public boolean storesLexicalHeadWord() {
		return 		this.templateID == 'a' || this.templateID == 'd' 
				|| 	this.templateID == 'p' || this.templateID == 'q' 
				|| 	(this.templateID >= 'L' && this.templateID <= 'N')
				|| 	(this.templateID >= 'F' && this.templateID <= 'H');
	}

	public boolean storesLexicalHeadPOS() {
		return 		this.templateID == 'b' || this.templateID == 'e' 
				|| 	this.templateID == 'r' || this.templateID == 's'
				|| 	(this.templateID >= 'P' && this.templateID <= 'R')
				|| 	(this.templateID >= 'I' && this.templateID <= 'K');
	}

	public boolean isSharedLexCatFeature() {
		return this.templateID == 'a' || this.templateID == 'b';
	}

	public boolean isSharedRootCatFeature() {
		return this.templateID == 'c' || this.templateID == 'd' || this.templateID == 'e';
	}

	public boolean isSharedRuleFeature() {
		return 		this.templateID == 'm' || this.templateID == 'n'
				|| 	(this.templateID >= 'p' && this.templateID <= 's');
	}

	private boolean isNormalFormFeature() {
		return this.isNormalFormRuleDistanceFeature() || this.isNormalFormSurfaceDependencyFeature();
	}

	public boolean isNormalFormSurfaceDependencyFeature() {
		return this.templateID >= 't' && this.templateID <= 'w';
	}

	public boolean isNormalFormRuleDistanceFeature() {
		return this.templateID >= 'F' && this.templateID <= 'K';
	}

	public boolean isPredArgDependencyFeature() {
		return isPredArgDependencyDistanceFeature() || isPredArgLexicalDependencyFeature();
	}

	public boolean isPredArgDependencyDistanceFeature() {
		char f = this.templateID;
		return 		(f >= 'L' && f <= 'N')
				|| 	(f >= 'P' && f <= 'R');
	}

	public boolean isPredArgLexicalDependencyFeature() {
		char f = this.templateID;
		return (f >= 'f' && f <= 'i');
	}

	private boolean isDistanceFeature() {
		return this.isNormalFormRuleDistanceFeature() || this.isPredArgDependencyDistanceFeature();
	}

	public boolean isSharedFeature() {
		char f = this.templateID;
		return 		(f >= 'a' && f <= 'e')
				|| 	(f >= 'm' && f <= 'n')
				|| 	(f >= 'p' && f <= 's');
	}

	public boolean ruleIsUnary() {
		char f = this.templateID;
		return f=='m' || f=='p' || f=='r';
	}

	public boolean ruleIsBinary() {
		char f = this.templateID;
		return f=='n' || f=='q' || f=='s' || this.isNormalFormFeature() || this.isPredArgDependencyFeature();
	}

	public int convertTemplateID() {
		switch(templateID) {

		// Shared feature templates
		// LexCat (+ Word/POS)
		case 'a': 
			return 0;
		case 'b':
			return 1;
			// RootCat (+ _/Word/POS)
		case 'c':
			return 5;
		case 'd':
			return 6;
		case 'e':
			return 7;
			// Rule (Unary/Binary)
		case 'm':
			return 2;
		case 'n':
			return 8;
			// Rule + Word (Unary/Binary)
		case 'p':
			return 3;
		case 'q':
			return 9;
			// Rule + POS (Unary/Binary)
		case 'r':
			return 4;
		case 's':
			return 10;

			// Normal-form model features
			// 
		case 't':
			return 11;
		case 'u':
			return 13;  // Note (Ryan): yes, these are backwards vs. alphabetical order/the way they're presented in the 2007 paper
		case 'v':
			return 12;
		case 'w':
			return 14;
		case 'F':
			return 15;
		case 'G':
			return 16;
		case 'H':
			return 17;
		case 'I':
			return 18;
		case 'J':
			return 19;
		case 'K':
			return 20;
			// Dependency model features
			// TODO: add dependency model feature template conversions (after implementing dependency model)
		case 'f':
			//return ;
		case 'g':
			//return ;
		case 'h':
			//return ;
		case 'i':
			//return ;
		case 'L':
			//return ;
		case 'M':
			//return ;
		case 'N':
			//return ;
		case 'P':
			//return ;
		case 'Q':
			//return ;
		case 'R':
			//return ;
			// Unrecognized feature template, return dummy value
		default:
			return -999;
		}
	}

	public ConditioningVariables convertToOurNormalForm(Grammar grammar) {
		// TODO: implement

		int intID = this.convertTemplateID();
		int P = grammar.getCatID(this.A);
		if(this.isSharedLexCatFeature()) {
			if(this.storesLexicalHeadWord()) {
				return VariablesFactory.get(intID, P, grammar.getWordID(this.firstHead));
			}
			else if(this.storesLexicalHeadPOS()) {
				return VariablesFactory.get(intID, P, grammar.getPOSID(this.firstHead));
			}
		}
		if(this.isSharedRootCatFeature()) {
			if(this.storesLexicalHeadWord()) {
				return VariablesFactory.get(intID, P, grammar.getWordID(this.firstHead));
			}
			else if(this.storesLexicalHeadPOS()) {
				return VariablesFactory.get(intID, P, grammar.getPOSID(this.firstHead));
			}
			else {
				return VariablesFactory.get(intID, P);
			}
		}
		if(this.isSharedRuleFeature()) {
			int H = grammar.getCatID(this.B);
			if(this.storesLexicalHeadWord()) {
				return VariablesFactory.get(intID, P, H, grammar.getWordID(this.firstHead));
			}
			else if(this.storesLexicalHeadPOS()) {
				return VariablesFactory.get(intID, P, H, grammar.getPOSID(this.firstHead));
			}
			else {
				return VariablesFactory.get(intID, P, H);
			}
		}
		// Normal form features
		if(this.isNormalFormSurfaceDependencyFeature()) {
			int R = grammar.getRuleID(this.A, this.B, this.C);
			if(R < 0) {
				return null;
			}
			int leftHeadID = -1;
			int rightHeadID = -1;
			switch(this.templateID) {
			case 't':
				leftHeadID = grammar.getWordID(this.firstHead);
				rightHeadID = grammar.getWordID(this.secondHead);
				break;
			case 'u':
				leftHeadID = grammar.getWordID(this.firstHead);
				rightHeadID = grammar.getPOSID(this.secondHead);
				break;
			case 'v':
				leftHeadID = grammar.getPOSID(this.firstHead);
				rightHeadID = grammar.getWordID(this.secondHead);
				break;
			case 'w':
				leftHeadID = grammar.getPOSID(this.firstHead);
				rightHeadID = grammar.getPOSID(this.secondHead);
				break;
			default:
				return null;
			}
			return VariablesFactory.get(intID, R, leftHeadID, rightHeadID);
		}
		if(this.isNormalFormRuleDistanceFeature()) {
			int R = grammar.getRuleID(this.A, this.B, this.C);
			if(R < 0) {
				return null;
			}
			if(this.storesLexicalHeadWord()) {
				return VariablesFactory.get(intID, R, grammar.getWordID(this.firstHead), this.distance);
			}
			else if(this.storesLexicalHeadPOS()) {
				return VariablesFactory.get(intID, R, grammar.getPOSID(this.firstHead), this.distance);
			}
		}
		// TODO: implements pred-arg dependency model conversion
		if(this.isPredArgLexicalDependencyFeature()) {
			// TODO: implement
		}
		if(this.isPredArgDependencyDistanceFeature()) {
			// TODO: implement
		}
		// Unrecognized template
		if(intID < 0) {
			System.out.println(this.desc);
		}
		return null;
	}
}
