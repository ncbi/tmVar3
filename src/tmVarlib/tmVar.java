package tmVarlib;
//
// tmVar - Java version
//

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class tmVar
{
	public static MaxentTagger tagger = new MaxentTagger();
	public static SnowballStemmer stemmer = new englishStemmer();
	public static ArrayList<String> RegEx_DNAMutation_STR=new ArrayList<String>(); 
	public static ArrayList<String> RegEx_ProteinMutation_STR=new ArrayList<String>(); 
	public static ArrayList<String> RegEx_SNP_STR=new ArrayList<String>();
	public static ArrayList<String> PAM_lowerScorePair = new ArrayList<String>();
	public static HashMap<String,String> GeneVariantMention = new HashMap<String,String>();
	public static HashMap<String,Integer> VariantFrequency = new HashMap<String,Integer>();
	public static Pattern Pattern_Component_1;
	public static Pattern Pattern_Component_2;
	public static Pattern Pattern_Component_3;
	public static Pattern Pattern_Component_4;
	public static Pattern Pattern_Component_5;
	public static Pattern Pattern_Component_6;
	public static boolean GeneMention = false; // will be turn to true if "ExtractFeature" can find gene mention
	public static HashMap<String,String> nametothree = new HashMap<String,String>();
	public static HashMap<String,String> threetone = new HashMap<String,String>();
	public static HashMap<String,String> threetone_nu = new HashMap<String,String>();
	public static HashMap<String,String> NTtoATCG = new HashMap<String,String>();
	public static ArrayList<String> MF_Pattern = new ArrayList<String>();
	public static ArrayList<String> MF_Type = new ArrayList<String>();
	public static HashMap<String,String> filteringStr_hash = new HashMap<String,String>();
	public static HashMap<String,String> Mutation_RS_Geneid_hash = new HashMap<String,String>();
	public static ArrayList<String> RS_DNA_Protein = new ArrayList<String>();
	public static HashMap<String,String> one2three = new HashMap<String,String>();
	public static PrefixTree PT_GeneVariantMention = new PrefixTree();
	public static HashMap<String,String> VariantType_hash = new HashMap<String,String>();
	public static HashMap<String,String> Number_word2digit = new HashMap<String,String>();
	public static HashMap<String,String> grouped_variants = new HashMap<String,String>();
	public static HashMap<String,String> nu2aa_hash = new HashMap<String,String>();
	public static HashMap<String,Integer> RS2Frequency_hash = new HashMap<String,Integer>();
	public static HashMap<String,HashMap<Integer,String>> variant_mention_to_filter_overlap_gene = new HashMap<String,HashMap<Integer,String>>(); // gene mention: GCC
	public static HashMap<String,String> Gene2HumanGene_hash = new HashMap<String,String>();
	public static HashMap<String,String> Variant2MostCorrespondingGene_hash = new HashMap<String,String>();
	public static HashMap<String,String> RSandPosition2Seq_hash = new HashMap<String,String>();
	
	public static void main(String [] args) throws IOException, InterruptedException, XMLStreamException, SQLException 
	{
		/*
		 * Parameters
		 */
		String InputFolder="input";
		String OutputFolder="output";
		String TrainTest="Test"; //Train|Train_Mention|Test|Test_FullText
		String DeleteTmp="True";
		String DisplayRSnumOnly="True"; // hide the types of the  methods 
		String DisplayChromosome="True"; // hide the chromosome mentions
		String DisplayRefSeq="True"; // hide the RefSeq mentions
		String DisplayGenomicRegion="True";
		String HideMultipleResult="True"; //L858R: 121434568|1057519847|1057519848 --> 121434568
		if(args.length<2)
		{
			System.out.println("\n$ java -Xmx5G -Xms5G -jar tmVar.jar [InputFolder] [OutputFolder]");
			System.out.println("[InputFolder] Default : input");
			System.out.println("[OutputFolder] Default : output\n\n");
		}
		else
		{
			InputFolder=args [0];
			OutputFolder=args [1];
			
			if(args.length>2 && args[2].toLowerCase().matches("(train|train_mention|test|test_fulltext)"))
			{
				TrainTest=args [2];
				if(args[2].toLowerCase().matches("(train|train_mention)"))
				{
					DeleteTmp="False";
				}
			}
			if(args.length>3 && args[3].toLowerCase().matches("(True|False)"))
			{
				DeleteTmp=args [3];
			}
			if(args.length>4 && args[4].toLowerCase().matches("(True|False)"))
			{
				DisplayRSnumOnly=args [4];
			}
			if(args.length>5 && args[5].toLowerCase().matches("(True|False)"))
			{
				HideMultipleResult=args [4];
			}
		}
		
		double startTime,endTime,totTime;
		startTime = System.currentTimeMillis();//start time
		BioCConverter BC= new BioCConverter();
		
		/**
		 * Import models and resource
		 */
		{
			/*
			 * POSTagging: loading model
			 */
			tagger = new MaxentTagger("lib/taggers/english-left3words-distsim.tagger");
			
			/*
			 * Stemming : using Snowball
			 */
			stemmer = new englishStemmer();
			
			/*
			 * PAM 140 : <=-6 pairs 
			 */
			BufferedReader PAM = new BufferedReader(new InputStreamReader(new FileInputStream("lib/PAM140-6.txt"), "UTF-8"));
			String line="";
			while ((line = PAM.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				PAM_lowerScorePair.add(nt[0]+"\t"+nt[1]);
				PAM_lowerScorePair.add(nt[1]+"\t"+nt[0]);
			}
			PAM.close();
			
			/*
			 * Variant frequency
			 */
			BufferedReader frequency = new BufferedReader(new InputStreamReader(new FileInputStream("Database/rs.rank.txt"), "UTF-8"));
			line="";
			while ((line = frequency.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				VariantFrequency.put(nt[1],Integer.parseInt(nt[0]));
			}
			frequency.close();
			/*
			 * HGVs nomenclature lookup - RegEx : DNAMutation
			 */
			BufferedReader RegEx_DNAMutation = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/DNAMutation.RegEx.txt"), "UTF-8"));
			line="";
			while ((line = RegEx_DNAMutation.readLine()) != null)  
			{
				RegEx_DNAMutation_STR.add(line);
			}
			RegEx_DNAMutation.close();
			
			/*
			 * HGVs nomenclature lookup - RegEx : ProteinMutation
			 */
			BufferedReader RegEx_ProteinMutation = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/ProteinMutation.RegEx.txt"), "UTF-8"));
			line="";
			while ((line = RegEx_ProteinMutation.readLine()) != null)  
			{
				RegEx_ProteinMutation_STR.add(line);
			}
			RegEx_ProteinMutation.close();
			
			/*
			 * HGVs nomenclature lookup - RegEx : SNP
			 */
			BufferedReader RegEx_SNP = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/SNP.RegEx.txt"), "UTF-8"));
			line="";
			while ((line = RegEx_SNP.readLine()) != null)  
			{
				RegEx_SNP_STR.add(line);
			}
			RegEx_SNP.close();
			
			/*
			 * Append-pattern
			 */
			BufferedReader RegEx_NL = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/MF.RegEx.2.txt"), "UTF-8"));
			while ((line = RegEx_NL.readLine()) != null)  
			{
				String RegEx[]=line.split("\t");
				MF_Pattern.add(RegEx[0]);
				MF_Type.add(RegEx[1]);
			}
			RegEx_NL.close();
			
			/*
			 * Append-pattern
			 */
			
			BufferedReader VariantType = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/VariantType.txt"), "UTF-8"));
			while ((line = VariantType.readLine()) != null)  
			{
				String split[]=line.split("\t");
				VariantType_hash.put(split[0],split[1]);
			}
			VariantType.close();
			
			/*
			 * nu2aa
			 */
			BufferedReader nu2aa = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/nu2aa.mapping.txt"), "UTF-8"));
			while ((line = nu2aa.readLine()) != null)  
			{
				nu2aa_hash.put(line,"");
			}
			nu2aa.close();
			
					
			//RegEx of component recognition
			Pattern_Component_1 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|(fs[^|]*)\\|([^|]*)$");
			Pattern_Component_2 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|(fs[^|]*)$");
			Pattern_Component_3 = Pattern.compile("^([^|]*)\\|([^|]*(ins|del|Del|dup|-)[^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern_Component_4 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern_Component_5 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern_Component_6 = Pattern.compile("^((\\[rs\\]|rs|RS|Rs|reference SNP no[.] )[0-9]+)$");
			
			nametothree.put("GLUTAMIC","GLU");nametothree.put("ASPARTIC","ASP");nametothree.put("THYMINE", "THY");nametothree.put("ALANINE", "ALA");nametothree.put("ARGININE", "ARG");nametothree.put("ASPARAGINE", "ASN");nametothree.put("ASPARTICACID", "ASP");nametothree.put("ASPARTATE", "ASP");nametothree.put("CYSTEINE", "CYS");nametothree.put("GLUTAMINE", "GLN");nametothree.put("GLUTAMICACID", "GLU");nametothree.put("GLUTAMATE", "GLU");nametothree.put("GLYCINE", "GLY");nametothree.put("HISTIDINE", "HIS");nametothree.put("ISOLEUCINE", "ILE");nametothree.put("LEUCINE", "LEU");nametothree.put("LYSINE", "LYS");nametothree.put("METHIONINE", "MET");nametothree.put("PHENYLALANINE", "PHE");nametothree.put("PROLINE", "PRO");nametothree.put("SERINE", "SER");nametothree.put("THREONINE", "THR");nametothree.put("TRYPTOPHAN", "TRP");nametothree.put("TYROSINE", "TYR");nametothree.put("VALINE", "VAL");nametothree.put("STOP", "XAA");nametothree.put("TERM", "XAA");nametothree.put("TERMINATION", "XAA");nametothree.put("STOP", "XAA");nametothree.put("TERM", "XAA");nametothree.put("TERMINATION", "XAA");nametothree.put("GLUTAMICCODON","GLU");nametothree.put("ASPARTICCODON","ASP");nametothree.put("THYMINECODON","THY");nametothree.put("ALANINECODON","ALA");nametothree.put("ARGININECODON","ARG");nametothree.put("ASPARAGINECODON","ASN");nametothree.put("ASPARTICACIDCODON","ASP");nametothree.put("ASPARTATECODON","ASP");nametothree.put("CYSTEINECODON","CYS");nametothree.put("GLUTAMINECODON","GLN");nametothree.put("GLUTAMICACIDCODON","GLU");nametothree.put("GLUTAMATECODON","GLU");nametothree.put("GLYCINECODON","GLY");nametothree.put("HISTIDINECODON","HIS");nametothree.put("ISOLEUCINECODON","ILE");nametothree.put("LEUCINECODON","LEU");nametothree.put("LYSINECODON","LYS");nametothree.put("METHIONINECODON","MET");nametothree.put("PHENYLALANINECODON","PHE");nametothree.put("PROLINECODON","PRO");nametothree.put("SERINECODON","SER");nametothree.put("THREONINECODON","THR");nametothree.put("TRYPTOPHANCODON","TRP");nametothree.put("TYROSINECODON","TYR");nametothree.put("VALINECODON","VAL");nametothree.put("STOPCODON","XAA");nametothree.put("TERMCODON","XAA");nametothree.put("TERMINATIONCODON","XAA");nametothree.put("STOPCODON","XAA");nametothree.put("TERMCODON","XAA");nametothree.put("TERMINATIONCODON","XAA");nametothree.put("TAG","XAA");nametothree.put("TAA","XAA");nametothree.put("UAG","XAA");nametothree.put("UAA","XAA");
			threetone.put("ALA", "A");threetone.put("ARG", "R");threetone.put("ASN", "N");threetone.put("ASP", "D");threetone.put("CYS", "C");threetone.put("GLN", "Q");threetone.put("GLU", "E");threetone.put("GLY", "G");threetone.put("HIS", "H");threetone.put("ILE", "I");threetone.put("LEU", "L");threetone.put("LYS", "K");threetone.put("MET", "M");threetone.put("PHE", "F");threetone.put("PRO", "P");threetone.put("SER", "S");threetone.put("THR", "T");threetone.put("TRP", "W");threetone.put("TYR", "Y");threetone.put("VAL", "V");threetone.put("ASX", "B");threetone.put("GLX", "Z");threetone.put("XAA", "X");threetone.put("TER", "X");
			threetone_nu.put("GCU","A");threetone_nu.put("GCC","A");threetone_nu.put("GCA","A");threetone_nu.put("GCG","A");threetone_nu.put("CGU","R");threetone_nu.put("CGC","R");threetone_nu.put("CGA","R");threetone_nu.put("CGG","R");threetone_nu.put("AGA","R");threetone_nu.put("AGG","R");threetone_nu.put("AAU","N");threetone_nu.put("AAC","N");threetone_nu.put("GAU","D");threetone_nu.put("GAC","D");threetone_nu.put("UGU","C");threetone_nu.put("UGC","C");threetone_nu.put("GAA","E");threetone_nu.put("GAG","E");threetone_nu.put("CAA","Q");threetone_nu.put("CAG","Q");threetone_nu.put("GGU","G");threetone_nu.put("GGC","G");threetone_nu.put("GGA","G");threetone_nu.put("GGG","G");threetone_nu.put("CAU","H");threetone_nu.put("CAC","H");threetone_nu.put("AUU","I");threetone_nu.put("AUC","I");threetone_nu.put("AUA","I");threetone_nu.put("CUU","L");threetone_nu.put("CUC","L");threetone_nu.put("CUA","L");threetone_nu.put("CUG","L");threetone_nu.put("UUA","L");threetone_nu.put("UUG","L");threetone_nu.put("AAA","K");threetone_nu.put("AAG","K");threetone_nu.put("AUG","M");threetone_nu.put("UUU","F");threetone_nu.put("UUC","F");threetone_nu.put("CCU","P");threetone_nu.put("CCC","P");threetone_nu.put("CCA","P");threetone_nu.put("CCG","P");threetone_nu.put("UCU","S");threetone_nu.put("UCC","S");threetone_nu.put("UCA","S");threetone_nu.put("UCG","S");threetone_nu.put("AGU","S");threetone_nu.put("AGC","S");threetone_nu.put("ACU","T");threetone_nu.put("ACC","T");threetone_nu.put("ACA","T");threetone_nu.put("ACG","T");threetone_nu.put("UGG","W");threetone_nu.put("UAU","Y");threetone_nu.put("UAC","Y");threetone_nu.put("GUU","V");threetone_nu.put("GUC","V");threetone_nu.put("GUA","V");threetone_nu.put("GUG","V");threetone_nu.put("UAA","X");threetone_nu.put("UGA","X");threetone_nu.put("UAG","X");threetone_nu.put("GCT","A");threetone_nu.put("GCC","A");threetone_nu.put("GCA","A");threetone_nu.put("GCG","A");threetone_nu.put("CGT","R");threetone_nu.put("CGC","R");threetone_nu.put("CGA","R");threetone_nu.put("CGG","R");threetone_nu.put("AGA","R");threetone_nu.put("AGG","R");threetone_nu.put("AAT","N");threetone_nu.put("AAC","N");threetone_nu.put("GAT","D");threetone_nu.put("GAC","D");threetone_nu.put("TGT","C");threetone_nu.put("TGC","C");threetone_nu.put("GAA","E");threetone_nu.put("GAG","E");threetone_nu.put("CAA","Q");threetone_nu.put("CAG","Q");threetone_nu.put("GGT","G");threetone_nu.put("GGC","G");threetone_nu.put("GGA","G");threetone_nu.put("GGG","G");threetone_nu.put("CAT","H");threetone_nu.put("CAC","H");threetone_nu.put("ATT","I");threetone_nu.put("ATC","I");threetone_nu.put("ATA","I");threetone_nu.put("CTT","L");threetone_nu.put("CTC","L");threetone_nu.put("CTA","L");threetone_nu.put("CTG","L");threetone_nu.put("TTA","L");threetone_nu.put("TTG","L");threetone_nu.put("AAA","K");threetone_nu.put("AAG","K");threetone_nu.put("ATG","M");threetone_nu.put("TTT","F");threetone_nu.put("TTC","F");threetone_nu.put("CCT","P");threetone_nu.put("CCC","P");threetone_nu.put("CCA","P");threetone_nu.put("CCG","P");threetone_nu.put("TCT","S");threetone_nu.put("TCC","S");threetone_nu.put("TCA","S");threetone_nu.put("TCG","S");threetone_nu.put("AGT","S");threetone_nu.put("AGC","S");threetone_nu.put("ACT","T");threetone_nu.put("ACC","T");threetone_nu.put("ACA","T");threetone_nu.put("ACG","T");threetone_nu.put("TGG","W");threetone_nu.put("TAT","Y");threetone_nu.put("TAC","Y");threetone_nu.put("GTT","V");threetone_nu.put("GTC","V");threetone_nu.put("GTA","V");threetone_nu.put("GTG","V");threetone_nu.put("TAA","X");threetone_nu.put("TGA","X");threetone_nu.put("TAG","X");
			NTtoATCG.put("ADENINE", "A");NTtoATCG.put("CYTOSINE", "C");NTtoATCG.put("GUANINE", "G");NTtoATCG.put("URACIL", "U");NTtoATCG.put("THYMINE", "T");NTtoATCG.put("ADENOSINE", "A");NTtoATCG.put("CYTIDINE", "C");NTtoATCG.put("THYMIDINE", "T");NTtoATCG.put("GUANOSINE", "G");NTtoATCG.put("URIDINE", "U");

			Number_word2digit.put("ZERO","0");Number_word2digit.put("SINGLE","1");Number_word2digit.put("ONE","1");Number_word2digit.put("TWO","2");Number_word2digit.put("THREE","3");Number_word2digit.put("FOUR","4");Number_word2digit.put("FIVE","5");Number_word2digit.put("SIX","6");Number_word2digit.put("SEVEN","7");Number_word2digit.put("EIGHT","8");Number_word2digit.put("NINE","9");Number_word2digit.put("TWN","10");
			
			//Filtering
			BufferedReader filterfile = new BufferedReader(new InputStreamReader(new FileInputStream("lib/filtering.txt"), "UTF-8"));
			while ((line = filterfile.readLine()) != null)  
			{
				filteringStr_hash.put(line, "");
			}
			filterfile.close();
			
			/*one2three*/
			one2three.put("A", "Ala");
			one2three.put("R", "Arg");
			one2three.put("N", "Asn");
			one2three.put("D", "Asp");
			one2three.put("C", "Cys");
			one2three.put("Q", "Gln");
			one2three.put("E", "Glu");
			one2three.put("G", "Gly");
			one2three.put("H", "His");
			one2three.put("I", "Ile");
			one2three.put("L", "Leu");
			one2three.put("K", "Lys");
			one2three.put("M", "Met");
			one2three.put("F", "Phe");
			one2three.put("P", "Pro");
			one2three.put("S", "Ser");
			one2three.put("T", "Thr");
			one2three.put("W", "Trp");
			one2three.put("Y", "Tyr");
			one2three.put("V", "Val");
			one2three.put("B", "Asx");
			one2three.put("Z", "Glx");
			one2three.put("X", "Xaa");
			one2three.put("X", "Ter");
			
			/*RS_DNA_Protein.txt - Pattern : PP[P]+[ ]*[\(\[][ ]*DD[D]+[ ]*[\)\]][ ]*[\(\[][ ]*SS[S]+[ ]*[\)\]]*/
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/RS_DNA_Protein.txt"), "UTF-8"));
			while ((line = inputfile.readLine()) != null)  
			{
				if(!line.equals(""))
				{
					RS_DNA_Protein.add(line);
				}
			}
			inputfile.close();
			
			/*PT_GeneVariantMention - BRAFV600E*/
			PT_GeneVariantMention.TreeFile2Tree("lib/PT_GeneVariantMention.txt");
				
			/*Mutation_RS_Geneid.txt - the patterns retrieved from PubMed result*/
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RegEx/Mutation_RS_Geneid.txt"), "UTF-8"));
			while ((line = inputfile.readLine()) != null)  
			{
				//Pattern	c|SUB|C|1749|T	2071558	269
				Pattern pat = Pattern.compile("^(Pattern|Recognized|ManuallyAdded)	([^\t]+)	([0-9]+)	([0-9,]+)$");
				Matcher mat = pat.matcher(line);
				if (mat.find())
	        	{
					String geneids[]=mat.group(4).split(",");
					for(int i=0;i<geneids.length;i++)
					{
						//mutation id | geneid --> rs#
						Mutation_RS_Geneid_hash.put(mat.group(2)+"\t"+geneids[i], mat.group(3));
					}
	        	}
			}
			inputfile.close();
			/** tmVarForm2RSID2Freq - together with Mutation_RS_Geneid.txt (the patterns retrieved from PubMed result) */
			BufferedReader tmVarForm2RSID2Freq = new BufferedReader(new InputStreamReader(new FileInputStream("lib/tmVarForm2RSID2Freq.txt"), "UTF-8"));
			line="";
			while ((line = tmVarForm2RSID2Freq.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				String tmVarForm=nt[0];
				String rs_gene_freq=nt[1];
				//RS:rs121913377|Gene:673|Freq:3
				Pattern pat = Pattern.compile("^RS:rs([0-9]+)\\|Gene:([0-9,]+)\\|Freq:([0-9]+)$");
				Matcher mat = pat.matcher(rs_gene_freq);
				if (mat.find())
	        	{
					String rs=mat.group(1);
					String gene=mat.group(2);
					Mutation_RS_Geneid_hash.put(tmVarForm+"\t"+gene,rs);
	        	}
			}
			tmVarForm2RSID2Freq.close();

			/** RS2Frequency - rs# to its frequency in PTC) */
			BufferedReader RS2Frequency = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RS2Frequency.txt"), "UTF-8"));
			line="";
			while ((line = RS2Frequency.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				String rs=nt[0];
				int freq=Integer.parseInt(nt[1]);
				RS2Frequency_hash.put(rs,freq);
			}
			RS2Frequency.close();
			
			/** Homoid2HumanGene_hash **/
			HashMap<String,String> Homoid2HumanGene_hash = new HashMap<String,String>();
			BufferedReader Homoid2HumanGene = new BufferedReader(new InputStreamReader(new FileInputStream("Database/Homoid2HumanGene.txt"), "UTF-8"));
			line="";
			while ((line = Homoid2HumanGene.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				String homoid=nt[0];
				String humangeneid=nt[1];
				Homoid2HumanGene_hash.put(homoid,humangeneid);
			}
			Homoid2HumanGene.close();
			
			/** Gene2Homoid.txt **/
			BufferedReader Gene2Homoid = new BufferedReader(new InputStreamReader(new FileInputStream("Database/Gene2Homoid.txt"), "UTF-8"));
			line="";
			while ((line = Gene2Homoid.readLine()) != null)  
			{
				String nt[]=line.split("\t");
				String geneid=nt[0];
				String homoid=nt[1];
				if(Homoid2HumanGene_hash.containsKey(homoid))
				{
					if(!geneid.equals(Homoid2HumanGene_hash.get(homoid)))
					{
						Gene2HumanGene_hash.put(geneid,Homoid2HumanGene_hash.get(homoid));
					}
				}
			}
			Gene2Homoid.close();
			
			/** Variant2MostCorrespondingGene **/
			BufferedReader Variant2MostCorrespondingGene = new BufferedReader(new InputStreamReader(new FileInputStream("Database/var2gene.txt"), "UTF-8"));
			line="";
			while ((line = Variant2MostCorrespondingGene.readLine()) != null)  
			{
				String nt[]=line.split("\t"); //4524	1801133 C677T
				String geneid=nt[0];
				String rsid=nt[1];
				String var=nt[2].toLowerCase();
				Variant2MostCorrespondingGene_hash.put(var,geneid+"\t"+rsid);
			}
			Variant2MostCorrespondingGene.close();
			
			BufferedReader RSandPosition2Seq = new BufferedReader(new InputStreamReader(new FileInputStream("lib/RS2tmVarForm.txt"), "UTF-8"));
			line="";
			while ((line = RSandPosition2Seq.readLine()) != null)  
			{
				String nt[]=line.split("\t"); //121908752	c	SUB	T	617	G
				if(nt.length>3)
				{
					String rs=nt[0];
					String seq=nt[1];
					String P=nt[4];
					RSandPosition2Seq_hash.put(rs+"\t"+P,seq);
				}
			}
			RSandPosition2Seq.close();
			
		}
		
		File folder = new File(InputFolder);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++)
		{
			if (listOfFiles[i].isFile()) 
			{
				String InputFile = listOfFiles[i].getName();
				
				File f = new File(OutputFolder+"/"+InputFile+".PubTator");
				File f_BioC = new File(OutputFolder+"/"+InputFile+".BioC.XML");

				if(f.exists() && !f.isDirectory()) 
				{ 
					System.out.println(InputFolder+"/"+InputFile+" - Done. (The output file (PubTator) exists in output folder)");
				}
				else if(f_BioC.exists() && !f_BioC.isDirectory()) 
				{ 
					System.out.println(InputFolder+"/"+InputFile+" - Done. (The output file (BioC) exists in output folder)");
				}
				else
				{
					/*
					 * Mention recognition by CRF++
					 */
					if(TrainTest.equals("Test") || TrainTest.equals("Test_FullText") || TrainTest.equals("Test_FullText") || TrainTest.equals("Train"))
					{
						/*
						 * Format Check 
						 */
						String Format = "";
						String checkR=BC.BioCFormatCheck(InputFolder+"/"+InputFile);
						if(checkR.equals("BioC"))
						{
							Format = "BioC";
						}
						else if(checkR.equals("PubTator"))
						{
							Format = "PubTator";
						}
						else
						{
							System.out.println(checkR);
							System.exit(0);
						}
						
						System.out.print(InputFolder+"/"+InputFile+" - ("+Format+" format) : Processing ... \r");
						 
						/*
						 * Pre-processing
						 */
						MentionRecognition MR= new MentionRecognition();
						if(Format.equals("BioC"))
						{
							BC.BioC2PubTator(InputFolder+"/"+InputFile,"tmp/"+InputFile);
							MR.FeatureExtraction("tmp/"+InputFile,"tmp/"+InputFile+".data","tmp/"+InputFile+".location",TrainTest);
						}
						else if(Format.equals("PubTator"))
						{
							MR.FeatureExtraction(InputFolder+"/"+InputFile,"tmp/"+InputFile+".data","tmp/"+InputFile+".location",TrainTest);
						}
						if(TrainTest.equals("Test") || TrainTest.equals("Test_FullText"))
						{
							MR.CRF_test("tmp/"+InputFile+".data","tmp/"+InputFile+".output",TrainTest);
						}
						
						/*
						 * CRF++ output --> PubTator
						 */
						PostProcessing PP = new PostProcessing();
						{
							if(Format.equals("BioC"))
							{
								PP.toME("tmp/"+InputFile,"tmp/"+InputFile+".output","tmp/"+InputFile+".location","tmp/"+InputFile+".ME");
								PP.toPostME("tmp/"+InputFile+".ME","tmp/"+InputFile+".PostME");
								PP.toPostMEData("tmp/"+InputFile,"tmp/"+InputFile+".PostME","tmp/"+InputFile+".PostME.ml","tmp/"+InputFile+".PostME.data",TrainTest);
							}
							else if(Format.equals("PubTator"))
							{
								PP.toME(InputFolder+"/"+InputFile,"tmp/"+InputFile+".output","tmp/"+InputFile+".location","tmp/"+InputFile+".ME");
								PP.toPostME("tmp/"+InputFile+".ME","tmp/"+InputFile+".PostME");
								PP.toPostMEData(InputFolder+"/"+InputFile,"tmp/"+InputFile+".PostME","tmp/"+InputFile+".PostME.ml","tmp/"+InputFile+".PostME.data",TrainTest);
							}
							if(TrainTest.equals("Test") || TrainTest.equals("Test_FullText"))
							{
								PP.toPostMEoutput("tmp/"+InputFile+".PostME.data","tmp/"+InputFile+".PostME.output");
							}
							
							else if(TrainTest.equals("Train"))
							{
								PP.toPostMEModel("tmp/"+InputFile+".PostME.data");
							}
							
							
							/*
							 * Post-processing
							 */
							if(TrainTest.equals("Test") || TrainTest.equals("Test_FullText"))
							{
								GeneMention = true;
								if(GeneMention == true) // MentionRecognition detect Gene mentions
								{
									PP.output2PubTator("tmp/"+InputFile+".PostME.ml","tmp/"+InputFile+".PostME.output","tmp/"+InputFile+".PostME","tmp/"+InputFile+".PubTator");
									
									if(Format.equals("BioC"))
									{
										PP.Normalization("tmp/"+InputFile,"tmp/"+InputFile+".PubTator",OutputFolder+"/"+InputFile+".PubTator",DisplayRSnumOnly,HideMultipleResult,DisplayChromosome,DisplayRefSeq,DisplayGenomicRegion);
									}
									else if(Format.equals("PubTator"))
									{
										PP.Normalization(InputFolder+"/"+InputFile,"tmp/"+InputFile+".PubTator",OutputFolder+"/"+InputFile+".PubTator",DisplayRSnumOnly,HideMultipleResult,DisplayChromosome,DisplayRefSeq,DisplayGenomicRegion);
									}
								}
								else
								{
									PP.output2PubTator("tmp/"+InputFile+".PostME.ml","tmp/"+InputFile+".PostME.output","tmp/"+InputFile+".PostME",OutputFolder+"/"+InputFile+".PubTator");
								}
								
								if(Format.equals("BioC"))
								{
									BC.PubTator2BioC_AppendAnnotation(OutputFolder+"/"+InputFile+".PubTator",InputFolder+"/"+InputFile,OutputFolder+"/"+InputFile+".BioC.XML");
								}			
							}
						}
						
						/*
						 * Time stamp - last
						 */
						endTime = System.currentTimeMillis();//ending time
						totTime = endTime - startTime;
						System.out.println(InputFolder+"/"+InputFile+" - ("+Format+" format) : Processing Time:"+totTime/1000+"sec");
						
						/*
						 * remove tmp files
						 */
						if(DeleteTmp.toLowerCase().equals("true"))
						{
							String path="tmp"; 
					        File file = new File(path);
					        File[] files = file.listFiles(); 
					        for (File ftmp:files) 
					        {
					        	if (ftmp.isFile() && ftmp.exists()) 
					            {
					        		if(ftmp.toString().matches("tmp."+InputFile+".*"))
						        	{
					        			ftmp.delete();
						        	}
					        	}
					        }
						}
					}
					else if(TrainTest.equals("Train_Mention"))
					{
						System.out.print(InputFolder+"/"+InputFile+" - Processing ... \r");
						 
						PostProcessing PP = new PostProcessing();
						PP.toPostMEData(InputFolder+"/"+InputFile,"tmp/"+InputFile+".PostME","tmp/"+InputFile+".PostME.ml","tmp/"+InputFile+".PostME.data","Train");
						
						/*
						 * Time stamp - last
						 */
						endTime = System.currentTimeMillis();//ending time
						totTime = endTime - startTime;
						System.out.println(InputFolder+"/"+InputFile+" - Processing Time:"+totTime/1000+"sec");
					}
				}
			}
		}
	}
}
