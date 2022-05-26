/**
 * Project: 
 * Function: Dictionary lookup by Prefix Tree
 */

package tmVarlib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrefixTree
{
	private Tree Tr=new Tree();
	
	/**
	 * Hash2Tree(HashMap<String, String> ID2Names)
	 * Dictionary2Tree_Combine(String Filename,String StopWords,String MentionType)
	 * Dictionary2Tree_UniqueGene(String Filename,String StopWords)	 //olr1831ps	10116:*405718
	 * TreeFile2Tree(String Filename)
	 * 
	 */
	public static HashMap<String, String> StopWord_hash = new HashMap<String, String>();
	
	public void Hash2Tree(HashMap<String, String> ID2Names)
	{
		for(String ID : ID2Names.keySet())  
		{
			Tr.insertMention(ID2Names.get(ID),ID);
		}
	}
	
	/*
	 * Type	Identifier	Names
	 * Species	9606	ttdh3pv|igl027/99|igl027/98|sw 1463
	 */
	public void Dictionary2Tree(String Filename,String StopWords)	
	{
		try 
		{
			/** Stop Word */
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(StopWords), "UTF-8"));
			String line="";
			while ((line = br.readLine()) != null)  
			{
				StopWord_hash.put(line, "StopWord");
			}
			br.close();	
			
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			line="";
			while ((line = inputfile.readLine()) != null)  
			{
				line = line.replaceAll("ω","w");line = line.replaceAll("μ","u");line = line.replaceAll("κ","k");line = line.replaceAll("α","a");line = line.replaceAll("γ","r");line = line.replaceAll("β","b");line = line.replaceAll("×","x");line = line.replaceAll("¹","1");line = line.replaceAll("²","2");line = line.replaceAll("°","o");line = line.replaceAll("ö","o");line = line.replaceAll("é","e");line = line.replaceAll("à","a");line = line.replaceAll("Á","A");line = line.replaceAll("ε","e");line = line.replaceAll("θ","O");line = line.replaceAll("•",".");line = line.replaceAll("µ","u");line = line.replaceAll("λ","r");line = line.replaceAll("⁺","+");line = line.replaceAll("ν","v");line = line.replaceAll("ï","i");line = line.replaceAll("ã","a");line = line.replaceAll("≡","=");line = line.replaceAll("ó","o");line = line.replaceAll("³","3");line = line.replaceAll("〖","[");line = line.replaceAll("〗","]");line = line.replaceAll("Å","A");line = line.replaceAll("ρ","p");line = line.replaceAll("ü","u");line = line.replaceAll("ɛ","e");line = line.replaceAll("č","c");line = line.replaceAll("š","s");line = line.replaceAll("ß","b");line = line.replaceAll("═","=");line = line.replaceAll("£","L");line = line.replaceAll("Ł","L");line = line.replaceAll("ƒ","f");line = line.replaceAll("ä","a");line = line.replaceAll("–","-");line = line.replaceAll("⁻","-");line = line.replaceAll("〈","<");line = line.replaceAll("〉",">");line = line.replaceAll("χ","X");line = line.replaceAll("Đ","D");line = line.replaceAll("‰","%");line = line.replaceAll("·",".");line = line.replaceAll("→",">");line = line.replaceAll("←","<");line = line.replaceAll("ζ","z");line = line.replaceAll("π","p");line = line.replaceAll("τ","t");line = line.replaceAll("ξ","X");line = line.replaceAll("η","h");line = line.replaceAll("ø","0");line = line.replaceAll("Δ","D");line = line.replaceAll("∆","D");line = line.replaceAll("∑","S");line = line.replaceAll("Ω","O");line = line.replaceAll("δ","d");line = line.replaceAll("σ","s");
				String Column[]=line.split("\t",-1);
				if(Column.length>2)
				{
					String ConceptType=Column[0];
					String ConceptID=Column[1];
					String ConceptNames=Column[2];
					/*
					 * Specific usage for Species
					 */
					if(	ConceptType.equals("Species"))
					{
						ConceptID=ConceptID.replace("species:ncbi:","");
						ConceptNames=ConceptNames.replaceAll(" strain=", " ");
						ConceptNames=ConceptNames.replaceAll("[\\W\\-\\_](str.|strain|substr.|substrain|var.|variant|subsp.|subspecies|pv.|pathovars|pathovar|br.|biovar)[\\W\\-\\_]", " ");
						ConceptNames=ConceptNames.replaceAll("[\\(\\)]", " ");
					}
					
					String NameColumn[]=ConceptNames.split("\\|");
					for(int i=0;i<NameColumn.length;i++)
					{
						String tmp = NameColumn[i];
						tmp=tmp.replaceAll("[\\W\\-\\_0-9]", "");
						
						/*
						 * Specific usage for Species
						 */
						if(	ConceptType.equals("Species"))
						{
							if ( (!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]")) && (!NameColumn[i].matches("a[\\W\\-\\_].*")) && tmp.length()>=3)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
								}
							}
						}
						/*
						 * Specific usage for Gene & Cell
						 */
						else if ((ConceptType.equals("Gene") || ConceptType.equals("Cell")) )
						{
							if ( (!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]")) && tmp.length()>=3)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
								}
							}
						}
						/*
						 * Other Concepts
						 */
						else
						{
							if ( (!NameColumn[i].equals("")) &&	(!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]"))	)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
								}
							}
						}
					}
				}
				else
				{
					System.out.println("[Dictionary2Tree_Combine]: Lexicon format error! Please follow : Type | Identifier | Names (Identifier can be NULL)");
				}
			}
			inputfile.close();	
		}
		catch(IOException e1){ System.out.println("[Dictionary2Tree_Combine]: Input file is not exist.");}
	}
	
	/*
	 * Type	Identifier	Names
	 * Species	9606	ttdh3pv|igl027/99|igl027/98|sw 1463
	 * 
	 * @ Prefix
	 */
	public void Dictionary2Tree(String Filename,String StopWords,String Prefix)	
	{
		try 
		{
			/** Stop Word */
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(StopWords), "UTF-8"));
			String line="";
			while ((line = br.readLine()) != null)  
			{
				StopWord_hash.put(line, "StopWord");
			}
			br.close();	
			
			/** Parsing Input */
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			line="";
			while ((line = inputfile.readLine()) != null)  
			{
				line = line.replaceAll("ω","w");line = line.replaceAll("μ","u");line = line.replaceAll("κ","k");line = line.replaceAll("α","a");line = line.replaceAll("γ","r");line = line.replaceAll("β","b");line = line.replaceAll("×","x");line = line.replaceAll("¹","1");line = line.replaceAll("²","2");line = line.replaceAll("°","o");line = line.replaceAll("ö","o");line = line.replaceAll("é","e");line = line.replaceAll("à","a");line = line.replaceAll("Á","A");line = line.replaceAll("ε","e");line = line.replaceAll("θ","O");line = line.replaceAll("•",".");line = line.replaceAll("µ","u");line = line.replaceAll("λ","r");line = line.replaceAll("⁺","+");line = line.replaceAll("ν","v");line = line.replaceAll("ï","i");line = line.replaceAll("ã","a");line = line.replaceAll("≡","=");line = line.replaceAll("ó","o");line = line.replaceAll("³","3");line = line.replaceAll("〖","[");line = line.replaceAll("〗","]");line = line.replaceAll("Å","A");line = line.replaceAll("ρ","p");line = line.replaceAll("ü","u");line = line.replaceAll("ɛ","e");line = line.replaceAll("č","c");line = line.replaceAll("š","s");line = line.replaceAll("ß","b");line = line.replaceAll("═","=");line = line.replaceAll("£","L");line = line.replaceAll("Ł","L");line = line.replaceAll("ƒ","f");line = line.replaceAll("ä","a");line = line.replaceAll("–","-");line = line.replaceAll("⁻","-");line = line.replaceAll("〈","<");line = line.replaceAll("〉",">");line = line.replaceAll("χ","X");line = line.replaceAll("Đ","D");line = line.replaceAll("‰","%");line = line.replaceAll("·",".");line = line.replaceAll("→",">");line = line.replaceAll("←","<");line = line.replaceAll("ζ","z");line = line.replaceAll("π","p");line = line.replaceAll("τ","t");line = line.replaceAll("ξ","X");line = line.replaceAll("η","h");line = line.replaceAll("ø","0");line = line.replaceAll("Δ","D");line = line.replaceAll("∆","D");line = line.replaceAll("∑","S");line = line.replaceAll("Ω","O");line = line.replaceAll("δ","d");line = line.replaceAll("σ","s");
				String Column[]=line.split("\t");
				if(Column.length>2)
				{
					String ConceptType=Column[0];
					String ConceptID=Column[1];
					String ConceptNames=Column[2];
					
					/*
					 * Specific usage for Species
					 */
					if(	ConceptType.equals("Species"))
					{
						ConceptID=ConceptID.replace("species:ncbi:","");
						ConceptNames=ConceptNames.replaceAll(" strain=", " ");
						ConceptNames=ConceptNames.replaceAll("[\\W\\-\\_](str.|strain|substr.|substrain|var.|variant|subsp.|subspecies|pv.|pathovars|pathovar|br.|biovar)[\\W\\-\\_]", " ");
						ConceptNames=ConceptNames.replaceAll("[\\(\\)]", " ");
					}
					String NameColumn[]=ConceptNames.split("\\|");
					
					for(int i=0;i<NameColumn.length;i++)
					{
						String tmp = NameColumn[i];
						tmp=tmp.replaceAll("[\\W\\-\\_0-9]", "");
						
						/*
						 * Specific usage for Species
						 */
						if(	ConceptType.equals("Species") )
						{
							if ((!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]")) && (!NameColumn[i].matches("a[\\W\\-\\_].*")) &&	tmp.length()>=3	)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									if(Prefix.equals(""))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Num") && NameColumn[i].toLowerCase().matches("[0-9].*"))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(NameColumn[i].length()>=2 && NameColumn[i].toLowerCase().substring(0,2).equals(Prefix))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Other")
											 && (!NameColumn[i].toLowerCase().matches("[0-9].*"))
											 && (!NameColumn[i].toLowerCase().matches("[a-z][a-z].*"))
											)
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
								}
							}
						}
						/*
						 * Specific usage for Gene & Cell
						 */
						else if ((ConceptType.equals("Gene") || ConceptType.equals("Cell")))
						{
							if(	(!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]")) &&	tmp.length()>=3	)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									if(Prefix.equals(""))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Num") && NameColumn[i].toLowerCase().matches("[0-9].*"))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(NameColumn[i].length()>=2 && NameColumn[i].toLowerCase().substring(0,2).equals(Prefix))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Other")
											 && (!NameColumn[i].toLowerCase().matches("[0-9].*"))
											 && (!NameColumn[i].toLowerCase().matches("[a-z][a-z].*"))
											)
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
								}
							}
						}
						/*
						 * Other Concepts
						 */
						else
						{
							if ( (!NameColumn[i].equals("")) && (!NameColumn[i].substring(0, 1).matches("[\\W\\-\\_]"))	)
							{
								String tmp_mention=NameColumn[i].toLowerCase();
								if(!StopWord_hash.containsKey(tmp_mention))
								{
									if(Prefix.equals(""))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Num") && NameColumn[i].toLowerCase().matches("[0-9].*"))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(NameColumn[i].length()>2 && NameColumn[i].toLowerCase().substring(0,2).equals(Prefix))
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
									else if(Prefix.equals("Other")
											 && (!NameColumn[i].toLowerCase().matches("[0-9].*"))
											 && (!NameColumn[i].toLowerCase().matches("[a-z][a-z].*"))
											)
									{
										Tr.insertMention(NameColumn[i],ConceptType+"\t"+ConceptID);
									}
								}
							}
						}
					}
				}
				else
				{
					System.out.println("[Dictionary2Tree_Combine]: Lexicon format error! Please follow : Type | Identifier | Names (Identifier can be NULL)");
				}
			}
			inputfile.close();	
		}
		catch(IOException e1){ System.out.println("[Dictionary2Tree_Combine]: Input file is not exist.");}
	}
	public void TreeFile2Tree(String Filename)	
	{
		try 
		{
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			String line="";
			int count=0;
			while ((line = inputfile.readLine()) != null)  
			{
				line = line.replaceAll("ω","w");line = line.replaceAll("μ","u");line = line.replaceAll("κ","k");line = line.replaceAll("α","a");line = line.replaceAll("γ","r");line = line.replaceAll("β","b");line = line.replaceAll("×","x");line = line.replaceAll("¹","1");line = line.replaceAll("²","2");line = line.replaceAll("°","o");line = line.replaceAll("ö","o");line = line.replaceAll("é","e");line = line.replaceAll("à","a");line = line.replaceAll("Á","A");line = line.replaceAll("ε","e");line = line.replaceAll("θ","O");line = line.replaceAll("•",".");line = line.replaceAll("µ","u");line = line.replaceAll("λ","r");line = line.replaceAll("⁺","+");line = line.replaceAll("ν","v");line = line.replaceAll("ï","i");line = line.replaceAll("ã","a");line = line.replaceAll("≡","=");line = line.replaceAll("ó","o");line = line.replaceAll("³","3");line = line.replaceAll("〖","[");line = line.replaceAll("〗","]");line = line.replaceAll("Å","A");line = line.replaceAll("ρ","p");line = line.replaceAll("ü","u");line = line.replaceAll("ɛ","e");line = line.replaceAll("č","c");line = line.replaceAll("š","s");line = line.replaceAll("ß","b");line = line.replaceAll("═","=");line = line.replaceAll("£","L");line = line.replaceAll("Ł","L");line = line.replaceAll("ƒ","f");line = line.replaceAll("ä","a");line = line.replaceAll("–","-");line = line.replaceAll("⁻","-");line = line.replaceAll("〈","<");line = line.replaceAll("〉",">");line = line.replaceAll("χ","X");line = line.replaceAll("Đ","D");line = line.replaceAll("‰","%");line = line.replaceAll("·",".");line = line.replaceAll("→",">");line = line.replaceAll("←","<");line = line.replaceAll("ζ","z");line = line.replaceAll("π","p");line = line.replaceAll("τ","t");line = line.replaceAll("ξ","X");line = line.replaceAll("η","h");line = line.replaceAll("ø","0");line = line.replaceAll("Δ","D");line = line.replaceAll("∆","D");line = line.replaceAll("∑","S");line = line.replaceAll("Ω","O");line = line.replaceAll("δ","d");line = line.replaceAll("σ","s");
				String Anno[]=line.split("\t");
				String LocationInTree = Anno[0];
				String token = Anno[1];
				String type="";
				String identifier="";
				if(Anno.length>2)
				{
					type = Anno[2];
				}
				if(Anno.length>3)
				{
					identifier = Anno[3];
				}
				
				String LocationsInTree[]=LocationInTree.split("-");
				TreeNode tmp = Tr.root;
				for(int i=0;i<LocationsInTree.length-1;i++)
				{
					tmp=tmp.links.get(Integer.parseInt(LocationsInTree[i])-1);
				}
				if(type.equals("") && identifier.equals(""))
				{
					tmp.InsertToken(token,"");
				}
				else if(identifier.equals(""))
				{
					tmp.InsertToken(token,type);
				}
				else
				{
					tmp.InsertToken(token,type+"\t"+identifier);
				}
				count++;
			}
			inputfile.close();	
		}
		catch(IOException e1){ System.out.println("[TreeFile2Tee]: Input file is not exist.");}
	}
	
	/*
	 * Search target mention in the Prefix Tree
	 */
	public String MentionMatch(String Mentions,Integer PrefixTranslation, Integer Tok_NumCharPartialMatch)
	{
		ArrayList<String> location = new ArrayList<String>();
		String Menlist[]=Mentions.split("\\|");
		for(int m=0;m<Menlist.length;m++)
		{
			String Mention=Menlist[m];
			String Mention_lc=Mention.toLowerCase();
			Mention_lc = Mention_lc.replaceAll("[\\W\\-\\_]+", "");
			Mention_lc = Mention_lc.replaceAll("([0-9])([a-z])", "$1 $2");
			Mention_lc = Mention_lc.replaceAll("([a-z])([0-9])", "$1 $2");
			String Tkns[]=Mention_lc.split(" ");
			
			int i=0;
			boolean find=false;
			TreeNode tmp = Tr.root;
			String Concept="";
			while( i<Tkns.length && tmp.CheckChild(Tkns[i],i,Tok_NumCharPartialMatch,PrefixTranslation)>=0) //Find Tokens in the links
			{
				int CheckChild_Num=tmp.CheckChild(Tkns[i],i,Tok_NumCharPartialMatch,PrefixTranslation);
				int CheckChild_Num_tmp=CheckChild_Num;
				if(CheckChild_Num>=10000000){CheckChild_Num_tmp=CheckChild_Num-10000000;}
				tmp=tmp.links.get(CheckChild_Num_tmp); //move point to the link
				Concept = tmp.Concept;
				if(CheckChild_Num>=10000000){Concept="Species	_PartialMatch_";}
				find=true;
				i++;
			}
			if(find == true)
			{
				if(i==Tkns.length)
				{
					if(!Concept.equals(""))
					{
						return Concept;
					}
					else
					{
						return "-1"; //gene id is not found.
					}
				}
				else
				{
					return "-2"; //the gene mention matched a substring in PrefixTree.
				}
			}
			else
			{
				return "-3"; //mention is not found
			}
		}
		return "-3"; //mention is not found
	}
	
	/*
	 * Search target mention in the Prefix Tree
	 * ConceptType: Species|Genus|Cell|CTDGene
	 */
	public ArrayList<String> SearchMentionLocation(String Doc,String Doc_org,String ConceptType, Integer PrefixTranslation, Integer Tok_NumCharPartialMatch)
	{
		ArrayList<String> location = new ArrayList<String>();
		Doc=Doc.toLowerCase();
		String Doc_lc=Doc;
		Doc = Doc.replaceAll("([0-9])([A-Za-z])", "$1 $2");
		Doc = Doc.replaceAll("([A-Za-z])([0-9])", "$1 $2");
		Doc = Doc.replaceAll("[\\W^;:,]+", " ");
		
		String DocTkns[]=Doc.split(" ");
		int Offset=0;
		int Start=0;
		int Last=0;
		int FirstTime=0;
		
		while(Doc_lc.length()>0 && Doc_lc.substring(0,1).matches("[\\W]")) //clean the forward whitespace
		{
			Doc_lc=Doc_lc.substring(1);
			Offset++;
		}
		
		for(int i=0;i<DocTkns.length;i++)
		{
			TreeNode tmp = Tr.root;
			boolean find=false;
			int ConceptFound=i; //Keep found concept
			String ConceptFound_STR="";//Keep found concept
			int Tokn_num=0;
			String Concept="";
			while( tmp.CheckChild(DocTkns[i],Tokn_num,Tok_NumCharPartialMatch,PrefixTranslation)>=0 ) //Find Tokens in the links
			{
				int CheckChild_Num=tmp.CheckChild(DocTkns[i],Tokn_num,Tok_NumCharPartialMatch,PrefixTranslation);
				int CheckChild_Num_tmp=CheckChild_Num;
				if(CheckChild_Num>=10000000){CheckChild_Num_tmp=CheckChild_Num-10000000;}
				tmp=tmp.links.get(CheckChild_Num_tmp); //move point to the link
				Concept = tmp.Concept;
				if(CheckChild_Num>=10000000){Concept="Species	_PartialMatch_";}
				
				if(Start==0 && FirstTime>0){Start = Offset;} //Start <- Offset 
				if(Doc_lc.length()>=DocTkns[i].length() && Doc_lc.substring(0,DocTkns[i].length()).equals(DocTkns[i]))
				{
					if(DocTkns[i].length()>0)
					{
						Doc_lc=Doc_lc.substring(DocTkns[i].length());
						Offset=Offset+DocTkns[i].length();
					}
				}
				Last = Offset;
				while(Doc_lc.length()>0 && Doc_lc.substring(0,1).matches("[\\W]")) //clean the forward whitespace
				{
					Doc_lc=Doc_lc.substring(1);
					Offset++;
				}
				i++;
				Tokn_num++;
				
				if(ConceptType.equals("Species"))
				{
					if(i<DocTkns.length-2 && DocTkns[i].matches("(str|strain|substr|substrain|subspecies|subsp|var|variant|pathovars|pv|biovar|bv)"))
					{
						Doc_lc=Doc_lc.substring(DocTkns[i].length());
						Offset=Offset+DocTkns[i].length();
						Last = Offset;
						while(Doc_lc.length()>0 && Doc_lc.substring(0,1).matches("[\\W]")) //clean the forward whitespace
						{
							Doc_lc=Doc_lc.substring(1);
							Offset++;
						}
						i++;
					}
				}
				
				if(!Concept.equals("") && (Last-Start>0)) //Keep found concept
				{
					ConceptFound=i;
					ConceptFound_STR=Start+"\t"+Last+"\t"+Doc_org.substring(Start, Last)+"\t"+Concept;
				}
				
				find=true;
				if(i>=DocTkns.length){break;}
				//else if(i==DocTkns.length-1){PrefixTranslation=2;}
			}
			
			if(find == true)
			{
				if(!Concept.equals("") && (Last-Start>0)) //the last matched token has concept id 
				{
					location.add(Start+"\t"+Last+"\t"+Doc_org.substring(Start, Last)+"\t"+Concept);
					
				}
				else if(!ConceptFound_STR.equals("")) //Keep found concept
				{
					location.add(ConceptFound_STR);
					i = ConceptFound + 1;
				}
				Start=0;
				Last=0;
				if(i>0){i--;}
				ConceptFound=i; //Keep found concept
				ConceptFound_STR="";//Keep found concept
			}
			else //if(find == false)
			{
				if(Doc_lc.length()>=DocTkns[i].length() && Doc_lc.substring(0,DocTkns[i].length()).equals(DocTkns[i]))
				{
					if(DocTkns[i].length()>0)
					{
						Doc_lc=Doc_lc.substring(DocTkns[i].length());
						Offset=Offset+DocTkns[i].length();
					}
				}
			}
			
			while(Doc_lc.length()>0 && Doc_lc.substring(0,1).matches("[\\W]")) //clean the forward whitespace
			{
				Doc_lc=Doc_lc.substring(1);
				Offset++;
			}
			FirstTime++;
		}
		return location;
	}
	
	/*
	 * Print out the Prefix Tree
	 */
	public String PrintTree()
	{
		return Tr.PrintTree_preorder(Tr.root,"");
	}
}

class Tree 
{
	/*
	 * Prefix Tree - root node
	 */
	public TreeNode root;
	
	public Tree() 
	{ 
		root = new TreeNode("-ROOT-"); 
	}
	
	/*
	 * Insert mention into the tree
	 */
	public void insertMention(String Mention, String Identifier)
	{
		Mention=Mention.toLowerCase();
		Identifier = Identifier.replaceAll("\t$", "");
		Mention = Mention.replaceAll("([0-9])([A-Za-z])", "$1 $2");
		Mention = Mention.replaceAll("([A-Za-z])([0-9])", "$1 $2");
		Mention = Mention.replaceAll("[\\W\\-\\_]+", " ");

		String Tokens[]=Mention.split(" ");
		TreeNode tmp = root;
		for(int i=0;i<Tokens.length;i++)
		{
			if(tmp.CheckChild(Tokens[i],i,0,0)>=0)
			{
				tmp=tmp.links.get( tmp.CheckChild(Tokens[i],i,0,0) ); //go through next generation (exist node)
				if(i == Tokens.length-1)
				{
					tmp.Concept=Identifier;
				}
			}
			else //not exist
			{
				if(i == Tokens.length-1)
				{
					tmp.InsertToken(Tokens[i],Identifier);
				}
				else
				{
					tmp.InsertToken(Tokens[i]);
				}
				tmp=tmp.links.get(tmp.NumOflinks-1); //go to the next generation (new node)
			}
		}
	}
	
	/*
	 * Print the tree by pre-order
	 */
	public String PrintTree_preorder(TreeNode node, String LocationInTree)
	{
		String opt="";
		if(!node.token.equals("-ROOT-"))//Ignore root
		{
			if(node.Concept.equals(""))
			{
				opt=opt+LocationInTree+"\t"+node.token+"\n";
			}
			else
			{
				opt=opt+LocationInTree+"\t"+node.token+"\t"+node.Concept+"\n";
			}
		} 
		if(!LocationInTree.equals("")){LocationInTree=LocationInTree+"-";}
		for(int i=0;i<node.NumOflinks;i++)
		{
			opt=opt+PrintTree_preorder(node.links.get(i),LocationInTree+(i+1));
		}
		return opt;
	}
}

class TreeNode 
{
	String token; //token of the node
	int NumOflinks; //Number of links
	public String Concept;
	ArrayList<TreeNode> links;
	
	public TreeNode(String Tok,String ID)
	{
		token = Tok;
		NumOflinks = 0;
		Concept = ID;
		links = new ArrayList<TreeNode>();
	}
	public TreeNode(String Tok)
	{
		token = Tok;
		NumOflinks = 0;
		Concept = "";
		links = new ArrayList<TreeNode>();
	}
	public TreeNode()
	{
		token = "";
		NumOflinks = 0;
		Concept = "";
		links = new ArrayList<TreeNode>();
	}
	
	/*
	 * Insert an new node under the target node
	 */
	public void InsertToken(String Tok)
	{
		TreeNode NewNode = new TreeNode(Tok);
		links.add(NewNode);
		NumOflinks++;
	}
	public void InsertToken(String Tok,String ID)
	{
		TreeNode NewNode = new TreeNode(Tok,ID);
		links.add(NewNode);
		NumOflinks++;
	}
	
	/*
	 * Check the tokens of children
	 * PrefixTranslation = 1 (SuffixTranslationMap)
	 * PrefixTranslation = 2 (CTDGene; partial match for numbers)
	 * PrefixTranslation = 3 (NCBI Taxonomy usage (IEB) : suffix partial match)
	 */
	public int CheckChild(String Tok,Integer Tok_num,Integer Tok_NumCharPartialMatch, Integer PrefixTranslation)
	{
		/** Suffix Translation */
		ArrayList<String> SuffixTranslationMap = new ArrayList<String>();
		SuffixTranslationMap.add("alpha-a");
		SuffixTranslationMap.add("alpha-1");
		SuffixTranslationMap.add("a-alpha");
		//SuffixTranslationMap.add("a-1");
		SuffixTranslationMap.add("1-alpha");
		//SuffixTranslationMap.add("1-a");
		SuffixTranslationMap.add("beta-b");
		SuffixTranslationMap.add("beta-2");
		SuffixTranslationMap.add("b-beta");
		//SuffixTranslationMap.add("b-2");
		SuffixTranslationMap.add("2-beta");
		//SuffixTranslationMap.add("2-b");
		SuffixTranslationMap.add("gamma-g");
		SuffixTranslationMap.add("gamma-y");
		SuffixTranslationMap.add("g-gamma");
		SuffixTranslationMap.add("y-gamma");
		SuffixTranslationMap.add("1-i");
		SuffixTranslationMap.add("i-1");
		SuffixTranslationMap.add("2-ii");
		SuffixTranslationMap.add("ii-2");
		SuffixTranslationMap.add("3-iii");
		SuffixTranslationMap.add("iii-3");
		SuffixTranslationMap.add("4-vi");
		SuffixTranslationMap.add("vi-4");
		SuffixTranslationMap.add("5-v");
		SuffixTranslationMap.add("v-5");
		
		for(int i=0;i<links.size();i++)
        {
			if(Tok.equals(links.get(i).token))
			{
				return (i);
			}
        }
		
		if(PrefixTranslation == 1 && Tok.matches("(alpha|beta|gamam|[abg]|[12])")) // SuffixTranslationMap
		{
			for(int i=0;i<links.size();i++)
	        {
				if(SuffixTranslationMap.contains(Tok+"-"+links.get(i).token))
				{
					return(i);
				}
	        }
		}
		else if(PrefixTranslation == 2 && Tok.matches("[1-5]")) // for CTDGene feature
		{
			for(int i=0;i<links.size();i++)
	        {
				if(links.get(i).token.matches("[1-5]"))
				{
					return(i);
				}
	        }
		}
		else if(PrefixTranslation == 3 && Tok.length()>=Tok_NumCharPartialMatch && Tok_num>=1) // for NCBI Taxonomy usage (IEB) : suffix partial match
		{
			for(int i=0;i<links.size();i++)
	        {
				if(links.get(i).token.length()>=Tok_NumCharPartialMatch)
				{
					String Tok_Prefix=Tok.substring(0,Tok_NumCharPartialMatch);
					if((!links.get(i).Concept.equals("")) && links.get(i).token.substring(0,Tok_NumCharPartialMatch).equals(Tok_Prefix))
					{
						return(i+10000000);
					}
				}
	        }
		}
		
		return(-1);
	}
}
	