package tmVarlib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.BreakIterator;

public class PostProcessing 
{

	public void toME(String Filename,String FilenameOutput,String FilenameLoca,String FilenameME)throws IOException 
	{
		HashMap<String,String> sentence_hash = new HashMap<String,String>();
		HashMap<String,String> article_hash = new HashMap<String,String>();
		HashMap<String,String> TypeOrder_hash = new HashMap<String,String>();
		ArrayList<String> pmidARR = new ArrayList<String>(); 
		
		try {
			/*
			 * load input sentences
			 */
			int ParagraphType_count=0;
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			String line;
			while ((line = inputfile.readLine()) != null)  
			{
				if(line.contains("|")) //Title|Abstract
	        	{
					String Pmid="";
					String ParagraphType="";
					String ParagraphContent="";
					Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
					Matcher mat = pat.matcher(line);
					if(mat.find()) //Title|Abstract
		        	{
						Pmid = mat.group(1);
						ParagraphType=mat.group(2);
						ParagraphContent=mat.group(3);
						//if(ParagraphContent.equals(""))
						//{
						//	ParagraphContent="- No text -";
						//}
					}
					sentence_hash.put(Pmid+"\t"+ParagraphType+"\t"+ParagraphType_count,ParagraphContent);
					if(article_hash.get(Pmid) != null)
					{
						article_hash.put(Pmid,article_hash.get(Pmid)+" "+ParagraphContent);
					}
					else
					{
						article_hash.put(Pmid,ParagraphContent);
					}
					if(TypeOrder_hash.get(Pmid) != null)
					{
						TypeOrder_hash.put(Pmid,TypeOrder_hash.get(Pmid)+"\t"+ParagraphType+"|"+ParagraphType_count);
					}
					else
					{
						TypeOrder_hash.put(Pmid,ParagraphType+"|"+ParagraphType_count);
					}
					if(!pmidARR.contains(Pmid))
					{
						pmidARR.add(Pmid);
					}
					ParagraphType_count++;
	        	}
			}
			inputfile.close();
			
			/*
			 * load CRF output
			 */
			ArrayList<String> outputArr = new ArrayList<String>(); 
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenameOutput), "UTF-8"));
			while ((line = inputfile.readLine()) != null)  
			{
				outputArr.add(line);
			}
			inputfile.close();
			
			/*
			 * load location
			 */
			ArrayList<String> locationArr = new ArrayList<String>(); 
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenameLoca), "UTF-8"));
			while ((line = inputfile.readLine()) != null)  
			{
				locationArr.add(line);
			}
			inputfile.close();
			
			BufferedWriter FileME = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenameME), "UTF-8"));
			
			String pmid="";
			for(int i=0;i<outputArr.size();i++)
			{
				String outputs[]=outputArr.get(i).split("\\t");
				/*
				 * Extract the states and tokens from CRF++ output file
				 */
				int start=100000000;
				int last=0;
				String mention="";
				String location[]=locationArr.get(i).split("\\t");
				HashMap<String,String> component_hash = new HashMap<String,String>();
				if(component_hash.get("A") == null){component_hash.put("A", "");}
				if(component_hash.get("T") == null){component_hash.put("T", "");}
				if(component_hash.get("P") == null){component_hash.put("P", "");}
				if(component_hash.get("W") == null){component_hash.put("W", "");}
				if(component_hash.get("M") == null){component_hash.put("M", "");}
				if(component_hash.get("F") == null){component_hash.put("F", "");}
				if(component_hash.get("S") == null){component_hash.put("S", "");}
				if(component_hash.get("D") == null){component_hash.put("D", "");}
				if(component_hash.get("I") == null){component_hash.put("I", "");}
				if(component_hash.get("R") == null){component_hash.put("R", "");}
				
				// print title/abstracts
				if(location.length > 1 && !pmid.equals(location[0]))
				{
					if(!pmid.equals("")){FileME.write("\n");}
					pmid=location[0];
					String TypeOrder[]=TypeOrder_hash.get(pmid).split("\\t");
					for(int j=0;j<TypeOrder.length;j++)
					{
						String[] TypeOrder_component=TypeOrder[j].split("\\|");
						FileME.write(pmid+"|"+TypeOrder_component[0]+"|"+sentence_hash.get(pmid+"\t"+TypeOrder_component[0]+"\t"+TypeOrder_component[1])+"\n");
					}
				}
				
				//find mention
				Pattern pat = Pattern.compile("([ATPWMFSDIR])$");
				Matcher mat = pat.matcher(outputs[outputs.length-1]);
				if((!outputs[0].equals(";")) && mat.find())
				{
					String prestate="";
					mat = pat.matcher(outputs[outputs.length-1]);
					
					//until the end token of the mention
					while((!outputs[0].equals(";")) && mat.find())
					{
						String state=mat.group(1);	
						String locationWhile[]=locationArr.get(i).split("\\t");
						String tkn=locationWhile[1];
						pmid=locationWhile[0];
						int start_tmp=Integer.parseInt(locationWhile[2]);
						int last_tmp=Integer.parseInt(locationWhile[3]);
						mention = mention + tkn;
						if(!component_hash.get(state).equals("") && !state.equals(prestate))
						{
							component_hash.put(state, component_hash.get(state)+","+tkn);
						}
						else
						{
							component_hash.put(state, component_hash.get(state)+tkn);
						}
						if(start_tmp<start){start=start_tmp;}
						if(last_tmp>last){last=last_tmp;}
						prestate=state;
						i++;
						outputs=outputArr.get(i).split("\\t");
						mat = pat.matcher(outputs[outputs.length-1]);
					}
					
					if(mention.length()>200)
					{
						// remove the mention
					}
					else
					{
						/*
						 * Recognize the components(identifiers)
						 */
						String identifier;
						String type="";
						if(!component_hash.get("D").equals(""))
						{
							identifier=component_hash.get("A")+"|"+component_hash.get("T")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|"+component_hash.get("D");
						}
						else if(!component_hash.get("T").equals(""))
						{
							identifier=component_hash.get("A")+"|"+component_hash.get("T")+"|"+component_hash.get("P")+"|"+component_hash.get("M");
						}
						else if(!component_hash.get("S").equals(""))
						{
							identifier=component_hash.get("A")+"|"+component_hash.get("W")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|"+component_hash.get("F")+"|"+component_hash.get("S");
							type="ProteinMutation";
						}
						else if(!component_hash.get("F").equals(""))
						{
							identifier=component_hash.get("A")+"|"+component_hash.get("W")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|"+component_hash.get("F");
							type="ProteinMutation";
						}
						else if(!component_hash.get("R").equals(""))
						{
							identifier=mention;
							type="SNP";
						}
						else
						{
							identifier=component_hash.get("A")+"|"+component_hash.get("W")+"|"+component_hash.get("P")+"|"+component_hash.get("M");
						}
						
						/*
						 * Recognize the type of mentions: ProteinMutation | DNAMutation | SNP 
						 */
						if(type.equals(""))
						{
							if(!component_hash.get("A").equals("") &&
							   (
							   component_hash.get("A").toLowerCase().equals("c") ||
							   component_hash.get("A").toLowerCase().equals("r") ||
							   component_hash.get("A").toLowerCase().equals("m") ||
							   component_hash.get("A").toLowerCase().equals("g")
							   )
							  )
							{
								type="DNAMutation";
							}
							else if(!component_hash.get("A").equals("") && component_hash.get("A").toLowerCase().equals("p"))
							{
								type="ProteinMutation";
							}
							else if(!component_hash.get("T").equals("") && component_hash.get("T").toLowerCase().equals("delta"))
							{
								type="DNAMutation";
							}
							else if(
									!component_hash.get("P").equals("") && 
									(
									component_hash.get("P").toLowerCase().equals("ex") ||
									component_hash.get("P").toLowerCase().equals("intron") ||
									component_hash.get("P").toLowerCase().equals("ivs")
									)
								   )
							{
								type="DNAMutation";
							}
							else if((!component_hash.get("T").equals("")) && (!component_hash.get("M").matches(".*[^ATCGYU].*")))
							{
								type="DNAMutation";
								
							}
							else if(!component_hash.get("M").equals("") && !component_hash.get("W").equals("")) //others
							{
								pat = Pattern.compile("^[ATCGatcgu]+$");
								Matcher mat_M = pat.matcher(component_hash.get("M"));
								Matcher mat_W = pat.matcher(component_hash.get("W"));
								
								if(!mat_M.find() || !mat_W.find())
								{
									type="ProteinMutation";
								}
								else //default
								{
									type="DNAMutation";
								}
							}
						}
						
						if(type.equals(""))
						{
							type="ProteinMutation";
						}
						
						/*
						 * filtering and Print out
						 */
						boolean show_removed_cases=false;
						if( (component_hash.get("W").length() == 3 || component_hash.get("M").length() == 3) 
								&& component_hash.get("W").length() != component_hash.get("M").length()
								&& !component_hash.get("W").equals("") && !component_hash.get("M").equals("") && component_hash.get("W").indexOf(",")!=-1 && component_hash.get("M").indexOf(",")!=-1
								&& ((component_hash.get("W").indexOf("A")!=-1 && component_hash.get("W").indexOf("T")!=-1 && component_hash.get("W").indexOf("C")!=-1 && component_hash.get("W").indexOf("G")!=-1) || (component_hash.get("M").indexOf("A")!=-1 && component_hash.get("M").indexOf("T")!=-1 && component_hash.get("M").indexOf("C")!=-1 && component_hash.get("M").indexOf("G")!=-1))
								&& component_hash.get("T").equals("")
							)
								{if(show_removed_cases==true){System.out.println("filtering 1:"+article_hash.get(pmid).substring(start-1,last));}}
						else if((component_hash.get("M").matches("[ISQMNPKDFHLRWVEYX]") || component_hash.get("W").matches("[ISQMNPKDFHLRWVEYX]")) && component_hash.get("P").matches("[6-9][0-9][0-9][0-9]+")){if(show_removed_cases==true){System.out.println("filtering 2:"+article_hash.get(pmid).substring(start-1,last));}} //length > 3000, if protein mutation
						else if(component_hash.get("M").equals("") && component_hash.get("T").equals("") && component_hash.get("F").equals("") && component_hash.get("P").equals("") && !type.equals("SNP")){} //Arg235
						else if(component_hash.get("W").equals("") && component_hash.get("T").equals("") && component_hash.get("F").equals("") && component_hash.get("D").equals("") && (!component_hash.get("M").equals("")) && !type.equals("SNP")){if(show_removed_cases==true){System.out.println("filtering 2.2:"+article_hash.get(pmid).substring(start-1,last));}} //314C
						else if(component_hash.get("M").equals("") && component_hash.get("T").equals("") && component_hash.get("F").equals("")&& component_hash.get("D").equals("") && (!component_hash.get("W").equals("")) && !type.equals("SNP")){if(show_removed_cases==true){System.out.println("filtering 2.2:"+article_hash.get(pmid).substring(start-1,last));}} //C314
						else if(component_hash.get("T").toLowerCase().equals("delta") && (!component_hash.get("M").matches("[ATCG0-9]*"))  && component_hash.get("P").equals("")){if(show_removed_cases==true){System.out.println("filtering 3:"+article_hash.get(pmid).substring(start-1,last));}} //DeltaEVIL
						else if(component_hash.get("T").toLowerCase().equals("delta") && component_hash.get("M").equals("")  && component_hash.get("P").equals("")){if(show_removed_cases==true){System.out.println("filtering 3:"+article_hash.get(pmid).substring(start-1,last));}} //Delta
						else if(component_hash.get("P").matches("^-") && type.equals("ProteinMutation")){if(show_removed_cases==true){System.out.println("filtering 4:"+article_hash.get(pmid).substring(start-1,last));}} //negative protein mutation
						else if(component_hash.get("W").matches("^[BJOUZ]") || component_hash.get("M").matches("^[BJOUZ]")){if(show_removed_cases==true){System.out.println("filtering 5:"+article_hash.get(pmid).substring(start-1,last));}} //not a mutation
						else if(component_hash.get("W").matches("^[A-Za-z][a-z]") || component_hash.get("M").matches("^[A-Za-z][a-z]") ){if(show_removed_cases==true){System.out.println("filtering 7:"+article_hash.get(pmid).substring(start-1,last));}} //not a mutation
						else if( component_hash.get("W").matches("[A-Za-z]") && component_hash.get("M").matches("[A-Za-z][a-z][a-z]+") && (!tmVar.nametothree.containsKey(component_hash.get("M").toUpperCase())) && (!tmVar.threetone.containsKey(component_hash.get("M").toUpperCase())) ){} //T-->infinity
						else if( component_hash.get("M").matches("[A-Za-z]") && component_hash.get("W").matches("[A-Za-z][a-z][a-z]+") && (!tmVar.nametothree.containsKey(component_hash.get("W").toUpperCase())) && (!tmVar.threetone.containsKey(component_hash.get("W").toUpperCase())) ){} //T-->infinity
						else if(article_hash.get(pmid).length()>start-1 && start>15 && article_hash.get(pmid).substring(start-15,start-1).matches(".*(Figure|Figures|Table|Fig|Figs|Tab|Tabs|figure|figures|table|fig|figs|tab|tabs)[ \\.].*")){if(show_removed_cases==true){System.out.println("filtering 6.1:"+start+"\t"+last+"\t"+article_hash.get(pmid).substring(start-1,last));}} //Figure S13A
						else if(article_hash.get(pmid).length()>last+1 && article_hash.get(pmid).substring(last,last+1).matches("[A-Za-z0-9\\>]") && !type.equals("SNP")){if(show_removed_cases==true){System.out.println("filtering 7:"+article_hash.get(pmid).substring(start-1,last));}} //V79 Chinese
						else if(mention.matches(".+\\).+\\(.+")){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //Arg)3(Ser
						else if(mention.matches(".+\\>.+\\>.+")){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //G>T>C
						else if(mention.matches("[A-Z][a-z][a-z][0-9]+,[A-Z][a-z][a-z][0-9]+")){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //Glu207, Thr209
						else if(mention.matches("[A-Z][a-z][a-z][0-9]+,[A-Z][a-z][a-z][0-9]+,[A-Z][a-z][a-z][0-9]+")){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //Glu207, Thr209, Gly211
						else if(mention.matches(".*[gG]\\/L.*")){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //450 G/L
						else if(tmVar.PAM_lowerScorePair.contains(component_hash.get("M")+"\t"+component_hash.get("W"))){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //unlikely to occur
						else if(component_hash.get("W").length()==3 && (!tmVar.threetone.containsKey(component_hash.get("W").toUpperCase()) && !tmVar.threetone_nu.containsKey(component_hash.get("W").toUpperCase()))){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention+"\tW:"+component_hash.get("W"));}} //The 100 Top
						else if(component_hash.get("M").length()==3 && (!tmVar.threetone.containsKey(component_hash.get("M").toUpperCase()) && !tmVar.threetone_nu.containsKey(component_hash.get("M").toUpperCase()))){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention+"\tW:"+component_hash.get("M"));}} //The 100 Top
						else if(mention.length()>200){if(show_removed_cases==true){System.out.println("filtering - Mention:"+mention);}} //too long
						else if(article_hash.get(pmid).length()>last)
						{
							String men=article_hash.get(pmid).substring(start-1,last);
							Pattern pat_cp = Pattern.compile("(c\\..*) (p\\..*)");
							Pattern pat_pc = Pattern.compile("(p\\..*) (c\\..*)");
							Matcher mat_cp = pat_cp.matcher(men);
							Matcher mat_pc = pat_pc.matcher(men);
							if(mat_cp.find()) //Title|Abstract
				        	{
								String men_c = mat_cp.group(1);
								String men_p = mat_cp.group(2);
								int start_c=start-1;
								int last_c=start_c+men_c.length();
								int start_p=last_c+1;
								int last_p=start_p+men_p.length();
								FileME.write(pmid+"\t"+start_c+"\t"+last_c+"\t"+men_c+"\tDNAMutation\t"+identifier+"\n");
								FileME.write(pmid+"\t"+start_p+"\t"+last_p+"\t"+men_p+"\tProteinMutation\t"+identifier+"\n");
							}
							else if(mat_pc.find()) //Title|Abstract
				        	{
								String men_p = mat_pc.group(1);
								String men_c = mat_pc.group(2);
								int start_p=start-1;
								int last_p=start_p+men_p.length();
								int start_c=last_p+1;
								int last_c=start_c+men_c.length();
								FileME.write(pmid+"\t"+start_p+"\t"+last_p+"\t"+men_p+"\tProteinMutation\t"+identifier+"\n");
								FileME.write(pmid+"\t"+start_c+"\t"+last_c+"\t"+men_c+"\tDNAMutation\t"+identifier+"\n");
							}
							else
							{
								FileME.write(pmid+"\t"+(start-1)+"\t"+last+"	"+article_hash.get(pmid).substring(start-1,last)+"\t"+type+"\t"+identifier+"\n");
							}
						}
					}
					if(!outputs[outputs.length-1].equals("O"))
					{
						i--;
					}
				}
			}
			FileME.write("\n");
			FileME.close();
		}
		catch(IOException e1){ System.out.println("[toME]: Input file is not exist.");}
	}

	public void toPostME(String FilenameME,String FilenamePostME)throws IOException
	{
		Pattern Pattern_Component_1 = Pattern.compile("^([RrSs][Ss][ ]*[0-9]+)[ ]*(and|/|,|or)[ ]*([RrSs][Ss][ ]*[0-9]+)$");
		Pattern Pattern_Component_2 = Pattern.compile("^(.*[^0-9])[ ]*([RrSs][Ss][ ]*[0-9]+)$");
		Pattern Pattern_Component_3 = Pattern.compile("^([RrSs][Ss][ ]*[0-9]+)[ ]*([^0-9].*)$");
		HashMap<String,HashMap<String,String>> Temporary_Pattern_Allele = new HashMap<String,HashMap<String,String>>();
		
		try {
			BufferedWriter FilePostME = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenamePostME), "UTF-8")); // .location
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenameME), "UTF-8"));
			ArrayList<String> Annotation = new ArrayList<String>();
			String article="";
			String Pmid="";
			String line="";
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					Pmid = mat.group(1);
					String ParagraphContent=mat.group(3);
					article=article+ParagraphContent+"\t";
					FilePostME.write(line+"\n");
					Temporary_Pattern_Allele.put(Pmid,new HashMap<String,String>());
				}
				else if (line.contains("\t")) //Annotation
		    	{
					String anno[]=line.split("\t");
					Pmid=anno[0];
					String mention=anno[3];
					String type=anno[4];
					Annotation.add(line);
		    	}
				else if(line.length()==0) //Processing
				{
					ArrayList<Integer> RemoveAnno = new ArrayList<Integer>();
					
					/*
					 * Split RSnumber
					 */
					for(int i=0;i<Annotation.size();i++)
					{
						String anno[]=Annotation.get(i).split("\t");
						
						Pmid=anno[0];
						int start = Integer.parseInt(anno[1]);
	        			int last = Integer.parseInt(anno[2]);
	        			String mention = anno[3];
	        			String type = anno[4];
	        			
	        			if(!tmVar.variant_mention_to_filter_overlap_gene.containsKey(Pmid))
	        			{
	        				tmVar.variant_mention_to_filter_overlap_gene.put(Pmid, new HashMap<Integer,String>());
	        			}
	        			for(int s=start;s<=last;s++)
	        			{
	        				tmVar.variant_mention_to_filter_overlap_gene.get(Pmid).put(s,"");
	        			}
	        			/**
	        			 The common combine tokens: 
		        			 m1: "rs123456 and rs234567"
		        			 m2: "A134T rs234567"
		        			 m3: "rs234567 A134T"
	        			 */
	        			Matcher m1 = Pattern_Component_1.matcher(mention);
	        			Matcher m2 = Pattern_Component_2.matcher(mention);
	        			Matcher m3 = Pattern_Component_3.matcher(mention);
	        			if(m1.find())
	        			{
	        				String sub_mention1=m1.group(1);
	        				String sub_mention2=m1.group(3);
	        				int start1=start;
	        				int last1=start+sub_mention1.length();
	        				int start2=last-sub_mention2.length();
	        				int last2=last;
	        				RemoveAnno.add(i);
	        				Annotation.add(Pmid+"\t"+start1+"\t"+last1+"\t"+sub_mention1+"\tSNP\t"+sub_mention1);
	        				Annotation.add(Pmid+"\t"+start2+"\t"+last2+"\t"+sub_mention2+"\tSNP\t"+sub_mention2);
	        			}
	        			else if(m2.find())
	        			{
	        				String sub_mention1=m2.group(1);
	        				String sub_mention2=m2.group(2);
	        				int start1=start;
	        				int last1=start+sub_mention1.length();
	        				int start2=last-sub_mention2.length();
	        				int last2=last;
	        				RemoveAnno.add(i);
	        				Annotation.add(Pmid+"\t"+start1+"\t"+last1+"\t"+sub_mention1+"\tDNAMutation\t"+sub_mention1);
	        				Annotation.add(Pmid+"\t"+start2+"\t"+last2+"\t"+sub_mention2+"\tSNP\t"+sub_mention2);
	        			}
	        			else if(m3.find())
	        			{
	        				String sub_mention1=m3.group(1);
	        				String sub_mention2=m3.group(2);
	        				int start1=start;
	        				int last1=start+sub_mention1.length();
	        				int start2=last-sub_mention2.length();
	        				int last2=last;
	        				RemoveAnno.add(i);
	        				Annotation.add(Pmid+"\t"+start1+"\t"+last1+"\t"+sub_mention1+"\tSNP\t"+sub_mention1);
	        				Annotation.add(Pmid+"\t"+start2+"\t"+last2+"\t"+sub_mention2+"\tDNAMutation\t"+sub_mention2);
	        			}
	        		}
					for(int i=RemoveAnno.size()-1;i>=0;i--)
					{
						int RemA=RemoveAnno.get(i);
						Annotation.remove(RemA);
					}
					
					/*
					 * Boundary
					 */
					for(int i=0;i<Annotation.size();i++)
					{
						String anno[]=Annotation.get(i).split("\t",-1);
						int start = Integer.parseInt(anno[1]);
	        			int last = Integer.parseInt(anno[2]);
	        			String mention = anno[3];
	        			String type = anno[4];
	        			String identifier = anno[5];
	        			int check=0;
	        			
	        			String identifier_component[]=identifier.split("\\|",-1);
	        			if(identifier_component.length>=3)
	        			{
	        				if(type.equals("DNAMutation"))
	        				{
	        					String position=identifier_component[2];
	        					String W=identifier_component[1];
		        				String M=identifier_component[3];
		        				if(position.matches("[IVS\\+0-9\\*\\- ]+"))
	        					{
		        					position=position.replaceAll("\\*","\\\\*");
		        					position=position.replaceAll("\\+","\\\\+");
		        					position=position.replaceAll("[\\(\\)\\[\\]\\{\\}]","");
			        				W=W.replaceAll("[\\W\\-\\_]","");
			        				M=M.replaceAll("[\\W\\-\\_]","");
		        					Temporary_Pattern_Allele.get(Pmid).put("([cg]\\.|)([ATCGU]+|[Gg]uanine|[Aa]denine|[Aa]denosine|[Tt]hymine|[Tt]hymidine|[Cc]ytosine|[Cc]ytidine|[Uu]racil|[Uu]ridine)[\\+ ]*"+position,"DNAAllele");
		        					Temporary_Pattern_Allele.get(Pmid).put("([cg]\\.|)"+position+"[ ]*([ATCGU]+|[Gg]uanine|[Aa]denine|[Aa]denosine|[Tt]hymine|[Tt]hymidine|[Cc]ytosine|[Cc]ytidine|[Uu]racil|[Uu]ridine)","DNAAllele");
		        					Temporary_Pattern_Allele.get(Pmid).put("([cg]\\.|)"+W+"[\\- ]*"+position+"[\\- ]*"+W,"DNAAllele");
	        						Temporary_Pattern_Allele.get(Pmid).put("([cg]\\.|)"+M+"[\\- ]*"+position+"[\\- ]*"+M,"DNAAllele");
	        					}
	        					if(W.length()>0 && W.length()==M.length() && !W.toLowerCase().matches("(del|ins|dup|indel).*") )
	        					{
	        						if((W.matches("[A-Za-z]+")) && (M.matches("[A-Za-z]+")))
	        						{
	        							Temporary_Pattern_Allele.get(Pmid).put(W+"[\\- \\/]+"+M,"DNAAcidChange");
	        							Temporary_Pattern_Allele.get(Pmid).put(M+"[\\- \\/]+"+W,"DNAAcidChange");
	        						}
	        					}
	        				}
	        				else if(type.equals("ProteinMutation"))
	        				{
	        					String position=identifier_component[2];
	        					String W=identifier_component[1];
		        				String M=identifier_component[3];
		        				if(position.matches("[IVS\\+0-9\\*\\- ]+"))
		        				{
	        						position=position.replaceAll("\\*","\\\\*");
	        						position=position.replaceAll("\\+","\\\\+");
	        						position=position.replaceAll("[\\(\\)\\[\\]\\{\\}]","");
			        				W=W.replaceAll("[\\W\\-\\_]","");
			        				M=M.replaceAll("[\\W\\-\\_]","");
		        					Temporary_Pattern_Allele.get(Pmid).put("(p\\.|)([ATCGU][ATCGU][ATCGU]|Cys|Ile|Ser|Gln|Met|Asn|Pro|Lys|Asp|Thr|Phe|Ala|Gly|His|Leu|Arg|Trp|Val|Glu|Tyr|CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR|CYs|ILe|SEr|GLn|MEt|ASn|PRo|LYs|ASp|THr|PHe|ALa|GLy|HIs|LEu|ARg|TRp|VAl|GLu|TYr|[gG]lutamine|[Gg]lutamic acid|[Ll]eucine|[Vv]aline|[Ii]soleucine|[Ll]ysine|[Aa]lanine|[Gg]lycine|[Aa]spartate|[Mm]ethionine|[Tt]hreonine|[Hh]istidine|[Aa]spartic acid|[Aa]rginine|[Aa]sparagine|[Tt]ryptophan|[Pp]roline|[Pp]henylalanine|[Cc]ysteine|[Ss]erine|[Gg]lutamate|[Tt]yrosine|[Ss]top|[Tt]erm|[Ss]top codon|[Tt]ermination codon|[Tt]ermination|[CISQMNPKDTFAGHLRWVEYXU])[\\+ ]*"+position,"ProteinAllele");
		        					Temporary_Pattern_Allele.get(Pmid).put("(p\\.|)"+position+"[ ]*([ATCGU][ATCGU][ATCGU]|Cys|Ile|Ser|Gln|Met|Asn|Pro|Lys|Asp|Thr|Phe|Ala|Gly|His|Leu|Arg|Trp|Val|Glu|Tyr|CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR|CYs|ILe|SEr|GLn|MEt|ASn|PRo|LYs|ASp|THr|PHe|ALa|GLy|HIs|LEu|ARg|TRp|VAl|GLu|TYr|[gG]lutamine|[Gg]lutamic acid|[Ll]eucine|[Vv]aline|[Ii]soleucine|[Ll]ysine|[Aa]lanine|[Gg]lycine|[Aa]spartate|[Mm]ethionine|[Tt]hreonine|[Hh]istidine|[Aa]spartic acid|[Aa]rginine|[Aa]sparagine|[Tt]ryptophan|[Pp]roline|[Pp]henylalanine|[Cc]ysteine|[Ss]erine|[Gg]lutamate|[Tt]yrosine|[Ss]top|[Tt]erm|[Ss]top codon|[Tt]ermination codon|[Tt]ermination|[CISQMNPKDTFAGHLRWVEYXU])","ProteinAllele");
		        					Temporary_Pattern_Allele.get(Pmid).put("(p\\.|)"+W+"[\\- ]*"+position+"[\\- ]*"+W,"ProteinAllele");
	        						Temporary_Pattern_Allele.get(Pmid).put("(p\\.|)"+M+"[\\- ]*"+position+"[\\- ]*"+M,"ProteinAllele");
	        					}
	        					if(W.length()>0 && W.length()==M.length() && !W.toLowerCase().matches("(del|ins|dup|indel).*") )
	        					{
	        						if((W.matches("[A-Za-z]+")) && (M.matches("[A-Za-z]+")))
	        						{
	        							Temporary_Pattern_Allele.get(Pmid).put(W+"[\\- \\/]+"+M,"ProteinAcidChange");
	        							Temporary_Pattern_Allele.get(Pmid).put(M+"[\\- \\/]+"+W,"ProteinAcidChange");
	        						}
	        					}
	        				}
	        			}
	        			if(mention.length()>0)
	        			{
		        			if(mention.matches("^[0-9]") && article.substring(start-1,start).matches("[+-]"))	//17000021	251	258	1858C>T --> +1858C>T
		        			{
		        				check=1;
		        				start=start-1;
		        				mention=article.substring(start-1,start)+mention;
		        			}
		        			
		        			Pattern ptmp = Pattern.compile("^([\\/,][A-Z])[\\W\\-\\_]");
		        			int last_bound=last+20;
		        			if(article.length()<last+20)
		        			{
		        				last_bound=article.length();
		        			}
							Matcher mtmp = ptmp.matcher(article.substring(last,last_bound));
							if(mtmp.find())
							{
								check=1;
		        				last=last+mtmp.group(1).length();
		        				mention=article.substring(start,last);
							}
		        			
		        			if(mention.matches("^[^0-9A-Za-z][RrSs][Ss][0-9]+$"))	//_rs7207916
							{
		        				check=1;
		        				start=start+1;
		        				mention=mention.substring(1,mention.length());
		        			}
		        			if(mention.startsWith("(") && !mention.contains(")")) // delete (
							{
		        				check=1;
		        				start=start+1;
		        				mention=mention.substring(1,mention.length());
							}
		        			
		        			//mention.substring(mention.length()-1, mention.length()) : the last character of mention
		        			
		        			else if(mention.substring(mention.length()-1, mention.length()).equals(")") && !mention.contains("(")) // delete )
		        			{
		        				check=1;
		        				last=last-1;
		        				mention=mention.substring(0,mention.length()-1);
		        			}
		        			else if(start > 0 && article.substring(start-1,start).equals("(") && mention.contains(")") && !mention.contains("(")) // add (
		        			{
		        				check=1;
		        				start=start-1;
		        				mention="("+mention;
		        			}
		        			else if(start > 0 && article.substring(start-1,start).equals("[") && mention.contains("]") && !mention.contains("[")) // add [
		        			{
		        				check=1;
		        				start=start-1;
		        				mention="["+mention;
		        			}
		        			else if(article.substring(last,last+1).equals(")") && mention.contains("(") && !mention.contains(")")) // add )
							{
		        				check=1;
		        				last=last+1;
		        				mention=mention+")";
							}
		        			else if(article.substring(last,last+1).equals("]") && mention.contains("[") && !mention.contains("]")) // add ]
							{
		        				check=1;
		        				last=last+1;
		        				mention=mention+"]";
							}
		        			else if(mention.startsWith("(") && mention.substring(mention.length()-1, mention.length()).equals(")")) // delete (  )
		        			{
		        				check=1;
		        				start=start+1;
		        				last=last-1;
		        				mention=mention.substring(1,mention.length()-1);
		        			}
	        			}
	        			if(check == 1)
	        			{	
	        				Annotation.remove(i);
	        				Annotation.add(i,Pmid+"\t"+start+"\t"+last+"\t"+mention+"\t"+type+"\t"+identifier); //problem not solve!!!
	        			}
					}
					
	    			/*
	    			 *  Mention Recognition by Pattern
	    			 */
					
					/*
					 * Self-pattern: should close in Full text
					 */
					/*
        			HashMap<String,String> SelfPattern2type_hash = new HashMap<String,String>();
					for(int i=0;i<Annotation.size();i++)
					{
						String anno[]=Annotation.get(i).split("\t");
						
			    		String mention = anno[3];
	        			String type = anno[4];
	        			if(!type.equals("SNP"))
	        			{
	        				mention = mention.replaceAll("([^A-Za-z0-9])","\\$1");
	        				mention = mention.replaceAll("[0-9]+","[0-9]+");
	        				mention = mention.replaceAll("(IVS|EX)","@@@@");
	        				mention = mention.replaceAll("(rs|ss)","@@@");
	        				mention = mention.replaceAll("[A-Z]","[A-Z]");
	        				mention = mention.replaceAll("@@@@","(IVS|EX)");
	        				mention = mention.replaceAll("@@@","(rs|ss)");
	        				SelfPattern2type_hash.put(mention, type);
	        			}
					}
	    			*/
	    			
					/*
	    			 * Pattern match
	    			 */
					HashMap <String,String> AddList = new HashMap <String,String>();
					ArrayList <String> removeList = new ArrayList <String>();
					String article_tmp=article;
					for(int pat_i=0;pat_i<tmVar.MF_Pattern.size();pat_i++)
					{
						String pattern_RegEx = tmVar.MF_Pattern.get(pat_i); 
						String pattern_Type = tmVar.MF_Type.get(pat_i);
						
						Pattern PATTERN_RegEx;
						
						if(pattern_Type.matches(".*(Allele|AcidChange)"))
						{
							PATTERN_RegEx = Pattern.compile("^(.*?[ \\(;,\\/])("+pattern_RegEx+")[ \\);,\\/\\.]");
						}
						else
						{
							PATTERN_RegEx = Pattern.compile("^(.*?[^A-Za-z0-9])("+pattern_RegEx+")[^A-Za-z0-9]");
						}
						Matcher mp = PATTERN_RegEx.matcher(article_tmp);
						while (mp.find()) 
						{
							String pre = mp.group(1);
							String mention = mp.group(2);
							
							if(mention.matches("[0-9]+[\\W\\-\\_]*[a-z]ins")){/*System.out.println(mention);*/}
							else
							{
								//System.out.println(pattern_RegEx+"\t"+mention);
								boolean ExistLarger = false;
								int start=pre.length();
								int last=start+mention.length();
								//Check if overlap with previous annotation
								for(int a=0;a<Annotation.size();a++)
								{
									String Exist[] = Annotation.get(a).split("\t");
									int Exist_start = Integer.parseInt(Exist[1]);
									int Exist_last = Integer.parseInt(Exist[2]);
									if( (start<Exist_start && last>=Exist_last) || (start<=Exist_start && last>Exist_last) )
									{
										removeList.add(Annotation.get(a));
									}
									else if (((start>Exist_start && start<Exist_last) || (last>Exist_start && last<Exist_last)) && ((last-start)>(Exist_last-Exist_start))) //overlap, but not subset
									{
										removeList.add(Annotation.get(a));
									}
									else if( (Exist_start<start && Exist_last>=last) || (Exist_start<=start && Exist_last>last) )
									{
										ExistLarger = true;
									}
								}
								if(ExistLarger == false)
								{	
									//Check if overlap with previous annotation
									for(String added : AddList.keySet())
									{
										String Exist[] = added.split("\t");
										int Exist_start = Integer.parseInt(Exist[1]);
										int Exist_last = Integer.parseInt(Exist[2]);
										if( (start<Exist_start && last>=Exist_last) || (start<=Exist_start && last>Exist_last) )
										{
											AddList.put(added,"remove");
										}
										else if ( ((start>Exist_start && start<Exist_last) || (last>Exist_start && last<Exist_last)) && ((last-start)>(Exist_last-Exist_start)) ) //overlap, but not subset
										{
											AddList.put(added,"remove");
										}
										else if( (Exist_start<start && Exist_last>=last) || (Exist_start<=start && Exist_last>last) )
										{
											ExistLarger = true;
										}
									}
									if(ExistLarger == false && !AddList.containsKey(Pmid+"\t"+start+"\t"+last+"\t"+mention))
									{

										if(mention.matches("[A-Z]+[ ]+(for|to)[ ]+[0-9]+")){/*System.out.println(mention);*/} //C for 6 - remove
										else if(mention.matches("[0-9]+[ ]+(for|to)[ ]+[A-Z]+")){/*System.out.println(mention);*/} //6 to C - remove
										else
										{
											AddList.put(Pmid+"\t"+start+"\t"+last+"\t"+mention,pattern_Type);
										}
									}
								}
								String tmp="";
		        				for(int j=0;j<mention.length();j++){tmp=tmp+"@";}
		        				article_tmp=article_tmp.substring(0,start)+tmp+article_tmp.substring(last);
								mp = PATTERN_RegEx.matcher(article_tmp);
							}
						}
					}
					for(String pattern_RegEx:Temporary_Pattern_Allele.get(Pmid).keySet())
					{
						String pattern_Type = Temporary_Pattern_Allele.get(Pmid).get(pattern_RegEx);
						
						Pattern PATT_RegEx;
						if(pattern_Type.matches(".*(Allele|AcidChange)"))
						{
							PATT_RegEx = Pattern.compile("^(.*?[ \\(;,\\/\\-])("+pattern_RegEx+")[ \\);,\\/\\.]");
						}
						else
						{
							PATT_RegEx = Pattern.compile("^(.*?[^A-Za-z0-9])("+pattern_RegEx+")[^A-Za-z0-9]");
						}
						Matcher mp = PATT_RegEx.matcher(article_tmp);
						while (mp.find()) 
						{
							String pre = mp.group(1);
							String mention = mp.group(2);
							
							if(mention.matches("[0-9]+[\\W\\-\\_]*[a-z]ins")){/*System.out.println(mention);*/}
							else
							{
								boolean ExistLarger = false;
								int start=pre.length();
								int last=start+mention.length();
								//Check if overlap with previous annotation
								for(int a=0;a<Annotation.size();a++)
								{
									String Exist[] = Annotation.get(a).split("\t");
									int Exist_start = Integer.parseInt(Exist[1]);
									int Exist_last = Integer.parseInt(Exist[2]);
									if( (start<Exist_start && last>=Exist_last) || (start<=Exist_start && last>Exist_last) )
									{
										removeList.add(Annotation.get(a));
									}
									else if (((start>Exist_start && start<Exist_last) || (last>Exist_start && last<Exist_last)) && ((last-start)>(Exist_last-Exist_start))) //overlap, but not subset
									{
										removeList.add(Annotation.get(a));
									}
									else if( (Exist_start<start && Exist_last>=last) || (Exist_start<=start && Exist_last>last) )
									{
										ExistLarger = true;
									}
								}
								if(ExistLarger == false)
								{	
									//Check if overlap with previous annotation
									for(String added : AddList.keySet())
									{
										String Exist[] = added.split("\t");
										int Exist_start = Integer.parseInt(Exist[1]);
										int Exist_last = Integer.parseInt(Exist[2]);
										if( (start<Exist_start && last>=Exist_last) || (start<=Exist_start && last>Exist_last) )
										{
											AddList.put(added,"remove");
										}
										else if ( ((start>Exist_start && start<Exist_last) || (last>Exist_start && last<Exist_last)) && ((last-start)>(Exist_last-Exist_start)) ) //overlap, but not subset
										{
											AddList.put(added,"remove");
										}
										else if( (Exist_start<start && Exist_last>=last) || (Exist_start<=start && Exist_last>last) )
										{
											ExistLarger = true;
										}
									}
									if(ExistLarger == false && !AddList.containsKey(Pmid+"\t"+start+"\t"+last+"\t"+mention))
									{
										AddList.put(Pmid+"\t"+start+"\t"+last+"\t"+mention,pattern_Type);
									}
								}
								String tmp="";
		        				for(int j=0;j<mention.length();j++){tmp=tmp+"@";}
		        				article_tmp=article_tmp.substring(0,start)+tmp+article_tmp.substring(last);
								mp = PATT_RegEx.matcher(article_tmp);
							}
						}
					}
					for(int r=0;r<removeList.size();r++)
					{
						Annotation.remove(removeList.get(r));
					}
					for(String added : AddList.keySet())
					{
						if(!AddList.get(added).equals("remove"))
						{
							boolean found= false;
							for(int a=0;a<Annotation.size();a++)
							{
								String Anno[] = Annotation.get(a).split("\t");
								if(added.equals(Anno[0]+"\t"+Anno[1]+"\t"+Anno[2]+"\t"+Anno[3]))
								{
									found = true;
								}
							}
							if(found == false)
							{
								Annotation.add(added+"\t"+AddList.get(added));
							}
						}
					}
    				
    				/*
	    			 * Pattern match : Self pattern
	    			 */
    				/*
    				for(String pattern_RegEx : SelfPattern2type_hash.keySet())
					{
    					String pattern_Type = SelfPattern2type_hash.get(pattern_RegEx);
				   		Pattern PATTERN_RegEx = Pattern.compile("^(.*[^A-Za-z0-9])("+pattern_RegEx+")[^A-Za-z0-9]");
						Matcher mp = PATTERN_RegEx.matcher(article);
						while (mp.find()) 
						{
							String pre = mp.group(1);
							String mention = mp.group(2);
							int start=pre.length();
							int last=start+mention.length();
							Annotation.add(Pmid+"\t"+start+"\t"+last+"\t"+mention+"\t"+pattern_Type);
							String tmp="";
	        				for(int j=0;j<mention.length();j++){tmp=tmp+"@";}
							article=article.substring(0,start)+tmp+article.substring(last);
							System.out.println(mention);
						}
					}
					*/
	    			
					for(int i=0;i<Annotation.size();i++)
					{
						FilePostME.write(Annotation.get(i)+"\n");
					}
					
					FilePostME.write("\n");
					article="";
					Annotation.clear();
				}
			}
			FilePostME.close();
		}
		catch(IOException e1){ System.out.println("[toPostME]: Input file is not exist.");}
	}
	public void toPostMEData(String Filename,String FilenamePostME,String FilenamePostMeML,String FilenamePostMeData,String TrainTest) throws IOException
	{
		try
		{
			//Parse identifier (components)
			Pattern Pattern_Component_1 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|(fs[^|]*)\\|([^|]*)$");
			Pattern Pattern_Component_1_1 = Pattern.compile("^([^|]*)\\|([^|]*(ins|del|Del|dup|-)[^|]*)\\|([^|]*)\\|([^|]*)\\|(fs[^|]*)$"); //append for p.G352fsdelG	p|del|352|G,G|fs
			Pattern Pattern_Component_2 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|(fs[^|]*)$");
			Pattern Pattern_Component_3 = Pattern.compile("^([^|]*)\\|([^|]*(ins|del|Del|dup|-|insertion|deletion|insertion|deletion|deletion\\/insertion|insertion\\/deletion|indel|delins|duplication|lack|lacked|copy|lose|losing|lacking|inserted|deleted|duplicated|insert|delete|duplicate|repeat|repeated)[^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern Pattern_Component_4 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern Pattern_Component_5 = Pattern.compile("^([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)$");
			Pattern Pattern_Component_6 = Pattern.compile("^((\\[rs\\]|[RrSs][Ss]|reference SNP no[.] )[0-9][0-9][0-9]+)$");
			
			HashMap<String,String> mention_hash = new HashMap<String,String>();
			HashMap<String,String> mention2type_hash = new HashMap<String,String>();
			BufferedReader inputfile;
			if(TrainTest.equals("Train"))
			{
				inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			}
			else
			{
				inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenamePostME), "UTF-8"));
			}
			
			String line;
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					
	        	}
				else if (line.contains("\t")) //Annotation
				{
					String anno[]=line.split("\t");
					if(anno.length>=6 && TrainTest.equals("Train"))
	        		{
	        			String mention=anno[3];
	        			String type=anno[4];
	        			String identifier=anno[5];
	        			mention2type_hash.put(mention, type);
	        			mention_hash.put(mention, identifier);
	        		}
					else if(anno.length>=5)
	        		{
	        			String mention=anno[3];
	        			String type=anno[4];
	        			mention2type_hash.put(mention, type);
	        			mention_hash.put(mention, type);
	        		}
				}
			}
			inputfile.close();
			
			BufferedWriter mentionlistbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenamePostMeML), "UTF-8")); // .ml
			BufferedWriter mentiondata = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenamePostMeData), "UTF-8")); // .location
			for(String mention : mention_hash.keySet() )
			{
				HashMap<Integer, String> character_hash = new HashMap<Integer, String>();
				
				int start=0;
    			int last=mention.length();
    			for(int s=start;s<last;s++)
    			{
    				character_hash.put(s,"I");
    			}
    			
    			if(TrainTest.equals("Train"))
    			{
    				Matcher m1 = Pattern_Component_1.matcher(mention_hash.get(mention));
    				Matcher m1_1 = Pattern_Component_1_1.matcher(mention_hash.get(mention));
        			Matcher m2 = Pattern_Component_2.matcher(mention_hash.get(mention));
        			Matcher m3 = Pattern_Component_3.matcher(mention_hash.get(mention));
        			Matcher m4 = Pattern_Component_4.matcher(mention_hash.get(mention));
        			Matcher m5 = Pattern_Component_5.matcher(mention_hash.get(mention));
        			Matcher m6 = Pattern_Component_6.matcher(mention_hash.get(mention));
        			
        			if(m1.find())
	    			{
        				String type[]=m1.group(1).split(",");
	    				String W[]=m1.group(2).split(",");
	    				String P[]=m1.group(3).split(",");
	    				String M[]=m1.group(4).split(",");
	    				String F[]=m1.group(5).split(",");
	    				String S[]=m1.group(6).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    		    				character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\ttype\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<W.length;i++)
	    				{
	    					String patt="^(.*?)("+W[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"W");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\tW\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+P[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\tM\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<F.length;i++)
	    				{
	    					F[i]=F[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+F[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"F");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\tF\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<S.length;i++)
	    				{
	    					String patt="^(.*?)("+S[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"S");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1\tS\t"+mention);
	    					}
	    				}
	    			}
        			else if(m1_1.find())
	    			{
        				String type[]=m1_1.group(1).split(",");
	    				String T[]=m1_1.group(2).split(",");
	    				String P[]=m1_1.group(4).split(",");
	    				String M[]=m1_1.group(5).split(",");
	    				String F[]=m1_1.group(6).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    		    				character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1_1\ttype\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<T.length;i++)
	    				{
	    					String patt="^(.*?)("+T[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"T");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1_1\tT\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*)("+P[i]+")(.*)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1_1\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1_1\tM\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<F.length;i++)
	    				{
	    					F[i]=F[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+F[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"F");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m1_1\tF\t"+mention);
	    					}
	    				}
	    			}
        			else if(m2.find())
	    			{
	    				String type[]=m2.group(1).split(",");
	    				String W[]=m2.group(2).split(",");
	    				String P[]=m2.group(3).split(",");
	    				String M[]=m2.group(4).split(",");
	    				String F[]=m2.group(5).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m2\tType\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<W.length;i++)
	    				{
	    					String patt="^(.*?)("+W[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"W");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m2\tW\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+P[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m2\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m2\tM\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<F.length;i++)
	    				{
	    					F[i]=F[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+F[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"F");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m2\tF\t"+mention);
	    					}
	    				}
	    			}
	    			else if(m3.find())
	    			{
	    				String type[]=m3.group(1).split(",");
	    				String T[]=m3.group(2).split(",");
	    				String P[]=m3.group(4).split(",");
	    				String M[]=m3.group(5).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m3\tType\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+P[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m3\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<T.length;i++)
	    				{
	    					String patt="^(.*?)("+T[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"T");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m3\tT\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m3\tM\t"+mention);
	    					}
	    				}
	    			}
	    			else if(m4.find())
	    			{
	    				String type[]=m4.group(1).split(",");
	    				String W[]=m4.group(2).split(",");
	    				String P[]=m4.group(3).split(",");
	    				String M[]=m4.group(4).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m4\tType\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<W.length;i++)
	    				{
	    					String patt="^(.*?)("+W[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"W");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m4\tW\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+P[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m4\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m4\tM\t"+mention);
	    					}
	    				}
	    			}
	    			else if(m5.find())
	    			{
	    				String type[]=m5.group(1).split(",");
	    				String T[]=m5.group(2).split(",");
	    				String P[]=m5.group(3).split(",");
	    				String M[]=m5.group(4).split(",");
	    				String D[]=m5.group(5).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<type.length;i++)
	    				{
	    					String patt="^(.*?)("+type[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"A");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m5\tType\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<T.length;i++)
	    				{
	    					String patt="^(.*?)("+T[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"T");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m5\tT\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<P.length;i++)
	    				{
	    					P[i]=P[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+P[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"P");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m5\tP\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<M.length;i++)
	    				{
	    					M[i]=M[i].replace("*", "\\*");
	    					String patt="^(.*?)("+M[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"M");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m5\tM\t"+mention);
	    					}
	    				}
	    				for(int i=0;i<D.length;i++)
	    				{
	    					D[i]=D[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
	    					String patt="^(.*?)("+D[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"D");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m5\tD\t"+mention);
	    					}
	    				}
	    			}
	    			else if(m6.find())
	    			{
	    				String RS[]=m6.group(1).split(",");
	    				String mention_tmp=mention;
	    				for(int i=0;i<RS.length;i++)
	    				{
	    					RS[i]=RS[i].replaceAll("([\\[\\]])", "\\\\$1");
	    					String patt="^(.*?)("+RS[i]+")(.*?)$";
	    					Pattern ptmp = Pattern.compile(patt);
	    					Matcher mtmp = ptmp.matcher(mention_tmp);
	    					if(mtmp.find())
	    					{
	    						String mtmp1=mtmp.group(1);
	    						for(int j=mtmp1.length();j<(mtmp1.length()+mtmp.group(2).length());j++)
	    						{
	    							character_hash.put(j,"R");
	    						}
	    						String mtmp2_tmp="";
	    						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
	    						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
	    					}
	    					else
	    					{
	    						System.out.println("Error! Cannot find component: m6\tType\t"+mention);
	    					}
	    				}
	    			}
	    			else
	        		{
	        			System.out.println("Error! Annotation component cannot match RegEx. " + mention);
	        		}
    			}
    			
    			mentionlistbw.write(mention+"\t"+mention2type_hash.get(mention)+"\n");
				if(TrainTest.equals("Train"))
    			{
					mentiondata.write("I I I I I I I I I I I I I I I I I I I\n");
				}
				else
				{
					mentiondata.write("I I I I I I I I I I I I I I I I I I\n");
				}
				String mention_tmp=mention;
				String mention_org=mention;
				mention_tmp = mention_tmp.replaceAll("([0-9])([A-Za-z])", "$1 $2");
				mention_tmp = mention_tmp.replaceAll("([A-Za-z])([0-9])", "$1 $2");
				mention_tmp = mention_tmp.replaceAll("([A-Z])([a-z])", "$1 $2");
				mention_tmp = mention_tmp.replaceAll("([a-z])([A-Z])", "$1 $2");
				mention_tmp = mention_tmp.replaceAll("(.+)fs", "$1 fs");
				mention_tmp = mention_tmp.replaceAll("fs(.+)", "fs $1");
				mention_tmp = mention_tmp.replaceAll("[ ]+", " ");
				String regex="\\s+|(?=\\p{Punct})|(?<=\\p{Punct})";
				String Tokens[]=mention_tmp.split(regex);
				
				start=0;
				last=0;
				
    			for(int i=0;i<Tokens.length;i++)
				{
					if(Tokens[i].length()>0)
					{
						String tkni=Tokens[i].replaceAll("([\\{\\}\\[\\]\\+\\-\\(\\)\\*\\?\\/\\\\])", "\\\\$1");
						Pattern Pcn = Pattern.compile("^([ ]*)("+tkni+")(.*)$");
						Matcher mPcn = Pcn.matcher(mention_org);
		    			if(mPcn.find())
						{
		    				last=last+mPcn.group(1).length();
		    				mention_org=mPcn.group(3);
		    			}
						start=last+1;
						last=start+Tokens[i].length()-1;
					
						//Number of Numbers [0-9]
						String Num_num="";
						String tmp=Tokens[i];
						tmp=tmp.replaceAll("[^0-9]","");
						if(tmp.length()>3){Num_num="N:4+";}else{Num_num="N:"+ tmp.length();}
						
						//Number of Uppercase [A-Z]
						String Num_Uc="";
						tmp=Tokens[i];
						tmp=tmp.replaceAll("[^A-Z]","");
						if(tmp.length()>3){Num_Uc="U:4+";}else{Num_Uc="U:"+ tmp.length();}
						
						//Number of Lowercase [a-z]
						String Num_Lc="";
						tmp=Tokens[i];
						tmp=tmp.replaceAll("[^a-z]","");
						if(tmp.length()>3){Num_Lc="L:4+";}else{Num_Lc="L:"+ tmp.length();}
						
						//Number of ALL char
						String Num_All="";
						if(Tokens[i].length()>3){Num_All="A:4+";}else{Num_All="A:"+ Tokens[i].length();}
						
						//specific character (;:,.->+_)
						String SpecificC="";
						tmp=Tokens[i];
						
						if(Tokens[i].equals(";") || Tokens[i].equals(":") || Tokens[i].equals(",") || Tokens[i].equals(".") || Tokens[i].equals("-") || Tokens[i].equals(">") || Tokens[i].equals("+") || Tokens[i].equals("_"))
						{
							SpecificC="-SpecificC1-";
						}
						else if(Tokens[i].equals("(") || Tokens[i].equals(")"))
						{
							SpecificC="-SpecificC2-";
						}
						else if(Tokens[i].equals("{") || Tokens[i].equals("}"))
						{
							SpecificC="-SpecificC3-";
						}
						else if(Tokens[i].equals("[") || Tokens[i].equals("]"))
						{
							SpecificC="-SpecificC4-";
						}
						else if(Tokens[i].equals("\\") || Tokens[i].equals("/"))
						{
							SpecificC="-SpecificC5-";
						}
						else
						{
							SpecificC="__nil__";
						}
						
						//mutation level
						String Mlevel="";
						if(Tokens[i].equals("p")){Mlevel="-ProteinLevel-";}
						else if(Tokens[i].matches("^[cgmr]$")){Mlevel="-DNALevel-";}
						else{Mlevel="__nil__";}
						
						//mutation type
						String Mtype="";
						String tkn=Tokens[i].toLowerCase();
						String last2_tkn="";
						String last_tkn="";
						String next_tkn="";
						String next2_tkn="";
						if(i>1){last2_tkn=Tokens[i-2];}
						if(i>0){last_tkn=Tokens[i-1];}
						if(Tokens.length>1 && i<Tokens.length-1){next_tkn=Tokens[i+1];}
						if(Tokens.length>2 && i<Tokens.length-2){next2_tkn=Tokens[i+2];}
						
						if(tkn.toLowerCase().matches("^(insert(ion|ed|ing|)|duplicat(ion|e|ed|ing)|delet(ion|e|ed|ing)|frameshift|missense|lack(|ed|ing)|los(e|ing|ed)|copy|repeat(|ed)|inversion)$")) {Mtype="-Mtype- -MtypeFull-";}
						else if(tkn.matches("^(nsert(ion|ed|ing|)|uplicat(ion|e|ed|ing)|elet(ion|e|ed|ing)|rameshift|issense|ack(|ed|ing)|os(e|ing|ed)|opy|epeat(|ed)|nversion)$")) {Mtype="-Mtype- -MtypeFull_suffix-";}
						else if(tkn.matches("^(del|ins|delins|indel|dup|inv)$")) {Mtype="-Mtype- -MtypeTri-";}
						else if(tkn.matches("^(start[a-z]*|found|identif[a-z]+|substit[a-z]*|lead[a-z]*|exchang[a-z]*|chang[a-z]*|mutant[a-z]*|mutate[a-z]*|devia[a-z]*|modif[a-z]*|alter[a-z]*|switch[a-z]*|variat[a-z]*|instead[a-z]*|replac[a-z]*|in place|convert[a-z]*|becom[a-z]*|transition|transversion)$")) {Mtype="-SurroundingWord- -CausingVerb-";}
						else if(tkn.matches("^(caus(e|es|ing)|lead(|s|ing|ed)|encod(e|es|ing|ed)|result(|s|ing|ed)|produc(e|es|ing|ed)|chang(e|es|ing|ed)|covert(|s|ing|ed)|correspond(|s|ing|ed)|predict(|s|ing|ed)|cod(e|es|ing|ed)|concordanc(e|es|ing|ed)|concordant|consist(|s|ing|ed)|encod(e|es|ing|ed)|represent(|s|ing|ed)|led|responsible|denot(e|es|ing|ed)|designat(e|es|ing|ed)|introduc(e|es|ing|ed)|characteriz(e|es|ing|ed)|bring|involv(e|es|ing|ed)|implicat(e|es|ing|ed)|indicat(e|es|ing|ed)|express(|s|ing|ed)|behav(e|es|ing|ed)|suggest(|s|ing|ed)|impl(y|ies|ed|ying)|presum(e|es|ing|ed))$")) {Mtype="-SurroundingWord- -CausingVerb-";}
						else if(tkn.matches("^(polymorphic|site|point|premature|replacement|replac(e|es|ed|ing)|substitution(s|)|polar|charged|amphipathic|hydrophobic|amino|acid(s|)|nucleotide(s|)|mutation(s|)|position(s|)|posi|pos|islet|liver|base|pair(s|)|bp|bps|bp|residue(s|)|radical|codon|aa|nt|alpha|beta|gamma|ezta|theta|delta|tetranucleotide|polymorphism|terminal)$")) {Mtype="-SurroundingWord- -SurroundingWord2-";}
						else if(tkn.matches("^((has|have|had) been|is|are|was|were)$")) {Mtype="-SurroundingWord- -BeVerb-";}
						else if(Tokens[i].matches("^(a|an|the|)$")) {Mtype="-SurroundingWord- -Article-";}
						else if(Tokens[i].matches("^(which|where|what|that)$")) {Mtype="-SurroundingWord- -RelativePronouns-";}
						else if(Tokens[i].matches("^(in|to|into|for|of|by|with|at|locat(e|ed|es)|among|between|through|rather|than|either)$")) {Mtype="-SurroundingWord- -Preposition-";}
						else if(tkn.equals("/") && last_tkn.matches("(ins|del)") && next_tkn.toLowerCase().matches("(ins|del)")) {Mtype="-Mtype- -MtypeTri-";}
						else if((last2_tkn.toLowerCase().equals("/") && last_tkn.toLowerCase().equals("\\")) || (next_tkn.toLowerCase().equals("/") && next2_tkn.toLowerCase().equals("\\"))) {Mtype="-Mtype- -MtypeTri-";}
						else {Mtype="__nil__ __nil__";}
						
						//DNA symbols
						String DNASym="";
						if(tkn.matches("^(adenine|guanine|thymine|cytosine)$")){DNASym="-DNASym- -DNASymFull-";}
						else if(tkn.matches("^(denine|uanine|hymine|ytosine)$")){DNASym="-DNASym- -DNASymFull_suffix-";}
						else if(Tokens[i].matches("^[ATCGU]+$")){DNASym="-DNASym- -DNASymChar-";}
						else {DNASym="__nil__ __nil__";}
						
						//Protein symbols
						String ProteinSym="";
						if(tkn.matches("^(glutamine|glutamic|leucine|valine|isoleucine|lysine|alanine|glycine|aspartate|methionine|threonine|histidine|aspartic|asparticacid|arginine|asparagine|tryptophan|proline|phenylalanine|cysteine|serine|glutamate|tyrosine|stop|frameshift)$")){ProteinSym="-ProteinSym- -ProteinSymFull-";}
						else if(tkn.matches("^(lutamine|lutamic|eucine|aline|soleucine|ysine|lanine|lycine|spartate|ethionine|hreonine|istidine|spartic|sparticacid|rginine|sparagine|ryptophan|roline|henylalanine|ysteine|erine|lutamate|yrosine|top|rameshift)$")){ProteinSym="-ProteinSym- -ProteinSymFull_suffix-";}
						else if(tkn.matches("^(cys|ile|ser|gln|met|asn|pro|lys|asp|thr|phe|ala|gly|his|leu|arg|trp|val|glu|tyr)$")){ProteinSym="-ProteinSym- -ProteinSymTri-";}
						else if(tkn.matches("^(ys|le|er|ln|et|sn|ro|ys|sp|hr|phe|la|ly|is|eu|rg|rp|al|lu|yr)$")){ProteinSym="-ProteinSym- -ProteinSymTri_suffix-";}
						else if(Tokens[i].matches("^[CISQMNPKDTFAGHLRWVEYX]$") && !next_tkn.toLowerCase().matches("^[ylsrhpiera]")) {ProteinSym="-ProteinSym- -ProteinSymChar-";}
						else if(Tokens[i].matches("^[CISGMPLTHAVF]$") && next_tkn.toLowerCase().matches("^[ylsrhpiera]")) {ProteinSym="-ProteinSym- -ProteinSymChar-";}
						else {ProteinSym="__nil__ __nil__";}
						
						//IVS/EX
						String IVSEX="";
						if(tkn.matches("^(ivs|ex)$")){IVSEX="-IVSEX-";}
						else if(Tokens[i].equals("E") && last_tkn.equals("x")){IVSEX="-IVSEX-";}
						else if(last_tkn.equals("E") && Tokens[i].equals("x")){IVSEX="-IVSEX-";}
						else {IVSEX="__nil__";}
						
						//FSX feature
						String FSXfeature="";
						if(tkn.matches("^(fs|fsx|x|\\*)$")){FSXfeature="-FSX-";}
						else if(last_tkn.toLowerCase().equals("s") && tkn.equals("x")){FSXfeature="-FSX-";}
						else {FSXfeature="__nil__";}
						
						//position type
						String PositionType="";
						if(tkn.matches("^(nucleotide|codon|amino|acid|position|bp|b|base|pair)$")){PositionType="-PositionType-";}
						else if(tkn.matches("^(single|one|two|three|four|five|six|seven|eight|nine|ten|[0-9]+)$")){PositionType="-PositionNum-";}
						else {PositionType="__nil__";}
						
						//sequence location
						String SeqLocat="";
						if(tkn.matches("^(intron|exon|promoter|utr)$")){SeqLocat="-SeqLocat-";}
						else {SeqLocat="__nil__";}
						
						//RS
						String RScode="";
						if(tkn.equals("rs")){RScode="-RScode-";}
						else {RScode="__nil__";}
						
						if(TrainTest.equals("Train"))
		    			{
							mentiondata.write(Tokens[i]+" "+Num_num+" "+Num_Uc+" "+Num_Lc+" "+Num_All+" "+SpecificC+" "+Mlevel+" "+Mtype+" "+DNASym+" "+ProteinSym+" "+IVSEX+" "+FSXfeature+" "+PositionType+" "+SeqLocat+" "+RScode+" "+character_hash.get(start-1)+"\n");
		    			}
						else
						{
							mentiondata.write(Tokens[i]+" "+Num_num+" "+Num_Uc+" "+Num_Lc+" "+Num_All+" "+SpecificC+" "+Mlevel+" "+Mtype+" "+DNASym+" "+ProteinSym+" "+IVSEX+" "+FSXfeature+" "+PositionType+" "+SeqLocat+" "+RScode+"\n");
						}
					}
				}
				mentiondata.write("\n");
			}
			mentionlistbw.close();
			mentiondata.close();
		}
		catch(IOException e1){ System.out.println("[toPostMEData]: "+e1+" Input file is not exist.");}
	}
	
	public void toPostMEModel(String FilenamePostMEdata) throws IOException
	{
		Process process = null;
	    String line = null;
	    InputStream is = null;
	    InputStreamReader isr = null;
	    BufferedReader br = null;
	   
	    Runtime runtime = Runtime.getRuntime();
	    String OS=System.getProperty("os.name").toLowerCase();
		String cmd="";
	    if(OS.contains("windows"))
	    {
	    	cmd ="CRF/crf_learn -f 3 -c 4.0 CRF/template_UB.mention "+FilenamePostMEdata+" CRF/ComponentExtraction.Model.new";
	    }
	    else //if(OS.contains("nux")||OS.contains("nix"))
	    {
	    	cmd ="./CRF/crf_learn -f 3 -c 4.0 CRF/template_UB.mention "+FilenamePostMEdata+" CRF/ComponentExtraction.Model.new";
	    }
	    
	    try {
	    	process = runtime.exec(cmd);
		    is = process.getInputStream();
		    isr = new InputStreamReader(is);
		    br = new BufferedReader(isr);
		    while ( (line = br.readLine()) != null) 
		    {
		    	System.out.println(line);
		        System.out.flush();
		    }
		    is.close();
		    isr.close();
		    br.close();
	    }
	    catch (IOException e) {
	    	System.out.println(e);
	    	runtime.exit(0);
	    }
	}
	public void toPostMEoutput(String FilenamePostMEdata,String FilenamePostMEoutput) throws IOException
	{
		/* 
		 * Recognizing components
		 */
		Runtime runtime = Runtime.getRuntime();
	    String OS=System.getProperty("os.name").toLowerCase();
		String cmd="";
	    if(OS.contains("windows"))
	    {
	    	cmd ="CRF/crf_test -m CRF/ComponentExtraction.Model -o "+FilenamePostMEoutput+" "+FilenamePostMEdata;
	    }
	    else //if(OS.contains("nux")||OS.contains("nix"))
	    {
	    	cmd ="./CRF/crf_test -m CRF/ComponentExtraction.Model -o "+FilenamePostMEoutput+" "+FilenamePostMEdata;
	    }
	    
	    try {
	    	File f = new File(FilenamePostMEoutput);
	        BufferedWriter fr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
	    	Process process = runtime.exec(cmd);
	    	InputStream is = process.getInputStream();
	    	InputStreamReader isr = new InputStreamReader(is);
	    	BufferedReader br = new BufferedReader(isr);
	    	String line="";
		    while ( (line = br.readLine()) != null) 
		    {
		    	fr.write(line);
		    	fr.newLine();
		        fr.flush();
		    }
		    is.close();
		    isr.close();
		    br.close();
		    fr.close();
	    }
	    catch (IOException e) {
	    	System.out.println(e);
	    	runtime.exit(0);
	    }
	}
	public void output2PubTator(String FilenamePostMEml,String FilenamePostMEoutput,String FilenamePostME,String FilenamePubTator) throws IOException
	{
		try {
			ArrayList<String> mentionlist = new ArrayList<String>(); 
			ArrayList<String> identifierlist = new ArrayList<String>(); 
			ArrayList<String> typelist = new ArrayList<String>(); 
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenamePostMEml), "UTF-8"));
			int count=0;
			HashMap<Integer,Integer> boundary_hash = new HashMap<Integer,Integer>();
			HashMap<Integer,String> WMstate_hash = new HashMap<Integer,String>();
			String line;
			while ((line = inputfile.readLine()) != null)  
			{
				String columns[]=line.split("\\t",-1);
				String line_nospace=columns[0].replaceAll(" ","");
				Pattern pat1 = Pattern.compile("(.+?)(for|inplaceof|insteadof|mutantof|mutantsof|ratherthan|ratherthan|replacementof|replacementsof|replaces|replacing|residueatposition|residuefor|residueinplaceof|residueinsteadof|substitutioat|substitutionfor|substitutionof|substitutionsat|substitutionsfor|substitutionsof|substitutedfor|toreplace)(.+)$");
				Matcher mat1 = pat1.matcher(line_nospace.toLowerCase());
				Pattern pat2 = Pattern.compile("^(.+?)(>|to|into|of|by|with|at)(.+)$");
				Matcher mat2 = pat2.matcher(line_nospace.toLowerCase());
				
				if(mat1.find())
				{
					boundary_hash.put(count,mat1.group(1).length());
					WMstate_hash.put(count,"Backward");
				}
				else if(mat2.find())
				{
					boundary_hash.put(count,mat2.group(1).length());
					WMstate_hash.put(count,"Forward");
				}
				mentionlist.add(columns[0]);
				if(columns.length==2)
				{
					typelist.add(columns[1]);
				}
				else
				{
					typelist.add("DNAMutation");
				}
				count++;
			}
			inputfile.close();
			
			HashMap<String,String> component_hash = new HashMap<String,String>();
			component_hash.put("A", ""); //type
			component_hash.put("T", ""); //Method
			component_hash.put("P", ""); //Position
			component_hash.put("W", ""); //Wide type
			component_hash.put("M", ""); //Mutant
			component_hash.put("F", ""); //frame shift
			component_hash.put("S", ""); //frame shift position
			component_hash.put("D", ""); //
			component_hash.put("I", ""); //Inside
			component_hash.put("R", ""); //RS number
			
			BufferedReader PostMEfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenamePostMEoutput), "UTF-8"));
			String prestate="";
			count=0;
			int start_count=0;
			boolean codon_exist=false;
			HashMap<Integer,String> filteringNum_hash = new HashMap<Integer,String>();
			
			while ((line = PostMEfile.readLine()) != null)  
			{
				String outputs[]=line.split("\\t",-1);
					
				/*
				 * recognize status and mention
				 */
				if(outputs.length<=1)
				{
					//System.out.println(component_hash.get("W")+"\t"+component_hash.get("P")+"\t"+component_hash.get("M"));
					/*
					 *  Translate : nametothree | threetone | etc
					 */
					component_hash.put("W",component_hash.get("W").toUpperCase());
					component_hash.put("M",component_hash.get("M").toUpperCase());
					component_hash.put("P",component_hash.get("P").toUpperCase());
					boolean translate=false;
					boolean NotAaminoacid=false;
					boolean exchange_nu=false;
					//M
					HashMap<String,String> component_avoidrepeat = new HashMap<String,String>();
					String components[]=component_hash.get("M").split(",");
					component_hash.put("M","");
					String component="";
					for(int i=0;i<components.length;i++)
					{
						if(tmVar.nametothree.containsKey(components[i]))
						{
							component=tmVar.nametothree.get(components[i]);
							translate=true;
						}
						else if(tmVar.NTtoATCG.containsKey(components[i]))
						{
							component=tmVar.NTtoATCG.get(components[i]);
							NotAaminoacid=true;
						}
						else if(tmVar.threetone_nu.containsKey(components[i]) && NotAaminoacid==false && translate==false) //&& (component_hash.get("P").matches("(.*CODON.*|)") || codon_exist == true) 
						{
							component=tmVar.threetone_nu.get(components[i]);
							exchange_nu=true;
						}
						else
						{
							component=components[i];
						}
						
						if(component_hash.get("M").equals(""))
						{
							component_hash.put("M",component);
						}
						else
						{
							if(!component_avoidrepeat.containsKey(component))
							{
								component_hash.put("M",component_hash.get("M")+","+component);
							}
						}
						component_avoidrepeat.put(component,"");
					}
					
					component_avoidrepeat = new HashMap<String,String>();
					String components2[]=component_hash.get("M").split(",");
					component_hash.put("M","");
					component="";
					for(int i=0;i<components2.length;i++)
					{
						if(tmVar.threetone.containsKey(components2[i]))
						{
							component=tmVar.threetone.get(components2[i]);
							translate=true;
						}
						else if(tmVar.NTtoATCG.containsKey(components2[i]))
						{
							component=tmVar.NTtoATCG.get(components2[i]);
							NotAaminoacid=true;
						}
						else if(tmVar.threetone_nu.containsKey(components2[i]) && NotAaminoacid==false && translate==false) //&& (component_hash.get("P").matches("(.*CODON.*|)") || codon_exist == true) 
						{
							component=tmVar.threetone_nu.get(components2[i]);
							exchange_nu=true;
						}
						else if(components2[i].length()>1)
						{
							NotAaminoacid=false;
							component=components2[i];
						}
						else
						{
							component=components2[i];
						}	
						if(component_hash.get("M").equals(""))
						{
							component_hash.put("M",component);
						}
						else
						{
							if(!component_avoidrepeat.containsKey(component))
							{
								component_hash.put("M",component_hash.get("M")+","+component);
							}
						}
						component_avoidrepeat.put(component,"");
					}
					
					//W
					component_avoidrepeat = new HashMap<String,String>();
					String components3[]=component_hash.get("W").split(",");
					component_hash.put("W","");
					component="";
					for(int i=0;i<components3.length;i++)
					{
						if(tmVar.nametothree.containsKey(components3[i]))
						{
							component=tmVar.nametothree.get(components3[i]);
							translate=true;
						}
						else if(tmVar.NTtoATCG.containsKey(components3[i]))
						{
							component=tmVar.NTtoATCG.get(components3[i]);
							NotAaminoacid=true;
						}
						else if(tmVar.threetone_nu.containsKey(components3[i]) && NotAaminoacid==false && translate==false) //&& (component_hash.get("P").matches("(.*CODON.*|)") || codon_exist == true) 
						{
							component=tmVar.threetone_nu.get(components3[i]);
							exchange_nu=true;
						}
						else
						{
							component=components3[i];
						}	
						if(component_hash.get("W").equals(""))
						{
							component_hash.put("W",component);
						}
						else
						{
							if(!component_avoidrepeat.containsKey(component))
							{
								component_hash.put("W",component_hash.get("W")+","+component);
							}
						}
						component_avoidrepeat.put(component,"");
					}
					
					component_avoidrepeat = new HashMap<String,String>();
					String components4[]=component_hash.get("W").split(",");
					component_hash.put("W","");
					component="";
					for(int i=0;i<components4.length;i++)
					{
						if(tmVar.threetone.containsKey(components4[i]))
						{
							component=tmVar.threetone.get(components4[i]);
							translate=true;
						}
						else if(tmVar.NTtoATCG.containsKey(components4[i]))
						{
							component=tmVar.NTtoATCG.get(components4[i]);
							NotAaminoacid=true;
						}
						else if(tmVar.threetone_nu.containsKey(components4[i]) && NotAaminoacid==false && translate==false) //&& (component_hash.get("P").matches("(.*CODON.*|)") || codon_exist == true) 
						{
							component=tmVar.threetone_nu.get(components4[i]);
							exchange_nu=true;
						}
						else if(components4[i].length()>1)
						{
							NotAaminoacid=false;
							component=components4[i];
						}
						else
						{
							component=components4[i];
						}	
						if(component_hash.get("W").equals(""))
						{
							component_hash.put("W",component);
						}
						else
						{
							if(!component_avoidrepeat.containsKey(component))
							{
								component_hash.put("W",component_hash.get("W")+","+component);
							}
						}
						component_avoidrepeat.put(component,"");
					}
					//System.out.println(component_hash.get("W")+"\t"+component_hash.get("P")+"\t"+component_hash.get("M")+"\n");
					
					//W/M - 2
					if(component_hash.get("W").matches(",") && (component_hash.get("M").equals("") && component_hash.get("T").equals("")))
					{
						String spl[]=component_hash.get("W").split("-");
						component_hash.put("W",spl[0]);
						component_hash.put("M",spl[1]);
					}
					if(component_hash.get("M").matches(",") && (component_hash.get("W").equals("") && component_hash.get("T").equals("")))
					{
						String spl[]=component_hash.get("M").split("-");
						component_hash.put("W",spl[0]);
						component_hash.put("M",spl[1]);
					}
					
					if(component_hash.get("M").equals("CODON")) // TGA stop codon at nucleotides 766 to 768	ProteinMutation	p|SUB|X|766_768|CODON
					{
						component_hash.put("M","");
					}
					
					//A
					Pattern pap = Pattern.compile("[+-][0-9]");
					Matcher mp = pap.matcher(component_hash.get("P"));
					component_hash.put("A",component_hash.get("A").toLowerCase());
					if(component_hash.get("A").equals("") && component_hash.get("P").matches("[\\+]*[0-9]+") && (component_hash.get("P").matches("[\\-\\+]{0,1}[0-9]{1,8}")  && component_hash.get("W").matches("[ATCG]")  && component_hash.get("M").matches("[ATCG]") && Integer.parseInt(component_hash.get("P"))>3000)) 
					{
						component_hash.put("A","g");
					}
					else if(component_hash.get("A").equals("cdna"))
					{
						component_hash.put("A","c");
					}
					else if(component_hash.get("A").equals("") && mp.find() && !typelist.get(count).equals("ProteinMutation"))
					{
						component_hash.put("A","c");
					}
					else if(component_hash.get("W").matches("^[ATCG]*$") && component_hash.get("M").matches("^[ATCG]*$") && translate==false && component_hash.get("A").equals(""))
					{
						component_hash.put("A","c");
					}
					
					//F
					if(component_hash.get("F").equals("*"))
					{
						component_hash.put("F","X");
					}
					
					//R
					component_hash.put("R",component_hash.get("R").toLowerCase());
					component_hash.put("R",component_hash.get("R").replaceAll("[\\[\\]]", ""));
					
					//P
					Pattern pat = Pattern.compile("^([0-9]+)[^0-9,]+([0-9]{1,8})$");
					Matcher mat = pat.matcher(component_hash.get("P"));
					if(mat.find()) //Title|Abstract
		        	{
						if(Integer.parseInt(mat.group(1))<Integer.parseInt(mat.group(2)))
						{
							component_hash.put("P",mat.group(1)+"_"+mat.group(2));
						}
		        	}
					component_hash.put("P",component_hash.get("P").replaceAll("[-\\[]+$", ""));
					component_hash.put("P",component_hash.get("P").replaceAll("^(POSITION|NUCLEOTIDE|BASE|PAIR[S]*|NT|AA||:)", ""));
					component_hash.put("P",component_hash.get("P").replaceAll("^(POSITION|NUCLEOTIDE|BASE|PAIR[S]*|NT|AA||:)", ""));
					if(typelist.get(count).equals("ProteinMutation") || typelist.get(count).equals("ProteinAllele"))
					{
						component_hash.put("P",component_hash.get("P").replaceAll("^CODON", ""));	
					}
					else if(typelist.get(count).equals("DNAMutation")) 
					{
						if(codon_exist==true)
						{	
							//add codon back
							if(component_hash.get("P").matches("[0-9]+"))
							{
								component_hash.put("P","CODON"+component_hash.get("P"));
							}
							
							//if(component_hash.get("W").matches("[ATCGUatcgu]") && component_hash.get("M").matches("[ATCGUatcgu]"))
							//{
							//	//codon position * 3
							//	//codon position * 3 - 1
							//	//codon position * 3 - 2
							//	String position_wo_codon=component_hash.get("P").replaceAll("^CODON", "");
							//	int position1=Integer.parseInt(position_wo_codon)*3;
							//	int position2=position1-1;
							//	int position3=position1-2;
							//	component_hash.put("P",position1+","+position2+","+position3);
							//}
						}
					}
					
					//T
					component_hash.put("T",component_hash.get("T").toUpperCase());
					
					//refine the variant types
					for(String original_type : tmVar.VariantType_hash.keySet())
					{
						String formal_type=tmVar.VariantType_hash.get(original_type);
						if(component_hash.get("T").equals(original_type))
						{
							component_hash.put("T",formal_type);
							break;
						}
					}
					
					if(component_hash.get("T").matches(".*INS.*DEL.*")) {component_hash.put("T","INDEL"); translate=false;}
					else if(component_hash.get("T").matches(".*DEL.*INS.*")) {component_hash.put("T","INDEL"); translate=false;}
					else if(component_hash.get("T").matches(".*DUPLICATION.*")) {component_hash.put("T","DUP"); translate=false;} //multiple types : 15148206	778	859	27 bp duplication was found inserted in the 2B domain at nucleotide position 1222
					else if(component_hash.get("T").matches(".*INSERTION.*")) {component_hash.put("T","INS"); translate=false;} //multiple types
					else if(component_hash.get("T").matches(".*DELETION.*")) {component_hash.put("T","DEL"); translate=false;} //multiple types
					else if(component_hash.get("T").matches(".*INDEL.*")) {component_hash.put("T","INDEL"); translate=false;} //multiple types
					
					if(component_hash.get("T").matches("(DEL|INS|DUP|INDEL)") && !(component_hash.get("W").equals("")) && component_hash.get("M").equals(""))
					{
						component_hash.put("M",component_hash.get("W"));
					}
					else if(component_hash.get("M").matches("(DEL|INS|DUP|INDEL)"))
					{
						component_hash.put("T",component_hash.get("M"));
						component_hash.put("M","");
					}
					else if(component_hash.get("W").matches("(DEL|INS|DUP|INDEL)"))
					{
						component_hash.put("T",component_hash.get("W"));
						component_hash.put("W","");
					}
					else if(!component_hash.get("D").equals(""))
					{
						component_hash.put("T","DUP");
					}
					
					if(tmVar.Number_word2digit.containsKey(component_hash.get("M")))
					{
						component_hash.put("M",tmVar.Number_word2digit.get(component_hash.get("M")));
					}

					//System.out.println(component_hash.get("T")+"\t"+component_hash.get("W")+"\t"+component_hash.get("P")+"\t"+component_hash.get("M"));
					
					String type="";
					if(exchange_nu==true)
					{
						component_hash.put("P",component_hash.get("P").replaceAll("^CODON", ""));
						component_hash.put("A","p");
						type="ProteinMutation";
					}
					
					String identifier="";
					if(component_hash.get("T").equals("DUP") && !component_hash.get("D").equals("")) //dup
					{
						identifier=component_hash.get("A")+"|"+component_hash.get("T")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|"+component_hash.get("D");
					}
					else if(component_hash.get("T").equals("DUP")) //dup
					{
						identifier=component_hash.get("A")+"|"+component_hash.get("T")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|";
					}
					else if(!component_hash.get("T").equals("")) //DEL|INS|INDEL
					{
						identifier=component_hash.get("A")+"|"+component_hash.get("T")+"|"+component_hash.get("P")+"|"+component_hash.get("M");
					}
					else if(!component_hash.get("F").equals("")) //FS
					{
						identifier=component_hash.get("A")+"|FS|"+component_hash.get("W")+"|"+component_hash.get("P")+"|"+component_hash.get("M")+"|"+component_hash.get("S");
						type="ProteinMutation";
					}
					else if(!component_hash.get("R").equals("")) //RS
					{
						identifier=mentionlist.get(count);
						type="SNP";
					}
					else if(mentionlist.get(count).matches("^I([RSrs][Ss][0-9].+)"))
					{
						String men=mentionlist.get(count);
						men.substring(1, men.length());
						men=men.replaceAll("[\\W-_]", men.toLowerCase());
						type="SNP";
					}
					else if(component_hash.get("W").equals("") && !component_hash.get("M").equals("")) //Allele
					{
						identifier=component_hash.get("A")+"|Allele|"+component_hash.get("M")+"|"+component_hash.get("P");
					}
					else if(component_hash.get("M").equals("") && !component_hash.get("W").equals("")) //Allele
					{
						identifier=component_hash.get("A")+"|Allele|"+component_hash.get("W")+"|"+component_hash.get("P");
					}
					else if((!component_hash.get("M").equals("")) && (!component_hash.get("W").equals("")) && component_hash.get("P").equals("")) //AcidChange
					{
						identifier=component_hash.get("A")+"|SUB|"+component_hash.get("W")+"||"+component_hash.get("M");
					}
					else
					{
						identifier=component_hash.get("A")+"|SUB|"+component_hash.get("W")+"|"+component_hash.get("P")+"|"+component_hash.get("M");
					}
					
					// filteringNum_hash
					//if(component_hash.get("M").equals(component_hash.get("W")) && (!component_hash.get("W").equals("")) && type.equals("DNAMutation")) // remove genotype
					//{
					//	filteringNum_hash.put(count, "");
					//}
					//else if(NotAaminoacid==false && type.equals("ProteinMutation")) //E 243 ASPARTATE
					//{
					//	filteringNum_hash.put(count, "");
					//}
					//else if(component_hash.get("W").matches(",") && component_hash.get("M").matches(",") && component_hash.get("P").matches("")) //T,C/T,C
					//{
					//	filteringNum_hash.put(count, "");
					//}
					//if(component_hash.get("W").equals("") && component_hash.get("M").equals("") && component_hash.get("T").equals("") && !type.equals("SNP")) //exons 5
					//{
					//	filteringNum_hash.put(count, "");
					//}
					if(mentionlist.get(count).matches("^I[RSrs][Ss]") && type.equals("SNP")) //exons 5
					{
						filteringNum_hash.put(count, "");
					}
					
					// Recognize the type of mentions: ProteinMutation | DNAMutation | SNP 
					if(type.equals(""))
					{
						if(NotAaminoacid==true)
						{
							type="DNAMutation";
							if(!component_hash.get("A").matches("[gmrn]"))
							{
								component_hash.put("A","c");
							}
						}
						else if(translate==true || exchange_nu==true)
						{
							type="ProteinMutation";
						}
						else
						{
							type="DNAMutation";
						}
						
						if(component_hash.get("P").matches("^([Ee]x|EX|[In]ntron|IVS|[Ii]vs)"))
						{
							type="DNAMutation";
							identifier=identifier.replaceAll("^[^|]*\\|","c|");
						}
						else if(component_hash.get("M").matches("[ISQMNPKDFHLRWVEYX]") || component_hash.get("W").matches("[ISQMNPKDFHLRWVEYX]") )
						{
							type="ProteinMutation";
							identifier=identifier.replaceAll("^[^|]*\\|","p|");
						}
						else if(component_hash.get("P").matches(".*[\\+\\-\\?].*"))
						{
							type="DNAMutation";
							identifier=identifier.replaceAll("^[^|]*\\|","c|");
						}
						else if(type.equals(""))
						{
							type="DNAMutation";
						}
						
						if(!component_hash.get("A").equals("") &&
						   (
						   component_hash.get("A").toLowerCase().equals("c") ||
						   component_hash.get("A").toLowerCase().equals("r") ||
						   component_hash.get("A").toLowerCase().equals("m") ||
						   component_hash.get("A").toLowerCase().equals("g")
						   )
						  )
						{
							type="DNAMutation";
							identifier=identifier.replaceAll("^[^|]*\\|",component_hash.get("A")+"|");
						}
						else if(component_hash.get("A").equals("p"))
						{
							type="ProteinMutation";
						}
					}
					
					if(type.equals("ProteinMutation"))
					{
						identifier=identifier.replaceAll("^[^|]*\\|","p|");
					}
					
					if(type.equals("ProteinMutation") && (!component_hash.get("W").equals("")) && (!component_hash.get("M").equals("")) && component_hash.get("P").equals(""))
					{
						type="ProteinAcidChange";
					}
					else if(type.equals("DNAMutation") && (!component_hash.get("W").equals("")) && (!component_hash.get("M").equals("")) && component_hash.get("P").equals(""))
					{
						type="DNAAcidChange";
					}
					//else if(type.equals("ProteinMutation") && component_hash.get("T").equals("")  && component_hash.get("A").equals("") && (!component_hash.get("W").equals("")) && component_hash.get("W").equals(component_hash.get("M")))
					//{
					//	type="ProteinAllele";
					//	identifier=component_hash.get("A")+"|Allele|"+component_hash.get("M")+"|"+component_hash.get("P");
					//}
					//else if(type.equals("DNAMutation") && component_hash.get("T").equals("") && (!component_hash.get("W").equals("")) && component_hash.get("W").equals(component_hash.get("M")))
					//{
					//	type="DNAAllele";
					//	identifier=component_hash.get("A")+"|Allele|"+component_hash.get("M")+"|"+component_hash.get("P");
					//}
					
					//System.out.println(identifier+"\t"+component_hash.get("T")+"\t"+component_hash.get("W")+"\t"+component_hash.get("P")+"\t"+component_hash.get("M"));
					
					boolean show_removed_cases=false;
					// filtering and Print out
					if( (component_hash.get("W").length() == 3 || component_hash.get("M").length() == 3) 
							&& component_hash.get("W").length() != component_hash.get("M").length()
							&& !component_hash.get("W").equals("") && !component_hash.get("M").equals("") && component_hash.get("W").indexOf(",")!=-1 && component_hash.get("M").indexOf(",")!=-1
							&& ((component_hash.get("W").indexOf("A")!=-1 && component_hash.get("W").indexOf("T")!=-1 && component_hash.get("W").indexOf("C")!=-1 && component_hash.get("W").indexOf("G")!=-1) || (component_hash.get("M").indexOf("A")!=-1 && component_hash.get("M").indexOf("T")!=-1 && component_hash.get("M").indexOf("C")!=-1 && component_hash.get("M").indexOf("G")!=-1))
							&& component_hash.get("T").equals("")
						)
					{if(show_removed_cases==true){System.out.println("filtering 1:"+mentionlist.get(count));}identifierlist.add("_Remove_");}
					else if((component_hash.get("M").matches("[ISQMNPKDFHLRWVEYX]") || component_hash.get("W").matches("[ISQMNPKDFHLRWVEYX]")) && component_hash.get("P").matches("[6-9][0-9][0-9][0-9]+")){if(show_removed_cases==true){System.out.println("filtering 2 length:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //M300000X
					else if(component_hash.get("T").equals("DUP") && component_hash.get("M").matches("") && component_hash.get("P").equals("") && !type.equals("SNP")){if(show_removed_cases==true){System.out.println("filtering 3:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //|DUP|||4]q33-->qter	del[4] q33-->qter	del4q33qter
					//else if(component_hash.get("T").equals("") && component_hash.get("M").matches("[ATCGUatcgu]") && (component_hash.get("M").equals(component_hash.get("W")))){if(show_removed_cases==true){System.out.println("filtering 4:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //T --> T
					else if(component_hash.get("P").matches("[\\-\\<\\)\\]][0-9]+") && type.equals("ProteinMutation")){if(show_removed_cases==true){System.out.println("filtering 5:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //negative protein mutation
					else if(component_hash.get("P").matches(".*>.*")){if(show_removed_cases==true){System.out.println("filtering 6:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //negative protein mutation
					else if(component_hash.get("W").matches("^[BJOUZ]") || component_hash.get("M").matches("^[BJOUZ]")){if(show_removed_cases==true){System.out.println("filtering 7:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //not a mutation
					else if(component_hash.get("T").equals("&")){if(show_removed_cases==true){System.out.println("filtering 8:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //not a mutation
					else if((!component_hash.get("T").equals("")) && component_hash.get("P").equals("")){if(show_removed_cases==true){System.out.println("filtering 8-1:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //Delta32
					else if(component_hash.get("W").equals("") && component_hash.get("M").equals("") && component_hash.get("T").equals("") && (!type.equals("SNP")) && (!component_hash.get("P").matches(".*\\?.*"))){if(show_removed_cases==true){System.out.println("filtering 9:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //not a mutation
					else if(type.equals("SNP") && identifier.matches("RS[0-9]+")){if(show_removed_cases==true){System.out.println("filtering 2-1:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //start with RS (uppercase)
					else if(type.equals("SNP") && identifier.matches("[Rr][Ss][0-9][0-9]{0,1}")){if(show_removed_cases==true){System.out.println("filtering 2-2:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //too short rs number
					else if(type.equals("SNP") && identifier.matches("[Rr][Ss]0[0-9]*")){if(show_removed_cases==true){System.out.println("filtering 2-3:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //start with 0
					else if(type.equals("SNP") && identifier.matches("[Rr][Ss][0-9]{11,}")){if(show_removed_cases==true){System.out.println("filtering 2-4:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //too long rs number; allows 10 numbers or less
					else if(type.equals("SNP") && identifier.matches("[Rr][Ss][5-9][0-9]{9,}")){if(show_removed_cases==true){System.out.println("filtering 2-5:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //too long rs number; allows 10 numbers or less
					else if(type.equals("SNP") && identifier.matches("[0-9][0-9]{0,1}[\\W\\-\\_]*delta")){if(show_removed_cases==true){System.out.println("filtering 2-6:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //0 delta
					//else if(tmVar.PAM_lowerScorePair.contains(component_hash.get("M")+"\t"+component_hash.get("W"))){if(show_removed_cases==true){System.out.println("filtering 7-1:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //unlikely to occur
					//else if(component_hash.get("P").equals("") && component_hash.get("T").equals("") && (!type.equals("SNP"))){if(show_removed_cases==true){System.out.println("filtering 8-2:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //position is empty
					else if(component_hash.get("W").equals("") && component_hash.get("M").equals("") && component_hash.get("T").equals("") && (!type.equals("SNP")) && (!component_hash.get("P").contains("?"))){if(show_removed_cases==true){System.out.println("filtering 8-3:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //p.1234
					else if(component_hash.get("P").matches("[21][0-9][0-9][0-9]") && (component_hash.get("W").matches("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)") || component_hash.get("M").matches("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)"))){if(show_removed_cases==true){System.out.println("filtering 8-4:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //May 2018The
					else if(type.equals("AcidChange") && component_hash.get("W").equals(component_hash.get("M"))){if(show_removed_cases==true){System.out.println("filtering 8-5:"+mentionlist.get(count));}identifierlist.add("_Remove_");} //Met/Met Genotype
					else if(type.matches("DNAMutation|ProteinMutation") && component_hash.get("T").equals("") && (component_hash.get("W").length() != component_hash.get("M").length()) && component_hash.get("W").length()>0 && component_hash.get("M").length()>0 && (!component_hash.get("P").matches(".*\\?.*")) && (!component_hash.get("W").matches(".*,.*")) && (!component_hash.get("M").matches(".*,.*"))){if(show_removed_cases==true){System.out.println("filtering 8-6:"+mentionlist.get(count)+"\t"+component_hash.get("W")+"\t"+component_hash.get("M"));}identifierlist.add("_Remove_");} //GUCA1A
					else
					{
						identifierlist.add(identifier);
						if(typelist.get(count).matches("DNAAllele|ProteinAllele")){}
						else
						{
							typelist.set(count,type);
						}
					}
					
					//End
					component_hash.put("A", "");
					component_hash.put("T", "");
					component_hash.put("P", "");
					component_hash.put("W", "");
					component_hash.put("M", "");
					component_hash.put("F", "");
					component_hash.put("S", "");
					component_hash.put("D", "");
					component_hash.put("I", "");
					component_hash.put("R", "");
					count++;
					start_count=0;
				}
				else if(outputs[1].equals("I"))
				{
					//Start
					codon_exist=false;
				}
				else if(outputs[outputs.length-1].matches("[ATPWMFSDIR]"))
				{
					if(WMstate_hash.containsKey(count) && WMstate_hash.get(count).equals("Forward"))
					{
						if(start_count<boundary_hash.get(count) && outputs[outputs.length-1].equals("M"))
						{
							outputs[outputs.length-1]="W";
						}
						else if(start_count>boundary_hash.get(count) && outputs[outputs.length-1].equals("W"))
						{
							outputs[outputs.length-1]="M";
						}
					}
					else if(WMstate_hash.containsKey(count) && WMstate_hash.get(count).equals("Backward"))
					{
						if(start_count<boundary_hash.get(count) && outputs[outputs.length-1].equals("W"))
						{
							outputs[outputs.length-1]="M";
						}
						else if(start_count>boundary_hash.get(count) && outputs[outputs.length-1].equals("M"))
						{
							outputs[outputs.length-1]="W";
						}
					}
					String state=outputs[outputs.length-1];	
					String tkn=outputs[0];
					
					if(!component_hash.get(state).equals("") && !state.equals(prestate)) //add "," if the words are not together
					{
						component_hash.put(state, component_hash.get(state)+","+tkn);
					}
					else
					{
						component_hash.put(state, component_hash.get(state)+tkn);
					}
					prestate=state;
					if(outputs[0].toLowerCase().equals("codon"))
					{
						codon_exist=true;
					}
				}
				start_count=start_count+outputs[0].length();
			}
			PostMEfile.close();
			
			HashMap<String,String> mention2id_hash = new HashMap<String,String>();
			for(int i=0;i<count;i++)
			{
				if(!filteringNum_hash.containsKey(i) && (!identifierlist.get(i).equals("_Remove_")))
				{
					if(typelist.get(i).equals("SNP"))
					{
						String id_rev = identifierlist.get(i);
						id_rev=id_rev.replaceAll(",","");
						identifierlist.set(i, id_rev);
					}
					mention2id_hash.put(mentionlist.get(i),typelist.get(i)+"\t"+identifierlist.get(i));
				}
			}
			
			String text=" ";
			String pmid="";
			BufferedWriter PubTatorfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenamePubTator), "UTF-8")); // .location
			PostMEfile = new BufferedReader(new InputStreamReader(new FileInputStream(FilenamePostME), "UTF-8"));
			while ((line = PostMEfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					pmid=mat.group(1);
					text=text+mat.group(3)+" ";
					PubTatorfile.write(line+"\n");
	        	}
				else if(line.contains("\t")) //Annotation
	        	{
					String outputs[]=line.split("\\t");
					pmid=outputs[0];
					String start=outputs[1];
					String last=outputs[2];
					String mention=outputs[3];
					String mention_tmp=mention.replaceAll(" ", "");
					
					
					if(Integer.parseInt(start)>16 && text.length()>20 && text.substring(Integer.parseInt(start)-15,Integer.parseInt(start)).matches(".*(Figure|Figures|Table|Fig|Figs|Tab|Tabs|figure|figures|table|fig|figs|tab|tabs)[ \\.].*"))
					{
						//System.out.println(start+"  "+last+"  "+"\t"+mention+"  "+text.substring(Integer.parseInt(start)-15,Integer.parseInt(start)));
	        		}
					else if((!tmVar.filteringStr_hash.containsKey(mention_tmp)) && mention2id_hash.containsKey(mention))
					{
						if(mention.matches(".* at")){}
						else if(mention.matches("C [0-9]+H")){}
						else
						{
							mention2id_hash.put(mention,mention2id_hash.get(mention).replaceAll(" ",""));
							PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+mention+"\t"+mention2id_hash.get(mention)+"\n");
						}
					}
	        	}
				else
				{
					Pattern pat_chro1 = Pattern.compile("^(.*?[\\W\\-\\_])((Chromosome|chromosome|chr|Chr)[ ]*([0-9XY]+)[ \\:]+([0-9][0-9,\\. ]*[0-9])[ ]*([\\-\\_]|to){1,}[ ]*([0-9][0-9,\\. ]*[0-9])[ ]*([MmKk]bp|[MmKk]b|[MmKk]bs|[MmKk]bps|c[MmKk]|bp|bps|b|)[ ]*(del\\/dup|del[0-9,]*|dup[0-9,]*|ins[0-9,]*|indel[0-9,]*|[\\+\\-]|))([\\W\\-\\_].*)$");
					Matcher mat_chro1 = pat_chro1.matcher(text);
					while(mat_chro1.find())
					{
						String pre=mat_chro1.group(1);
						String men=mat_chro1.group(2);
						String CNV_chr=mat_chro1.group(4);
						String CNV_start=mat_chro1.group(5);
						String CNV_last=mat_chro1.group(7);
						String unit=mat_chro1.group(8);
						String type=mat_chro1.group(9);
						String post=mat_chro1.group(10);
						CNV_start=CNV_start.replaceAll("[, ]","");
						CNV_last=CNV_last.replaceAll("[, ]","");
						if(type.equals("-")) {type="del";}
						else if(type.equals("+")) {type="dup";}
						else
						{
							int pre_distance=100;
							if(pre.length()<100) {pre_distance=pre.length();}
							String pre_last100char=pre.substring(pre.length()-pre_distance);
							int loca_del_dup=pre_last100char.lastIndexOf("deletion/duplication");
							int loca_dup_del=pre_last100char.lastIndexOf("duplication/deletion");
							int loca_del=pre_last100char.lastIndexOf("deletion");
							int loca_del2=pre_last100char.lastIndexOf("loss");
							int loca_dup=pre_last100char.lastIndexOf("duplication");
							int loca_dup2=pre_last100char.lastIndexOf("gain");
							if((loca_del_dup!=-1) || (loca_dup_del!=-1))
							{
								type="del/dup";
							}
							else if(loca_del>-1 && loca_del>loca_dup)
							{
								type="del";
							}
							else if(loca_del2>-1 && loca_del2>loca_dup)
							{
								type="del";
							}
							else if(loca_dup>-1 && loca_dup>loca_del)
							{
								type="dup";
							}
							else if(loca_dup2>-1 && loca_dup2>loca_del)
							{
								type="dup";
							}
						}
						
						String CNV_id="Chr"+CNV_chr+":"+CNV_start+"-"+CNV_last+type;
						if(unit.toUpperCase().lastIndexOf("M")!=-1)
						{
							CNV_id="Chr"+CNV_chr+":"+CNV_start+"-"+CNV_last+"M"+type;
						}
						else if(unit.toUpperCase().lastIndexOf("K")!=-1)
						{
							CNV_id="Chr"+CNV_chr+":"+CNV_start+"-"+CNV_last+"K"+type;
						}
						else
						{
							CNV_start=CNV_start.replaceAll("[\\.]","");
							CNV_last=CNV_last.replaceAll("[\\.]","");
						}
						int start=pre.length();
						int last=start+men.length();
						if(type.equals(""))
						{
							PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tGenomicRegion\t"+CNV_id+"\n");
						}
						else
						{
							PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tCopyNumberVariant\t"+CNV_id+"\n");
						}
						men=men.replaceAll(".","@");
						text=pre+men+post;
						mat_chro1 = pat_chro1.matcher(text);
					}
					
					pat_chro1 = Pattern.compile("^(.*?[\\W\\-\\_])((Chromosome|chromosome|chr|Chr)[ ]*([0-9XY]+)[ \\:]+([0-9][0-9,\\. ]*[0-9])[ ]*([MmKk]bp|[MmKk]b|[MmKk]bs|[MmKk]bps|c[MmKk]|bp|bps|b|))([\\W\\-\\_].*)$");
					mat_chro1 = pat_chro1.matcher(text);
					while(mat_chro1.find())
					{
						String pre=mat_chro1.group(1);
						String men=mat_chro1.group(2);
						String CNV_chr=mat_chro1.group(4);
						String CNV_start=mat_chro1.group(5);
						String unit=mat_chro1.group(6);
						String post=mat_chro1.group(7);
						CNV_start=CNV_start.replaceAll("[, ]","");
						String CNV_id="Chr"+CNV_chr+":"+CNV_start;
						if(unit.toUpperCase().lastIndexOf("M")!=-1)
						{
							CNV_id="Chr"+CNV_chr+":"+CNV_start+"M";
						}
						else if(unit.toUpperCase().lastIndexOf("K")!=-1)
						{
							CNV_id="Chr"+CNV_chr+":"+CNV_start+"K";
						}
						else
						{
							CNV_start=CNV_start.replaceAll("[\\.]","");
						}
						int start=pre.length();
						int last=start+men.length();
						PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tGenomicRegion\t"+CNV_id+"\n");
						men=men.replaceAll(".","@");
						text=pre+men+post;
						mat_chro1 = pat_chro1.matcher(text);
					}
					
					pat_chro1 = Pattern.compile("^(.*?[\\W\\-\\_])((chromosome|chromosome band|chr)[ ]*(([0-9XY]+)([pq][0-9\\.]+|)))([\\W\\-\\_].*)$");
					mat_chro1 = pat_chro1.matcher(text);
					while(mat_chro1.find())
					{
						String pre=mat_chro1.group(1);
						String men=mat_chro1.group(2);
						String chro_num=mat_chro1.group(5);
						String post=mat_chro1.group(7);
						if(men.matches("^(.+)\\.$"))
						{
							men=men.substring(0, men.length()-1);
							post="."+post;
						}
						int start=pre.length();
						int last=start+men.length();
						PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tChromosome\t"+chro_num+"\n");
						men=men.replaceAll(".","@");
						text=pre+men+post;
						mat_chro1 = pat_chro1.matcher(text);
					}
					
					pat_chro1 = Pattern.compile("^(.*?[\\W\\-\\_])(([0-9XY]+)[pq][0-9\\-\\.]+)([\\W\\-\\_].*)$");
					mat_chro1 = pat_chro1.matcher(text);
					while(mat_chro1.find())
					{
						String pre=mat_chro1.group(1);
						String men=mat_chro1.group(2);
						String chro_num=mat_chro1.group(3);
						String post=mat_chro1.group(4);
						if(men.matches("^(.+)\\.$"))
						{
							men=men.substring(0, men.length()-1);
							post="."+post;
						}
						int start=pre.length();
						int last=start+men.length();
						PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tChromosome\t"+chro_num+"\n");
						men=men.replaceAll(".","@");
						text=pre+men+post;
						mat_chro1 = pat_chro1.matcher(text);
					}
					
					pat_chro1 = Pattern.compile("^(.*?[\\W\\-\\_])([NX][MPGCRT]\\_[0-9\\.]+)([\\W\\-\\_].*)$");
					mat_chro1 = pat_chro1.matcher(text);
					while(mat_chro1.find())
					{
						String pre=mat_chro1.group(1);
						String men=mat_chro1.group(2);
						String post=mat_chro1.group(3);
						if(men.matches("^(.+)\\.$"))
						{
							men=men.substring(0, men.length()-1);
							post="."+post;
						}
						int start=pre.length();
						int last=start+men.length();
						PubTatorfile.write(pmid+"\t"+start+"\t"+last+"\t"+men+"\tRefSeq\t"+men+"\n");
						men=men.replaceAll(".","@");
						text=pre+men+post;
						mat_chro1 = pat_chro1.matcher(text);
					}
					PubTatorfile.write(line+"\n");
					text="";
				}
			}
			PostMEfile.close();
			PubTatorfile.close();
		}
		catch(IOException e1){ System.out.println("[output2PubTator]: "+e1+" Input file is not exist.");}
	}
	
	public void Normalization(String input,String outputPubTator,String finalPubTator,String DisplayRSnumOnly, String HideMultipleResult, String DisplayChromosome, String DisplayRefSeq, String DisplayGenomicRegion) throws IOException,SQLException
	{
		/**
		 * input : gene mentions
		 * outputPubTator : mutation mentions
		 * finalPubTator : normalized result of mutation mentions
		 */
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);	
		
		/*Database Connection*/
		Connection c = null;
		Statement stmt = null;
		Connection c_merged = null;
		Statement stmt_merged = null;
		Connection c_expired = null;
		Statement stmt_expired = null;
		Connection c_gene2rs = null;
		Statement stmt_gene2rs = null;
		Connection c_rs2gene = null;
		Statement stmt_rs2gene = null;
		Connection c_gene2tax = null;
		Statement stmt_gene2tax = null;
		Connection c_gene2humangene = null;
		Statement stmt_gene2humangene = null;
		
		try {
			Class.forName("org.sqlite.JDBC");
		} 
		catch ( Exception e ) 
		{
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		
		try {
			
			c_merged = DriverManager.getConnection("jdbc:sqlite:Database/Merged.db");
			stmt_merged = c_merged.createStatement();
			
			c_expired = DriverManager.getConnection("jdbc:sqlite:Database/Expired.db");
			stmt_expired = c_expired.createStatement();
			
			c_gene2rs = DriverManager.getConnection("jdbc:sqlite:Database/gene2rs.db");
			stmt_gene2rs = c_gene2rs.createStatement();
			
			c_rs2gene = DriverManager.getConnection("jdbc:sqlite:Database/rs2gene.db");
			stmt_rs2gene = c_rs2gene.createStatement();
			
			c_gene2tax = DriverManager.getConnection("jdbc:sqlite:Database/gene2tax.db");
			stmt_gene2tax = c_gene2tax.createStatement();
			
			c_gene2humangene = DriverManager.getConnection("jdbc:sqlite:Database/gene2humangene.db");
			stmt_gene2humangene = c_gene2humangene.createStatement();
			
			
			
			/**
			 * Gene Mention Extraction
			 */
			HashMap<String,String> rs2gene = new HashMap<String,String>(); // the corresponding gene of the rs number
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
			HashMap<String,String> pmid_gene2rs = new HashMap<String,String>(); // pmid_gene2rs(pmid,geneid) -> RS#s
			HashMap<String,String> annotations_gene = new HashMap<String,String>();
			String line="";
			HashMap<String,HashMap<String,Integer>> SingleGene = new HashMap<String,HashMap<String,Integer>>();
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
				{
					String Pmid=mat.group(1);
					SingleGene.put(Pmid,new HashMap<String,Integer>());
				}
	        	else if (line.contains("\t")) //Gene mentions
	        	{
	        		String anno[]=line.split("\t");
	        		String Pmid=anno[0];
	        		//System.out.println(line);
					if(anno.length>=6)
	        		{
						int start=Integer.parseInt(anno[1]);
						String gene_mention=anno[3];
	        			String mentiontype=anno[4];
	        			if(gene_mention.toLowerCase().equals("raf"))
	        			{
	        				anno[5]=anno[5]+";673";
	        			}
	        			boolean variant_gene_overlap=false;
	        			if(tmVar.variant_mention_to_filter_overlap_gene.containsKey(Pmid))
	        			{
	        				if(tmVar.variant_mention_to_filter_overlap_gene.get(Pmid).containsKey(start))
	        				{
	        					
	        				}
	        				
	        				for(int var_mention_start : tmVar.variant_mention_to_filter_overlap_gene.get(Pmid).keySet())
		        			{
		        				if(var_mention_start == start) // the gene mention is the same to one of the variant mention
		        				{
		        					variant_gene_overlap=true;
		        				}
		        			}
	        			}
	        					
	        			if(mentiontype.equals("Gene") && variant_gene_overlap==false)
	        			{
	        				/*
	        				 * Search Database - Gene
	        				 */
	        				String geneids[]=anno[5].split(";");
	        				
	        				for(int gi=0;gi<geneids.length;gi++)
	        				{
	        					annotations_gene.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+geneids[gi],"");
	        					if(SingleGene.get(anno[0]).containsKey(geneids[gi]))
	        					{
	        						SingleGene.get(anno[0]).put(geneids[gi],SingleGene.get(anno[0]).get(geneids[gi])+1);
	        					}
	        					else
	        					{
	        						SingleGene.get(anno[0]).put(geneids[gi],1);
	        					}
	        					
	        					/* pmid_gene2rs(pmid,geneid) -> RS#s */
	        					/* annotations_gene(pmid,start,last,mention,geneid) -> RS#s */
	        					if(pmid_gene2rs.containsKey(anno[0]+"\t"+geneids[gi]))
		        				{
		        					annotations_gene.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+geneids[gi],pmid_gene2rs.get(anno[0]+"\t"+geneids[gi]));
		        				}
		        				else
		        				{
		        					String gene_id=geneids[gi];
		        					if(tmVar.Gene2HumanGene_hash.containsKey(gene_id)) //translate to human gene
		        					{
		        						gene_id=tmVar.Gene2HumanGene_hash.get(gene_id);
		        					}
		        					ResultSet rs = stmt_gene2rs.executeQuery("SELECT rs FROM gene2rs WHERE gene='"+gene_id+"' order by rs asc limit 1;");
		        					if ( rs.next() ) 
		        					{
		        				         String  rsNumber = rs.getString("rs");
		        				         annotations_gene.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+geneids[gi],rsNumber);
		        				         pmid_gene2rs.put(anno[0]+"\t"+geneids[gi],rsNumber);
										String rsNumbers[]=rsNumber.split("\\|");
										for(int i=0;i<rsNumbers.length;i++)
										{
											rs2gene.put(Pmid+"\t"+rsNumbers[i],geneids[gi]);
										}
		        				    }
	        					}
	        				}
	        			}
	        		}
	        	}
			}
			inputfile.close();
			
			/**
			 * Chromosome & RefSeq Mention Extraction
			 */
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(outputPubTator), "UTF-8"));
			HashMap<String,String> annotations_CNV = new HashMap<String,String>();
			HashMap<String,String> annotations_chromosome = new HashMap<String,String>();
			HashMap<String,String> annotations_RefSeq = new HashMap<String,String>();
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) {}//Title|Abstract
	        	else if (line.contains("\t")) //Annotation
	        	{
					String anno[]=line.split("\t");
	        		if(anno.length>=6)
	        		{
	        			String mentiontype=anno[4];
	        			if(mentiontype.equals("Chromosome"))
	        			{
	        				String chr_id=anno[5];
	        				annotations_chromosome.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+chr_id,"chr"+chr_id);
	        			}
	        			else if(mentiontype.equals("RefSeq"))
	        			{
	        				String RefSeq=anno[3];
	        				annotations_RefSeq.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+RefSeq,RefSeq); 
	        			}
	        		}
	        	}
			}
			inputfile.close();
			
			/**
			 * RS_DNA_Protein Pattern Extraction
			 */
			String article="";
			String Pmid="";
			HashMap<String,Integer> Flag_Protein_WPM=new HashMap<String,Integer>();
			HashMap<String,Integer> Flag_DNA_gt_Format=new HashMap<String,Integer>();
			HashMap<String,String> RSnumberList = new HashMap<String,String>();
			HashMap<Integer,String> start2id = new HashMap<Integer,String>(); //tmp
			HashMap<String,String> MentionPatternMap2rs = new HashMap<String,String>(); //Assign RS# to the DNA/Protein mutations in the same pattern range
			HashMap<String,String> MentionPatternMap = new HashMap<String,String>();  // group DNA/Protein mutations (will assign the same RS# if possible)
			HashMap<String,String> MentionPatternMap2rs_extend = new HashMap<String,String>(); //by pattern [PMID:24073655] c.169G>C (p.Gly57Arg) -- map to rs764352037
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(outputPubTator), "UTF-8"));
			
			HashMap<String,ArrayList<Integer>> PMID2SentenceOffsets = new HashMap<String,ArrayList<Integer>>();
			
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					Pmid = mat.group(1);
					String ParagraphContent=mat.group(3);
					article=article+ParagraphContent+"\t";
					
					// split sentences (get the start offset of every sentence)
					iterator.setText(article);
					ArrayList<Integer> Sentence_offsets = new ArrayList<Integer>();
					int Sent_start = iterator.first();
					for (int Sent_last = iterator.next(); Sent_last != BreakIterator.DONE; Sent_start = Sent_last, Sent_last = iterator.next()) 
					{
						Sentence_offsets.add(Sent_start);
					}
					PMID2SentenceOffsets.put(Pmid, Sentence_offsets);
				}
				else if (line.contains("\t")) //Annotation
		    	{
					String anno[]=line.split("\t");
	        		if(anno.length>=6)
	        		{
	        			Pmid = anno[0];
	        			int start = Integer.parseInt(anno[1]);
    	        		int last = Integer.parseInt(anno[2]);
    	        		String mention=anno[3];
    	        		String mentiontype=anno[4];
    	        		if(mention.matches("^c\\.[0-9]+[ ]*[ATCG][ ]*>[ ]*[ATCG]$")) // if exists 12345A>G, or A1556E or E1356A, the A154T should be protein mutation 
    	        		{
    	        			if(Flag_DNA_gt_Format.containsKey(Pmid))
    	        			{
    	        				Flag_DNA_gt_Format.put(Pmid,Flag_DNA_gt_Format.get(Pmid)+1);
    	        			}
    	        			else
    	        			{
    	        				Flag_DNA_gt_Format.put(Pmid,1);
    	        			}
    	        		}
    	        		else if(mention.matches("^[ATCGISQMNPKDFHLRWVEYX][0-9]+[ISQMNPKDFHLRWVEYX]$") || mention.matches("^[ATCGISQMNPKDFHLRWVEYX][0-9]+[ISQMNPKDFHLRWVEYX]$"))
    					{
    	        			if(Flag_Protein_WPM.containsKey(Pmid))
    	        			{
    							Flag_Protein_WPM.put(Pmid,Flag_Protein_WPM.get(Pmid)+1);
    	        			}
    	        			else
    	        			{
    	        				Flag_Protein_WPM.put(Pmid,1);
    	        			}
    					}
    	        		
    					if(anno[5].matches("\\|.*"))
    	        		{
    	        			if(mentiontype.matches(".*Protein.*"))
    	        			{
    	        				anno[5]="p"+anno[5];
        	        		}
    	        			else
    	        			{
    	        				anno[5]="c"+anno[5];
    	        			}
    	        		}
    	        		String id=anno[5];
    	        		
    	        		if(mentiontype.matches("DNA.*"))
	        			{
    	        			if(article.substring(start,last).equals(mention))
        	        		{
        	        			String tmp = mention;
        	        			tmp=tmp.replaceAll(".","D");
        	        			article=article.substring(0,start)+tmp+article.substring(last,article.length()); // replace DNAMutation|DNAAllele|DNAAcidChange to DDDDD
        	        		}
    	        			start2id.put(start,id);
	        			}
    	        		else if(mentiontype.matches("Protein.*"))
	        			{
    	        			if(article.substring(start,last).equals(mention))
        	        		{
        	        			String tmp = mention;
        	        			tmp=tmp.replaceAll(".","P");
        	        			article=article.substring(0,start)+tmp+article.substring(last,article.length()); // replace ProteinMutation|ProteinAllele|ProteinAcidChange to PPPPP
        	        		}
    	        			start2id.put(start,id);
	        			}
    	        		else if(mentiontype.equals("SNP"))
	        			{
	        				anno[5]=anno[5].replaceAll("^rs","");
	        				RSnumberList.put(anno[0]+"\t"+anno[5],"");
	        				
	        				if(article.substring(start,last).equals(mention))
        	        		{
        	        			String tmp = mention;
        	        			tmp=tmp.replaceAll(".","S");
        	        			article=article.substring(0,start)+tmp+article.substring(last,article.length()); // replace SNP to SSSSS
        	        		}
	        				start2id.put(start,id);
	        			}
	        		}
	        	}
				else if(line.length()==0)
				{
					String text=article;
					
					for(int i=0;i<tmVar.RS_DNA_Protein.size();i++) // patterns : PP[P]+[ ]*[\(\[][ ]*DD[D]+[ ]*[\)\]][ ]*[\(\[][ ]*SS[S]+[ ]*[\)\]]
					{
						pat = Pattern.compile("^(.*?)("+tmVar.RS_DNA_Protein.get(i)+")");
						mat = pat.matcher(text);
						while(mat.find())
						{
							String pre=mat.group(1);
							String match_string=mat.group(2);
							int start=pre.length();
							int last=start+match_string.length();
							if(!match_string.matches(".*[\\W\\_\\-](and|or)[\\W\\_\\-].*")) // the variants are different.
							{
								ArrayList<String> DP = new ArrayList<String>();
								String S = ""; // the RS# in the pattern
								
								for(int s : start2id.keySet())
								{
									if(s>=start && s<last)
									{
										Pattern pat_rs = Pattern.compile("[Rr][Ss]([0-9]+)$");
										Matcher mat_rs = pat_rs.matcher(start2id.get(s));
										if(mat_rs.find())
										{
											S = mat_rs.group(1);
										}
										else
										{
											DP.add(start2id.get(s));
										}
									}
								}
								if(!S.equals("")) //RS number is in the pattern
								{
									for(int dp=0;dp<DP.size();dp++)
									{
										MentionPatternMap2rs.put(Pmid+"\t"+DP.get(dp),S); // assign the RS# to the DNA/Protein Mutations
									}
								}
								else 
								{
									if(DP.size()>1)
									{
										//group the DNA/Protein mutations together
										MentionPatternMap.put(Pmid+"\t"+DP.get(0),DP.get(1)); 
										MentionPatternMap.put(Pmid+"\t"+DP.get(1),DP.get(0));
									}
								}
							}	
							String tmp = mat.group(2);
							tmp=tmp.replaceAll(".", "F"); //finished
							text = text.substring(0, start)+tmp+text.substring(last,text.length());
							pat = Pattern.compile("^(.*?)("+tmVar.RS_DNA_Protein.get(i)+")"); // next run
							mat = pat.matcher(text);
						}
					}
					
					article="";
					start2id.clear();
				}
			}
			inputfile.close();
			
			
			/**
			 * DNAMutation|ProteinMutation normalization
			 */
			BufferedWriter outputfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(finalPubTator), "UTF-8")); // .location
			inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(outputPubTator), "UTF-8"));
			HashMap<String,String> normalized_mutation = new HashMap<String,String>();
			HashMap<String,String> changetype = new HashMap<String,String>(); //eg., G469A from DNA to Protein mutation
			HashMap<String,String> mutation2rs_indatabase = new HashMap<String,String>();
			HashMap<String,String> rs_foundbefore_hash = new HashMap<String,String>();
			HashMap<String,String> rs_foundinText_hash = new HashMap<String,String>();
			HashMap<String,HashMap<String,String>> P2variant = new HashMap<String,HashMap<String,String>>();
			HashMap<String,HashMap<String,String>> WM2variant = new HashMap<String,HashMap<String,String>>();
			String outputSTR="";
			String tiabs="";
			String pmid="";
			String articleid="";
			while ((line = inputfile.readLine()) != null)  
			{
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					outputSTR=outputSTR+line+"\n";
					pmid=mat.group(1);
					tiabs=tiabs+mat.group(3)+"\t";
	        	}
				else if (line.contains("\t")) //Annotation
	        	{
					String anno[]=line.split("\t");
					String mentiontype=anno[4];
					String mention=anno[3];
					String Flag_Seq="";
					if(mention.matches("^[mgcrn]\\..*"))
					{
						Flag_Seq="c";
						mentiontype=mentiontype.replaceAll("Protein","DNA");
					}
					else if(mention.matches("^p\\..*"))
					{
						Flag_Seq="p";
						mentiontype=mentiontype.replaceAll("DNA","Protein");
					}
					else if(mention.matches(".*[ATCG]>[ATCG]"))
					{
						Flag_Seq="c";
						mentiontype=mentiontype.replaceAll("Protein","DNA");
					}
					else if(mention.matches("[A-Z][a-z][a-z][0-9]+[A-Z][a-z][a-z]"))
					{
						Flag_Seq="p";
						anno[5]=anno[5].replaceAll("[mgcrnc]\\|","p|");
						mentiontype=mentiontype.replaceAll("DNA","Protein");
					}
					else if(mention.matches("^[ATCG][0-9]+[ATCG]$") && ((Flag_Protein_WPM.containsKey(Pmid) && Flag_DNA_gt_Format.containsKey(Pmid)) || (Flag_Protein_WPM.containsKey(Pmid) && Flag_Protein_WPM.get(Pmid)>50))) //5236G>C and G1738R --> G1706A (ProteinMutation)
					{
						Flag_Seq="p";
						anno[5]=anno[5].replaceAll("c\\|","p|");
						mentiontype=mentiontype.replaceAll("DNA","Protein");
					}
					
					if(anno.length>=6)
	        		{
	        			if(mentiontype.matches("(DNAMutation|ProteinMutation|ProteinAllele|DNAAllele|DNAAcidChange|ProteinAcidChange)"))
	        			{
	        				int start=Integer.parseInt(anno[1]);
	        				int pre_start=0;
	        				if(start>20){pre_start=start-20;}
	        				String pre_text=tiabs.substring(pre_start, start);
	        				Pattern pat_NM = Pattern.compile("^.*([NX][MPGCRT]\\_[0-9\\.\\_]+[\\:\\; ]+)$");
	        				Matcher mat_NM = pat_NM.matcher(pre_text);
	        				if(mat_NM.find()) //NM_001046020:c.12A>G
	        				{
	        					String RefSeq=mat_NM.group(1);
	        					line=anno[0]+"\t"+(start-RefSeq.length())+"\t"+anno[2]+"\t"+RefSeq+anno[3]+"\t"+mentiontype+"\t"+anno[5];
	        				}
	        				
	        				String component[]=anno[5].split("\\|",-1);
	        				String NormalizedForm="";
	        				String NormalizedForm_reverse="";
	        				String NormalizedForm_plus1="";
	        				String NormalizedForm_minus1="";
	        				String NormalizedForm_protein="";
	        				
	        				/*
	        				 *  translate the tmVar format to HGVS format
	        				 */
	        				if(component.length>=3)
	        				{
	        					if(component[1].equals("Allele"))
		        				{
	        						//to extract the location and WM for variant grouping
	        						String Seq="c";
	        						if(component[0].matches("[gcnrm]")){Seq="c";}
	        						if(!component[3].equals(""))
	        						{
			        					if(!P2variant.containsKey(pmid+"\t"+Seq+"\tSUB\t"+component[3])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\tSUB\t"+component[3],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\tSUB\t"+component[3]).put(pmid+"\t"+anno[5],"");
	        						}
		        				}
	        					else if(component[1].equals("SUB"))
		        				{
	        						//to extract the location and WM for variant grouping
	        						String Seq="c";
	        						if(component[0].matches("[gcnrm]")){Seq="c";}
	        						if(!component[3].equals(""))
	        						{
			        					if(!P2variant.containsKey(pmid+"\t"+Seq+"\tSUB\t"+component[3])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\tSUB\t"+component[3],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\tSUB\t"+component[3]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(!WM2variant.containsKey(pmid+"\t"+Seq+"\tSUB\t"+component[2]+"\t\t"+component[4])) //W,M
		        					{
		        						WM2variant.put(pmid+"\t"+Seq+"\tSUB\t"+component[2]+"\t\t"+component[4],new HashMap<String,String>());
		        					}
		        					WM2variant.get(pmid+"\t"+Seq+"\tSUB\t"+component[2]+"\t\t"+component[4]).put(pmid+"\t"+anno[5],component[3]);
	        						
		        					if(component[0].equals("p"))
			        				{
		        						String tmp="";
		        						/*one -> three*/
		        						for(int len=0;len<component[2].length();len++)
		        						{
		        							if(tmVar.one2three.containsKey(component[2].substring(len, len+1)))
		        							{
		        								if(tmp.equals(""))
		        								{
		        									tmp = tmVar.one2three.get(component[2].substring(len, len+1));
		        								}
		        								else
		        								{
		        									tmp = tmp +","+ tmVar.one2three.get(component[2].substring(len, len+1));
		        								}
		        							}
		        						}
		        						component[2]=tmp;
		        						tmp="";
		        						for(int len=0;len<component[4].length();len++)
		        						{
		        							if(tmVar.one2three.containsKey(component[4].substring(len, len+1)))
		        							{
		        								if(tmp.equals(""))
		        								{
		        									tmp = tmVar.one2three.get(component[4].substring(len, len+1));	
		        								}
		        								else
		        								{	     
		        									tmp = tmp +","+ tmVar.one2three.get(component[4].substring(len, len+1));	
		        								}
		        							}
		        						}
		        						component[4]=tmp;
		        						
		        						if(component[2].equals(component[4]))
		        						{
		        							NormalizedForm=component[0]+"."+component[2]+component[3]+"=";
		        						}
		        						else
		        						{
			        						NormalizedForm=component[0]+"."+component[2]+component[3]+component[4];
			        						NormalizedForm_reverse=component[0]+"."+component[4]+component[3]+component[2];
		        						}
			        					String wildtype[]=component[2].split(",");
			        					String mutant[]=component[4].split(",");
			        					String positions[]=component[3].split(",");
			        					
			        					if(wildtype.length == positions.length && wildtype.length>1) //Pair of wildtype&position
			        					{
			        						for (int i=0;i<wildtype.length;i++) //May have more than one pair
				        					{
				        						for (int j=0;j<mutant.length;j++)
					        					{
				        							NormalizedForm=NormalizedForm+"|"+component[0]+"."+wildtype[i]+positions[i]+mutant[j];
					        					}
				        					}
			        					}
			        					else
			        					{
			        						for (int i=0;i<wildtype.length;i++) //May have more than one pair
				        					{
				        						for (int j=0;j<mutant.length;j++)
					        					{
				        							NormalizedForm=NormalizedForm+"|"+component[0]+"."+wildtype[i]+component[3]+mutant[j];
					        					}
				        					}
			        					}
			        				}
		        					else //[rmgc]
		        					{
		        						if(component.length>4)
		    	        				{
			        						component[3]=component[3].replaceAll("^\\+", "");
			        						NormalizedForm=component[0]+"."+component[3]+component[2]+">"+component[4];
			        						NormalizedForm_reverse=component[0]+"."+component[3]+component[4]+">"+component[2];
			        						if(component[3].matches("[\\+\\-]{0,1}[0-9]{1,8}"))
			        						{
				        						NormalizedForm_plus1=component[0]+"."+(Integer.parseInt(component[3])+1)+""+component[2]+">"+component[4];
				        						NormalizedForm_minus1=component[0]+"."+(Integer.parseInt(component[3])-1)+""+component[2]+">"+component[4];
			        						}
				        					String wildtype[]=component[2].split(",");
				        					String mutant[]=component[4].split(",");
				        					String positions[]=component[3].split(",");
				        					
				        					if(wildtype.length == positions.length && wildtype.length>1) //Pair of wildtype&position
				        					{
				        						for (int i=0;i<wildtype.length;i++) //May have more than one pair
					        					{
					        						for (int j=0;j<mutant.length;j++)
						        					{
					        							NormalizedForm=NormalizedForm+"|"+component[0]+"."+positions[i]+wildtype[i]+">"+mutant[j];
						        					}
					        					}
				        					}
				        					else
				        					{
					        					for (int i=0;i<wildtype.length;i++) //May have more than one pair
					        					{
					        						for (int j=0;j<mutant.length;j++)
						        					{
					        							NormalizedForm=NormalizedForm+"|"+component[0]+"."+component[3]+wildtype[i]+">"+mutant[j];
						        					}
					        					}
					        					component[3]=component[3].replaceAll(",", "");
				        					}
				        					if(component[3].matches("[0-9]{5,}"))
				        					{
				        						component[0]="g";
				        						NormalizedForm=NormalizedForm+"|"+component[0]+"."+component[3]+component[2]+">"+component[4];
				        					}
				        					
				        					//protein
			        						{
				        						String tmp="";
				        						/*one -> three*/
				        						for(int len=0;len<component[2].length();len++)
				        						{
				        							if(tmVar.one2three.containsKey(component[2].substring(len, len+1)))
				        							{
				        								if(tmp.equals(""))
				        								{
				        									tmp = tmVar.one2three.get(component[2].substring(len, len+1));
				        								}
				        								else
				        								{
				        									tmp = tmp +","+ tmVar.one2three.get(component[2].substring(len, len+1));
				        								}
				        							}
				        						}
				        						component[2]=tmp;
				        						tmp="";
				        						for(int len=0;len<component[4].length();len++)
				        						{
				        							if(tmVar.one2three.containsKey(component[4].substring(len, len+1)))
				        							{
				        								if(tmp.equals(""))
				        								{
				        									tmp = tmVar.one2three.get(component[4].substring(len, len+1));	
				        								}
				        								else
				        								{	     
				        									tmp = tmp +","+ tmVar.one2three.get(component[4].substring(len, len+1));	
				        								}
				        							}
				        						}
				        						component[4]=tmp;
				        						
				        						if(component[2].equals(component[4]))
				        						{
				        							NormalizedForm_protein="p."+component[2]+component[3]+"=";
				        						}
				        						else
				        						{
				        							NormalizedForm_protein="p."+component[2]+component[3]+component[4];
					        					}
					        					wildtype=component[2].split(",");
					        					mutant=component[4].split(",");
					        					positions=component[3].split(",");
					        					
					        					if(wildtype.length == positions.length && wildtype.length>1) //Pair of wildtype&position
					        					{
					        						for (int i=0;i<wildtype.length;i++) //May have more than one pair
						        					{
						        						for (int j=0;j<mutant.length;j++)
							        					{
						        							NormalizedForm_protein=NormalizedForm_protein+"|"+"p."+wildtype[i]+positions[i]+mutant[j];
							        					}
						        					}
					        					}
					        					else
					        					{
					        						for (int i=0;i<wildtype.length;i++) //May have more than one pair
						        					{
						        						for (int j=0;j<mutant.length;j++)
							        					{
						        							NormalizedForm_protein=NormalizedForm_protein+"|"+"p."+wildtype[i]+component[3]+mutant[j];
							        					}
						        					}
					        					}
				        					}
		    	        				}
			        				}
		        				}
		        				else if(component[1].equals("DEL"))
		        				{
		        					//to extract the location and WM for variant grouping
		        					String Seq="c";
		        					//if(component[0].matches("[gcnrm]")){component[0]="c";}
	        						if(!component[2].equals(""))
	        						{
			        					if(!P2variant.containsKey(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(component.length>3)
		        					{
		        						if(component[0].equals("p"))
				        				{
			        						String tmp="";
			        						for(int len=0;len<component[3].length();len++)
			        						{
			        							tmp = tmp + tmVar.one2three.get(component[3].substring(len, len+1));
			        						}
			        						component[3]=tmp;
			        					}
			        					NormalizedForm=component[0]+"."+component[2]+"del"+component[3];
		        					}
		        					NormalizedForm=NormalizedForm+"|"+component[0]+"."+component[2]+"del";
		        				}
		        				else if(component[1].equals("INS"))
		        				{
		        					//to extract the location and WM for variant grouping
		        					String Seq="c";
		        					//if(component[0].matches("[gcnrm]")){component[0]="c";}
	        						if(!component[2].equals(""))
	        						{
			        					if(!P2variant.containsKey(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(component.length>3)
		        					{
		        						if(component[0].equals("p"))
				        				{
			        						String tmp="";
			        						for(int len=0;len<component[3].length();len++)
			        						{
			        							tmp = tmp + tmVar.one2three.get(component[3].substring(len, len+1));
			        						}
			        						component[3]=tmp;
			        					}
			        					NormalizedForm=component[0]+"."+component[2]+"ins"+component[3];
		        					}
		        					NormalizedForm=NormalizedForm+"|"+component[0]+"."+component[2]+"ins";
		        				}
		        				else if(component[1].equals("INDEL"))
		        				{
		        					//to extract the location and WM for variant grouping
		        					String Seq="c";
		        					//if(component[0].matches("[gcnrm]")){component[0]="c";}
	        						if(!component[2].equals(""))
	        						{
				        				if(!P2variant.containsKey(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(component.length>3)
		        					{
		        						//c.*2361_*2362delAAinsA
			        					//c.2153_2155delinsTCCTGGTTTA
			        					if(component[0].equals("p"))
				        				{
			        						String tmp="";
			        						for(int len=0;len<component[3].length();len++)
			        						{
			        							tmp = tmp + tmVar.one2three.get(component[3].substring(len, len+1));
			        						}
			        						component[3]=tmp;
			        					}
			        					NormalizedForm=component[0]+"."+component[2]+"delins"+component[3];
		        					}
		        				}
		        				else if(component[1].equals("DUP"))
		        				{
		        					//to extract the location and WM for variant grouping
		        					String Seq="c";
		        					//if(component[0].matches("[gcnrm]")){component[0]="c";}
	        						if(!component[2].equals(""))
	        						{
				        				if(!P2variant.containsKey(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\t"+component[1]+"\t"+component[2]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(component.length>3)
		        					{
		        						if(component[0].equals("p"))
				        				{
			        						String tmp="";
			        						for(int len=0;len<component[3].length();len++)
			        						{
			        							tmp = tmp + tmVar.one2three.get(component[3].substring(len, len+1));
			        						}
			        						component[3]=tmp;
			        					}
			        					NormalizedForm=component[0]+"."+component[2]+"dup"+component[3];
		        					}
		        					NormalizedForm=NormalizedForm+"|"+component[0]+"."+component[2]+"dup";
		        				}
		        				else if(component[1].equals("FS"))
		        				{
		        					//to extract the location and WM for variant grouping
		        					String Seq="c";
		        					//if(component[0].matches("[gcnrm]")){component[0]="c";}
	        						if(!component[3].equals(""))
	        						{
			        					if(!P2variant.containsKey(pmid+"\t"+Seq+"\tSUB\t"+component[3])) //position
			        					{
			        						P2variant.put(pmid+"\t"+Seq+"\tSUB\t"+component[3],new HashMap<String,String>());
			        					}
			        					P2variant.get(pmid+"\t"+Seq+"\tSUB\t"+component[3]).put(pmid+"\t"+anno[5],"");
	        						}
		        					if(component[0].equals("p"))
			        				{
		        						String tmp="";
		        						for(int len=0;len<component[2].length();len++)
		        						{
		        							tmp = tmp + tmVar.one2three.get(component[2].substring(len, len+1));
		        						}
		        						component[2]=tmp;
		        						
		        						tmp="";
		        						if(component.length>=5)
		        						{
			        						for(int len=0;len<component[4].length();len++)
			        						{
			        							tmp = tmp + tmVar.one2three.get(component[4].substring(len, len+1));
			        						}
			        						component[4]=tmp;
		        						}
		        					}
		        					
		        					if(component.length>=5)
		        					{
		        						NormalizedForm=component[0]+"."+component[2]+component[3]+component[4]+"fs";
		        					}
		        					else if(component.length==4)
		        					{
		        						NormalizedForm=component[0]+"."+component[2]+component[3]+"fs";
		        					}
		        				}
	        				}
	        				
	        				// array --> hash --> array (remove the duplicates)
	        				String NormalizedForms[]=NormalizedForm.split("\\|");
	        				HashMap<String,String> NormalizedForm_hash = new HashMap<String,String>();
	        				for(int n=0;n<NormalizedForms.length;n++)
	        				{
	        					NormalizedForm_hash.put(NormalizedForms[n], "");
	        				}
	        				NormalizedForm="";
	        				for(String NF : NormalizedForm_hash.keySet())
	        				{
	        					if(NormalizedForm.equals(""))
	        					{
	        						NormalizedForm=NF;
	        					}
	        					else
	        					{
	        						NormalizedForm=NormalizedForm+"|"+NF;
	        					}
	        				}
	        				
	        				//Assign RS# to the DNA/Protein mutations in the same pattern range
	        				if(MentionPatternMap2rs.containsKey(anno[0]+"\t"+anno[5]))
	        				{
	        					String RSNum=MentionPatternMap2rs.get(anno[0]+"\t"+anno[5]);
	        					
        						//Expired
	        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
	        					if ( rs.next() ) 
	        					{
	        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Pattern-RS#:"+RSNum+"(Expired)\n";
	        				    }
	        					else
	        					{
	        						//Merged
	        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
		        					if ( rs.next() ) 
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Pattern-RS#:"+rs.getString("merged")+"\n";
		        					}
		        					else
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Pattern-RS#:"+RSNum+"\n";
		        					}
	        					}
	        				}
	        				else if(rs_foundinText_hash.containsKey(anno[0]+"\t"+NormalizedForm)) //recognized (RS# in the text, no need to compare with gene-related-RS#)
	        				{
	        					String RSNum=rs_foundinText_hash.get(anno[0]+"\t"+NormalizedForm);
	        					
        						//Expired
	        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
	        					if ( rs.next() ) 
	        					{
	        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Recognized-RS#:"+RSNum+"(Expired)\n";
	        				    }
	        					else
	        					{
	        						//Merged
	        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
		        					if ( rs.next() ) 
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Recognized-RS#:"+rs.getString("merged")+"\n";
		        					}
		        					else
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Recognized-RS#:"+RSNum+"\n";
		        					}
	        					}
	        				}
	        				else if(rs_foundbefore_hash.containsKey(anno[0]+"\t"+NormalizedForm)) //already found by dictionary-lookup (no need to repeat the action)
	        				{
	        					String RSNum=rs_foundbefore_hash.get(anno[0]+"\t"+NormalizedForm);
	        					
	        					//Expired
	        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
	        					if ( rs.next() ) 
	        					{
	        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Foundbefore-RS#:"+RSNum+"(Expired)\n";
	        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum+"(Expired)");
		        				}
	        					else
	        					{
	        						//Merged
	        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
		        					if ( rs.next() ) 
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Foundbefore-RS#:"+rs.getString("merged")+"\n";
		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],rs.getString("merged"));
			        				}
		        					else
		        					{
		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Foundbefore-RS#:"+RSNum+"\n";
		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum);
			        				}
	        					}
	        				}
	        				else
	        				{
	        					boolean found=false;
		        				String tmVarid_tmp=anno[5]; //c|SUB|C|1749|T
		        				tmVarid_tmp=tmVarid_tmp.replaceAll("^\\+", "");
	        					
		        				/**
		        				 * 1. compare with history
		        				 * 2. compare with RS# in text (the mutation should exist in database)
		        				 * 3. compare with RefSeq
		        				 * 4. compare with gene2rs
		        				 * 5. Extended
		        				 * 6. compare with chromosome#
		        				 */
		        				
		        				/*
		        				 *  1. compare with history
		        				 */
		        				for(String pmid_geneid : pmid_gene2rs.keySet())
	        					{
		        					String pmid_geneid_split[] = pmid_geneid.split("\t");
	        						String pmidID=pmid_geneid_split[0];
	        						String geneid=pmid_geneid_split[1];
	        						if(pmidID.equals(anno[0]))
	        						{
	        							if(tmVar.Mutation_RS_Geneid_hash.containsKey(tmVarid_tmp+"\t"+geneid)) //in history
			        					{
		        							String RSNum=tmVar.Mutation_RS_Geneid_hash.get(tmVarid_tmp+"\t"+geneid);
		        							
		        							//Expired
	    		        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
	    		        					if ( rs.next() ) 
	    		        					{
	    		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Mapping-RS#:"+RSNum+"(Expired)\n";
	    		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum+"(Expired)");
	    			        				}
	    		        					else
	    		        					{
	    		        						//Merged
	    		        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
	    			        					if ( rs.next() ) 
	    			        					{
	    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Mapping-RS#:"+rs.getString("merged")+"\n";
	    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],rs.getString("merged"));
	    				        				}
	    			        					else
	    			        					{
	    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Mapping-RS#:"+RSNum+"\n";
	    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum);
	    				        				}
	    		        					}
		        							found=true;
		        							break;
		        						}
	        						}
	        					}
		        				
		        				if(found == false)
	        					{
	        						HashMap<String,String> rs2var = new HashMap<String,String>();
	        						
	        						
			        				//Search database for normalization
			        				if(normalized_mutation.containsKey(anno[5])) // The variant has been normalized
			        				{
			        					mutation2rs_indatabase.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5],normalized_mutation.get(anno[5]));
			        				}
			        				else
			        				{
				        				try {
				        					String DB="var2rs_p";
				        					String Table="var2rs_p";
				        					if(component[0].equals("p"))
					        				{	
				        						DB="var2rs_p";
				        						Table="var2rs_p";
				        					}
				        					else if(component[0].equals("c") || component[0].equals("r"))
					        				{	
				        						DB="var2rs_c";
				        						Table="var2rs_c";
				        					}
				        					else if(component[0].equals("m"))
					        				{	
				        						DB="var2rs_m";
				        						Table="var2rs_m";
				        					}
				        					else if(component[0].equals("n"))
					        				{	
				        						DB="var2rs_n";
				        						Table="var2rs_n";
				        					}
				        					else if(component[0].equals("g"))
					        				{	
				        						//DB="var2rs_g";Table="var2rs_g";
				        						if(component[3].matches("^10.*$")) {DB="var2rs_g.10";Table="var2rs";}
				        						else if(component[3].matches("^11.*$")) {DB="var2rs_g.11";Table="var2rs";}
				        						else if(component[3].matches("^12.*$")) {DB="var2rs_g.12";Table="var2rs";}
				        						else if(component[3].matches("^13.*$")) {DB="var2rs_g.13";Table="var2rs";}
				        						else if(component[3].matches("^14.*$")) {DB="var2rs_g.14";Table="var2rs";}
				        						else if(component[3].matches("^15.*$")) {DB="var2rs_g.15";Table="var2rs";}
				        						else if(component[3].matches("^16.*$")) {DB="var2rs_g.16";Table="var2rs";}
				        						else if(component[3].matches("^17.*$")) {DB="var2rs_g.17";Table="var2rs";}
				        						else if(component[3].matches("^18.*$")) {DB="var2rs_g.18";Table="var2rs";}
				        						else if(component[3].matches("^19.*$")) {DB="var2rs_g.19";Table="var2rs";}
				        						else if(component[3].matches("^2.*$")) {DB="var2rs_g.2";Table="var2rs";}
				        						else if(component[3].matches("^3.*$")) {DB="var2rs_g.3";Table="var2rs";}
				        						else if(component[3].matches("^4.*$")) {DB="var2rs_g.4";Table="var2rs";}
				        						else if(component[3].matches("^5.*$")) {DB="var2rs_g.5";Table="var2rs";}
				        						else if(component[3].matches("^6.*$")) {DB="var2rs_g.6";Table="var2rs";}
				        						else if(component[3].matches("^7.*$")) {DB="var2rs_g.7";Table="var2rs";}
				        						else if(component[3].matches("^8.*$")) {DB="var2rs_g.8";Table="var2rs";}
				        						else if(component[3].matches("^9.*$")) {DB="var2rs_g.9";Table="var2rs";}
				        						else{DB="var2rs_g.19";Table="var2rs";}
				        					}
				        					
				        					String rsNumber="";
				        					
				        					//N prefix
				        					{
				        						c = DriverManager.getConnection("jdbc:sqlite:Database/"+DB+".db");
					        					stmt = c.createStatement();
					        					String NormalizedForm_arr[]=NormalizedForm.split("\\|");
					        					String SQL="SELECT rs,var FROM "+Table+" WHERE ";
							        			for(int nfa=0;nfa<NormalizedForm_arr.length;nfa++)
					        					{
							        				SQL=SQL+"var='"+NormalizedForm_arr[nfa]+"' or ";
					        					}
					        					SQL=SQL.replaceAll(" or $", "");
					        					ResultSet rs = stmt.executeQuery(SQL+" order by var");
					        					while ( rs.next() ) 
					        					{
					        						String rss[]=rs.getString("rs").split("\\|");
					        						for(int rsi=0;rsi<rss.length;rsi++)
					        						{
					        							rs2var.put(rss[rsi], rs.getString("var"));
					        						}
					        						if(rsNumber.equals(""))
					        						{
					        							rsNumber = rs.getString("rs");
					        						}
					        						else
					        						{
					        							rsNumber = rsNumber+"|"+rs.getString("rs");
					        						}
					        					}
				        						stmt.close();
				        						c.close();
				        						rsNumber=rsNumber+"|end";
				        					}
			        			
				        					//NormalizedForm_reverse
				        					if(!NormalizedForm_reverse.equals(""))
				        					{
					        					c = DriverManager.getConnection("jdbc:sqlite:Database/"+DB+".db");
					        					stmt = c.createStatement();
					        					String NormalizedForm_arr[]=NormalizedForm_reverse.split("\\|");
					        					String SQL="SELECT rs,var FROM "+Table+" WHERE ";
							        			for(int nfa=0;nfa<NormalizedForm_arr.length;nfa++)
					        					{
							        				SQL=SQL+"var='"+NormalizedForm_arr[nfa]+"' or ";
					        					}
					        					SQL=SQL.replaceAll(" or $", "");
					        					ResultSet rs = stmt.executeQuery(SQL+" order by var");
					        					while ( rs.next() ) 
					        					{
					        						String rss[]=rs.getString("rs").split("\\|");
					        						for(int rsi=0;rsi<rss.length;rsi++)
					        						{
					        							rs2var.put(rss[rsi], rs.getString("var"));
					        						}
					        						if(rsNumber.equals(""))
					        						{
					        							rsNumber = rs.getString("rs");
					        						}
					        						else
					        						{
					        							rsNumber = rsNumber+"|"+rs.getString("rs");
					        						}
					        					}
				        						stmt.close();
				        						c.close();
				        						rsNumber=rsNumber+"|end";
				        					}
				        					
			        						//NormalizedForm - proteinchange
			        						if(!Flag_Seq.equals("c"))
			        						{
			        							c = DriverManager.getConnection("jdbc:sqlite:Database/var2rs_clinvar_proteinchange.db");
					        					stmt = c.createStatement();
					        					String NormalizedForm_arr[]=NormalizedForm.split("\\|");
					        					String SQL="SELECT rs,var FROM var2rs_clinvar_proteinchange WHERE ";
					        					for(int nfa=0;nfa<NormalizedForm_arr.length;nfa++)
					        					{
							        				SQL=SQL+"var='"+NormalizedForm_arr[nfa]+"' or ";
					        					}
					        					SQL=SQL.replaceAll(" or $", "");
					        					ResultSet rs = stmt.executeQuery(SQL+" order by var");
					        					while ( rs.next() ) 
					        					{
					        						String rss[]=rs.getString("rs").split("\\|");
					        						for(int rsi=0;rsi<rss.length;rsi++)
					        						{
					        							rs2var.put(rss[rsi], rs.getString("var"));
					        						}
					        						if(rsNumber.equals(""))
					        						{
					        							rsNumber = rs.getString("rs");
					        						}
					        						else
					        						{
					        							rsNumber = rsNumber+"|"+rs.getString("rs");
					        						}
					        					}
				        						stmt.close();
				        						c.close();
				        						rsNumber=rsNumber+"|end";
			        						}
			        						
			        						//NormalizedForm - reverse_proteinchange
			        						if(!Flag_Seq.equals("c"))
				        					{
			        							c = DriverManager.getConnection("jdbc:sqlite:Database/var2rs_clinvar_proteinchange.db");
				        						stmt = c.createStatement();
					        					String NormalizedForm_arr[]=NormalizedForm_reverse.split("\\|");
					        					String SQL="SELECT rs,var FROM var2rs_clinvar_proteinchange WHERE ";
					        					for(int nfa=0;nfa<NormalizedForm_arr.length;nfa++)
					        					{
							        				SQL=SQL+"var='"+NormalizedForm_arr[nfa]+"' or ";
					        					}
					        					SQL=SQL.replaceAll(" or $", "");
					        					ResultSet rs = stmt.executeQuery(SQL+" order by var");
					        					while ( rs.next() ) 
					        					{
					        						String rss[]=rs.getString("rs").split("\\|");
					        						for(int rsi=0;rsi<rss.length;rsi++)
					        						{
					        							rs2var.put(rss[rsi], rs.getString("var"));
					        						}
					        						if(rsNumber.equals(""))
					        						{
					        							rsNumber = rs.getString("rs");
					        						}
					        						else
					        						{
					        							rsNumber = rsNumber+"|"+rs.getString("rs");
					        						}
					        					}
				        						stmt.close();
				        						c.close();
				        						rsNumber=rsNumber+"|end";
			        						}
			        						
			        						//if DNA cannot found, try protein
			        						if((!NormalizedForm_protein.equals("")) && (!Flag_Seq.equals("c")))
			        						{
			        							DB="var2rs_p";
				        						Table="var2rs_p";
				        						boolean found_changetype=false;
				        						c = DriverManager.getConnection("jdbc:sqlite:Database/"+DB+".db");
					        					stmt = c.createStatement();
					        					String NormalizedForm_arr[]=NormalizedForm_protein.split("\\|");
					        					String SQL="SELECT rs,var FROM "+Table+" WHERE ";
					        					for(int nfa=0;nfa<NormalizedForm_arr.length;nfa++)
					        					{
							        				SQL=SQL+"var='"+NormalizedForm_arr[nfa]+"' or ";
					        					}
					        					SQL=SQL.replaceAll(" or $", "");
					        					ResultSet rs = stmt.executeQuery(SQL+" order by var");
					        					while ( rs.next() ) 
					        					{
					        						found_changetype=true;
					        						String rss[]=rs.getString("rs").split("\\|");
					        						for(int rsi=0;rsi<rss.length;rsi++)
					        						{
					        							rs2var.put(rss[rsi], rs.getString("var"));
					        						}
					        						if(rsNumber.equals(""))
					        						{
					        							rsNumber = rs.getString("rs");
					        						}
					        						else
					        						{
					        							rsNumber = rsNumber+"|"+rs.getString("rs");
					        						}
					        					}
				        						stmt.close();
				        						c.close();
				        						rsNumber=rsNumber+"|end";
					        					
			        						}
			        						
			        						//System.out.println(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]+"\t"+rsNumber);
			        						mutation2rs_indatabase.put(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5],rsNumber);
			        				        normalized_mutation.put(anno[5],rsNumber);
			        				    } 
			        					catch ( SQLException e ) 
			        					{
			        						System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			        					}
			        				}
			        				
		        					/*
		        					 *  2. compare with co-occurred RS# in text (the mutation should exist in database)
		        					 */
			        				if(mutation2rs_indatabase.containsKey(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5])) //the mutation mention can find RS#s in dictionary
			        				{
			        					String rsNumbers_arr[] = mutation2rs_indatabase.get(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]).split("\\|");
				        				for(int ra=0;ra<rsNumbers_arr.length;ra++)
				        				{
				        					String RSNum=rsNumbers_arr[ra];
				        					pat = Pattern.compile("^([0-9]+)[\\-\\/](.*)$");
					    					mat = pat.matcher(rsNumbers_arr[ra]);
					    					if(mat.find())
					    		        	{
					    						RSNum = mat.group(1);
					    					}
					    					
					    					if(RSnumberList.containsKey(anno[0]+"\t"+RSNum)) //one of the RS#s is in the text, no need to normalized by gene
					        				{
					    						rs_foundinText_hash.put(anno[0]+"\t"+NormalizedForm, RSNum);
				        						
				        						//Expired
					    						ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
					        					if ( rs.next() ) 
		    		        					{
		    		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";SNPinText-RS#:"+RSNum+"(Expired)\n";
		    		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum+"(Expired)");
		    			        				}
		    		        					else
		    		        					{
		    		        						//Merged
		    		        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
		    			        					if ( rs.next() ) 
		    			        					{
		    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";SNPinText-RS#:"+rs.getString("merged")+"\n";
		    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],rs.getString("merged"));
		    				        				}
		    			        					else
		    			        					{
		    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";SNPinText-RS#:"+RSNum+"\n";
		    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum);
		    				        				}
		    		        					}
		    		        					
		    	        						ra = rsNumbers_arr.length; //last
					        					found=true;
					        				}
				        				}
				        			}
			        				
			        				/*
		        					 * 3. compare with RefSeq
		        					 */
		        					if(found == false)
			        				{
		        						HashMap<String,String> Unique_RefSeq = new HashMap<String,String>();
		        						for(String RefSeq : annotations_RefSeq.keySet())
				        				{
		        							String RefSeq_anno[] = RefSeq.split("\t"); //pmid,start,last,mention,RefSeq
		        							if(anno[0].equals(RefSeq_anno[0]) && RefSeq_anno.length>=5)
				        					{
				        						Unique_RefSeq.put(annotations_RefSeq.get(RefSeq),"");
				        					}
				        				}
		        						
		        						if(mutation2rs_indatabase.containsKey(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]))
		        						{
				        					String rsNumbers_arr[]=mutation2rs_indatabase.get(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]).split("\\|");
					        				for(int ra=0;ra<rsNumbers_arr.length;ra++)
					        				{
					        					String RSNum=rsNumbers_arr[ra];
					        					pat = Pattern.compile("^([0-9]+)[\\-\\/](.*)$");
						    					mat = pat.matcher(rsNumbers_arr[ra]);
						    					if(mat.find())
						    		        	{
						    						RSNum = mat.group(1);
						    						String Chro_RefSeqs = mat.group(2);
						    						String Chro_RefSeq_arr[] = Chro_RefSeqs.split("/");
						        					for(int ch=0;ch<Chro_RefSeq_arr.length;ch++)
						        					{
						        						if(Unique_RefSeq.containsKey(Chro_RefSeq_arr[ch]))
						        						{
						        							//Expired
					    		        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
					    		        					if ( rs.next() ) 
					    		        					{
					    		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";RefSeq-RS#:"+RSNum+"(Expired)\n";
					    		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum+"(Expired)");
					    			        				}
					    		        					else
					    		        					{
					    		        						//Merged
					    		        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
					    			        					if ( rs.next() ) 
					    			        					{
					    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";RefSeq-RS#:"+rs.getString("merged")+"\n";
					    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],rs.getString("merged"));
					    				        				}
					    			        					else
					    			        					{
					    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";RefSeq-RS#:"+RSNum+"\n";
					    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum);
					    				        				}
					    		        					}
					    		        					
					    	        						ra = rsNumbers_arr.length; //last
					    	        						found=true;
					    	        					}
						        					}
						    					}
					        				}
		        						}
			        				}
		        					
		        					/*
		        					 * 4. compare with gene2rs
		        					 */
		        					if(found == false) // can't find relevant RS# in text 
			        				{
			        					HashMap<String,Integer> geneRS2distance_hash= new HashMap<String,Integer>();
			        					HashMap<Integer,ArrayList<String>> sentencelocation_gene_hash= new HashMap<Integer,ArrayList<String>>(); //sentence --> gene ids
			        					int sentencelocation_rs=0;
			        					int sentencelocation_gene=0;
			        					
			        					//detect the sentence boundaries for gene and RS#
		        						for(String gene : annotations_gene.keySet())
				        				{
		        							String gene_anno[] = gene.split("\t"); //pmid,start,last,mention,geneid
				        					if(anno[0].equals(gene_anno[0]) && gene_anno.length>=5)
				        					{
				        						ArrayList<Integer> SentenceOffsets = PMID2SentenceOffsets.get(anno[0]);
				        						for(int si=0;si<SentenceOffsets.size();si++) // find sentence location for gene mention
				        						{
				        							if(Integer.parseInt(gene_anno[1])<SentenceOffsets.get(si)) //gene_anno[1] = start
				        							{
				        								if(!sentencelocation_gene_hash.containsKey(si-1))
				        								{
				        									sentencelocation_gene_hash.put(si-1,new ArrayList<String>());
				        								}
				        								sentencelocation_gene_hash.get(si-1).add(gene_anno[4]); //gene_info[4] = gene identifier
				        								sentencelocation_gene=si-1;
				        								break;
				        							}
				        						}
				        						for(int si=0;si<SentenceOffsets.size();si++) // find sentence location for rs mention 
				        						{
				        							if(Integer.parseInt(anno[1])<SentenceOffsets.get(si))
				        							{
				        								sentencelocation_rs=si-1;
				        								break;
				        							}
				        						}
				        										        						
				        						if(Integer.parseInt(gene_anno[2])<=Integer.parseInt(anno[1])) // gene --- mutation
				        						{
				        							int distance = Integer.parseInt(anno[1])-Integer.parseInt(gene_anno[2]);
				        							if(sentencelocation_gene != sentencelocation_rs){distance=distance+1000;}
				        							if(!geneRS2distance_hash.containsKey(annotations_gene.get(gene)))
				        							{
					        							geneRS2distance_hash.put(annotations_gene.get(gene),distance);
				        							}
				        							else if(distance<geneRS2distance_hash.get(annotations_gene.get(gene)))
				        							{
				        								geneRS2distance_hash.put(annotations_gene.get(gene),distance);
				        							}
				        						}
				        						else if(Integer.parseInt(gene_anno[1])>=Integer.parseInt(anno[2])) // mutation --- gene
				        						{
				        							int distance = Integer.parseInt(gene_anno[1])-Integer.parseInt(anno[2]);
				        							if(sentencelocation_gene != sentencelocation_rs){distance=distance+1000;}
				        							if(!geneRS2distance_hash.containsKey(annotations_gene.get(gene)))
				        							{
				        								geneRS2distance_hash.put(annotations_gene.get(gene),distance);
				        							}
				        							else if(distance<geneRS2distance_hash.get(annotations_gene.get(gene)))
				        							{
				        								geneRS2distance_hash.put(annotations_gene.get(gene),distance);
				        							}
				        						}
				        						else // mutation & gene may overlap
				        						{
				        							geneRS2distance_hash.put(annotations_gene.get(gene),0);
				        						}
				        					}
				        				}
				        				
		        						// ranking the geneRS based on the distance
				        				ArrayList <String> geneRS_ranked = new ArrayList <String>();
				        				while(!geneRS2distance_hash.isEmpty())
				        				{
				        					int closet_distance=10000000;
					        				String closet_geneRS="";
					        				for (String geneRS : geneRS2distance_hash.keySet())
					        				{
					        					if(geneRS2distance_hash.get(geneRS)<closet_distance)
					        					{
					        						closet_distance=geneRS2distance_hash.get(geneRS);
					        						closet_geneRS=geneRS;
					        					}
					        				}
					        				if(closet_geneRS.equals(""))
					        				{	
						        				break;
					        				}
					        				geneRS_ranked.add(closet_geneRS);
					        				geneRS2distance_hash.remove(closet_geneRS);
				        				}
				        				
				        				// if at least one gene and RS# are in the same sentence, only look at those genes in the same sentence.
				        				int number_geneRS=geneRS_ranked.size();
				        				/*
				        				if(number_geneRS>0 && sentencelocation_gene_hash.containsKey(sentencelocation_rs)) 
				        				{
				        					if(number_geneRS>sentencelocation_gene_hash.get(sentencelocation_rs).size())
				        					{
				        						number_geneRS=sentencelocation_gene_hash.get(sentencelocation_rs).size();
				        					}
				        				}
				        				*/
				        				
				        				for(int rsg=0;rsg<number_geneRS;rsg++)
			        					{
				        					String target_gene_rs=geneRS_ranked.get(rsg);
				        					String geneRSs[]=target_gene_rs.split("\\|");
					        				if(mutation2rs_indatabase.containsKey(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]))
					        				{
					        					String rsNumbers_arr[]=mutation2rs_indatabase.get(anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[5]).split("\\|");
						        				String found_rs = "";
						        				
						        				/*
						        				 * Compare the RS#s found by Gene and Variants
						        				 * RS#s (Gene) <-----> RS#s (Variant)
						        				 */
						        				HashMap<String,Integer> rshash = new HashMap<String,Integer>();
						        				String lastvar="";
						        				for(int ra=0;ra<rsNumbers_arr.length;ra++)
						        				{
						        					String RSNum=rsNumbers_arr[ra];
						        					pat = Pattern.compile("^([0-9]+)[\\-\\/](.*)$");
							    					mat = pat.matcher(rsNumbers_arr[ra]);
							    					if(mat.find())
							    		        	{
							    						RSNum = mat.group(1);
							    					}
							    					
						        					for(int g=0;g<geneRSs.length;g++)
						        					{
						        						if(found==true && RSNum.equals("end"))
						        						{
						        							//leave the loop
						        							ra=rsNumbers_arr.length;
						        							g=geneRSs.length;
						        						}
						        						else if(geneRSs[g].equals(RSNum) && !geneRSs[g].equals(""))
						        						{
						        							if(rshash.containsKey(geneRSs[g]))
						        							{
						        								// removed duplicated RSs : RS#:747319628|747319628
						        							}
						        							else
						        							{
							        							found=true;
							        							String currentvar="";
							        							if(rs2var.containsKey(RSNum)){currentvar=rs2var.get(RSNum);}
							        							if(found_rs.equals(""))
							        							{
							        								found_rs = geneRSs[g];
							        							}
							        							else
							        							{
							        								if(lastvar.equals("") || lastvar.equals(currentvar))
							        								{
							        									found_rs = found_rs+"|"+ geneRSs[g];
							        								}
							        								else
							        								{
							        									found_rs = found_rs+";"+ geneRSs[g];
							        								}
							        							}
							        							lastvar=currentvar;
							        							rshash.put(geneRSs[g], 1);
						        							}
						        						}
							        				}
						        				}
						        				
						        				if(!found_rs.equals(""))
						        				{
						        					rs_foundbefore_hash.put(anno[0]+"\t"+NormalizedForm, found_rs);
						        				}
						        				
						        				if(found == true)
						        				{
						        					String RSNum=found_rs;
						        					
				    	        					//Expired
			    		        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
			    		        					if ( rs.next() ) 
			    		        					{
			    		        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";GeneNormalized-RS#:"+RSNum+"(Expired)\n";
			    		        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum+"(Expired)");
			    			        				}
			    		        					else
			    		        					{
			    		        						//Merged
			    		        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
			    			        					if ( rs.next() ) 
			    			        					{
			    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";GeneNormalized-RS#:"+rs.getString("merged")+"\n";
			    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],rs.getString("merged"));
			    				        				}
			    			        					else
			    			        					{
			    			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";GeneNormalized-RS#:"+RSNum+"\n";
			    			        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5],RSNum);
			    				        				}
			    		        					}
			    		        					
			    	        						rsg=geneRS_ranked.size();
						        				}
					        				}
				        				}
			        				}
/*
		        					 * 5. Extended
		        					 */
		        					if(found == false)
			        				{
			        					if(MentionPatternMap2rs_extend.containsKey(anno[0]+"\t"+anno[5])) //by pattern [PMID:24073655] c.169G>C (p.Gly57Arg) -- map to rs764352037
				        				{
			        						String RSNum=MentionPatternMap2rs_extend.get(anno[0]+"\t"+anno[5]);
			        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Extended-RS#:"+RSNum+"\n";
				        					found=true;
				        				}
			        				}
		        					/*
		        					 * 6. Variant2MostCorrespondingGene_hash
		        					 */
		        					if(found == false) // can't find relevant RS# in text 
			        				{
		        						if(tmVar.Variant2MostCorrespondingGene_hash.containsKey(anno[3].toLowerCase()))
        								{
		        							String nt[]=tmVar.Variant2MostCorrespondingGene_hash.get(anno[3].toLowerCase()).split("\t"); //4524	1801133	MTHFR C677T
		        							String geneid=nt[0];
		        							String rsid=nt[1];
		        							outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+geneid+";RS#:"+rsid+"\n";
				        					found=true;
        								}
			        				}
		        					
		        					/*
		        					 * Others
		        					 */
		        					if(found == false)
			        				{
			        					outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+"\n";
			        				}
	        					}
	        				}
	        				if(MentionPatternMap2rs_extend.containsKey(anno[0]+"\t"+anno[5]))
	        				{
	        					if(MentionPatternMap.containsKey(anno[0]+"\t"+anno[5]))
	        					{
	        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+MentionPatternMap.get(anno[0]+"\t"+anno[5]), MentionPatternMap2rs_extend.get(anno[0]+"\t"+anno[5]));
	        						String tmp_anno5 = MentionPatternMap.get(anno[0]+"\t"+anno[5]).replaceAll("([^A-Za-z0-9])","\\\\$1");
	        						String RSNum=MentionPatternMap2rs_extend.get(anno[0]+"\t"+anno[5]);
	        						outputSTR=outputSTR.replaceAll(tmp_anno5+"\n", tmp_anno5+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Extended-RS#:"+RSNum+"\n");
	        					}
	        					else
	        					{
	        						MentionPatternMap2rs_extend.put(anno[0]+"\t"+anno[5], MentionPatternMap2rs_extend.get(anno[0]+"\t"+anno[5]));
	        						String tmp_anno5 = anno[5].replaceAll("([^A-Za-z0-9])","\\\\$1");
	        						String RSNum=MentionPatternMap2rs_extend.get(anno[0]+"\t"+anno[5]);
	        						outputSTR=outputSTR.replaceAll(tmp_anno5+"\n", tmp_anno5+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSNum)+";Extended-RS#:"+RSNum+"\n");
	        					}	
	        				}
	        			}
	        			else if(mentiontype.equals("SNP"))
	        			{
	        				String RSNum=anno[5].replace("[Rr][Ss]", "");
	        				RSNum=RSNum.replaceAll("[\\W\\-\\_]", "");
	        				
	        				//Expired
        					ResultSet rs = stmt_expired.executeQuery("SELECT rs FROM Expired WHERE rs='"+RSNum+"' limit 1;");
        					if ( rs.next() ) 
        					{
        						String RSnum=anno[5];
        						RSnum=RSnum.replaceAll("[\\W\\-\\_RrSs]", "");
        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+"(Expired)"+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSnum)+";RS#:"+RSnum+"\n";
	        				}
        					else
        					{
        						//Merged
        						rs = stmt_merged.executeQuery("SELECT merged FROM Merged WHERE origin='"+RSNum+"' limit 1;");
	        					if ( rs.next() ) 
	        					{
	        						String RSnum=rs.getString("merged");
	        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSnum)+";RS#:"+RSnum+"\n";
		        				}
	        					else
	        					{
	        						String RSnum=anno[5];
	        						RSnum=RSnum.replaceAll("[\\W\\-\\_RrSs]", "");
	        						outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSnum)+";RS#:"+RSnum+"\n";
	        					}
        					}
	        			}
	        			else
		        		{
	        				outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+"\n";
	        			}
	        		}
	        		else
	        		{
	        			outputSTR=outputSTR+anno[0]+"\t"+anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+mentiontype+"\t"+anno[5]+"\n";
	        		}
	        	}
				else if (line.equals(""))
				{
					HashMap <String,String> OnlyOneRSnumbers=new HashMap<String,String>();
					/*
    				 *  1)	Consistency: C.1799C>A and V600E should be normalized to the same RS#.
    				 *  2)  Sorting tmVar.VariantFrequency
    				 *  3)	G469A from DNA to Protein mutation
    				 *  4)	BRAFV600E
    				 */
					
					String tiabs_rev = tiabs.replaceAll("[\\-\\_]", " ");
					String BCon="Variant";
					int PrefixTranslation = 1; 
					int Tok_NumCharPartialMatch = 0;
					ArrayList<String> locations = tmVar.PT_GeneVariantMention.SearchMentionLocation(tiabs_rev,tiabs,BCon,PrefixTranslation,Tok_NumCharPartialMatch);
					for (int k = 0 ; k < locations.size() ; k++)
					{
						String anno[]=locations.get(k).split("\t");
						String start=anno[0];
						String last=anno[1];
						String ment=anno[2];
						String type=anno[3];
						String rsid=anno[4];
						outputSTR=outputSTR.replaceAll(pmid+"\t[0-9]+\t"+last+"\t.*\n","");
						outputSTR=outputSTR+pmid+"\t"+start+"\t"+last+"\t"+ment+"\t"+type+"\t"+rsid+"\n";
					}
					
					HashMap<String,HashMap<String,String>> VariantLinking = new HashMap<String,HashMap<String,String>>();
	        		HashMap<String,HashMap<Integer,Integer>> VarID2Mention = new HashMap<String,HashMap<Integer,Integer>>();  //for variant-gene assocation only 
	        		String outputSTR_tmp="";
    				boolean DNAProteinMutation_exists=false;
    				String outputSTR_line[]=outputSTR.split("\n");
	        		for(int l=0;l<outputSTR_line.length;l++)
	        		{
	        			String lstr=outputSTR_line[l];
	        			String lstr_column[]=lstr.split("\t");
	        			if(lstr_column.length>=5)
	        			{
	        				if(lstr_column[1].matches("[0-9]+") && lstr_column[2].matches("[0-9]+")) // in case the text contains "\t"
							{
		        				int start=Integer.parseInt(lstr_column[1]);
		        				int last=Integer.parseInt(lstr_column[2]);
		        				String mention=lstr_column[3];
		        				String type=lstr_column[4];
		        				String IDcolumn=lstr_column[5];
		        				pat = Pattern.compile("^(.+);CorrespondingGene:[0-9null]+;(|SameGroup-|GeneNormalized-|Pattern-|Extended-|Recognized-|Mapping-|SNPinText-|Foundbefore-|RefSeq-)RS#:(.+)$");
		    					mat = pat.matcher(IDcolumn);
		    					if(mat.find())
		    		        	{
		    						IDcolumn = mat.group(1);
		    						String RSnum = "rs"+mat.group(3);
		    						if(!VariantLinking.containsKey(pmid+"\t"+RSnum))
				        			{
				        				VariantLinking.put(pmid+"\t"+RSnum,new HashMap<String,String>());
				        			}
				        			VariantLinking.get(pmid+"\t"+RSnum).put(pmid+"\t"+IDcolumn,"");
				        			if(!VariantLinking.containsKey(pmid+"\t"+IDcolumn))
				        			{
				        				VariantLinking.put(pmid+"\t"+IDcolumn,new HashMap<String,String>());
				        			}
				        			VariantLinking.get(pmid+"\t"+IDcolumn).put(pmid+"\t"+RSnum,"");
		    					}
	    						
		    					//for variant-gene association only: Variant# <p|SUB|V|600|E> --> Mentions (Hash)
		    					if(!VarID2Mention.containsKey(pmid+"\t"+IDcolumn))
		    					{
		    						VarID2Mention.put(pmid+"\t"+IDcolumn, new HashMap<Integer,Integer>());
		    					}
		    					VarID2Mention.get(pmid+"\t"+IDcolumn).put(start,last);//start+"\t"+last+"\t"+mention
		    					
		        				
		        				if(type.matches("(DNAMutation|ProteinMutation|SNP)")) // used to remove Allele only
		        				{
		        					DNAProteinMutation_exists = true;
		        				}
		        				
		        				// Sort the rsids
		        				pat = Pattern.compile("^(.+RS#:)(.+)$");
		    					mat = pat.matcher(lstr);
		    					if(mat.find())
		    		        	{
		    						if(changetype.containsKey(lstr_column[3]))
			        				{
			        					lstr=lstr.replaceAll("\tDNAMutation\tc", "\tProteinMutation\tp");
			        				}
			        				
		    						String pre = mat.group(1);
		    						String rsids = mat.group(2);
		    						String individualconcept_rsids[]=rsids.split(";");
		    						rsids="";
		    						for(int ics=0;ics<individualconcept_rsids.length;ics++)
		    						{
		    							String individualconcept_rsid[]=individualconcept_rsids[ics].split("\\|");
		    							if(individualconcept_rsid.length>1)
		    							{
		    								//sorting
		    	    						for(int ic=0;ic<individualconcept_rsid.length;ic++)
				    						{
		    									for(int jc=1;jc<(individualconcept_rsid.length-ic);jc++)
					    						{
		    										int var1=0;
		    										int var2=0;
		    										if(tmVar.VariantFrequency.containsKey("rs"+individualconcept_rsid[jc-1])){var1=tmVar.VariantFrequency.get("rs"+individualconcept_rsid[jc-1]);}
		    										if(tmVar.VariantFrequency.containsKey("rs"+individualconcept_rsid[jc])){var2=tmVar.VariantFrequency.get("rs"+individualconcept_rsid[jc]);}
		    										if(var1 < var2)
		    										{
		    											String temp = individualconcept_rsid[jc-1];  
		    											individualconcept_rsid[jc-1] = individualconcept_rsid[jc];  
		    											individualconcept_rsid[jc] = temp;
		    										}
					    						}
				    						}
		    								
		    	    						int rsid_len=individualconcept_rsid.length;
		    	    						for(int ic=0;ic<rsid_len;ic++)
				    						{
		    									if(rsids.equals(""))
			    								{
			    									rsids=individualconcept_rsid[ic];
			    								}
			    								else
			    								{
			    									if(ic==0)
			    									{
			    										rsids=rsids+","+individualconcept_rsid[ic];
			    									}
			    									else
			    									{
			    										rsids=rsids+"|"+individualconcept_rsid[ic];
			    									}
			    								}
				    						}
					    				}
		    							else
		    							{
		    								if(rsids.equals(""))
		    								{
		    									rsids=individualconcept_rsids[ics];
		    								}
		    								else
		    								{
		    									rsids=rsids+","+individualconcept_rsids[ics];
		    								}
		    							}
		    							
		    							if(individualconcept_rsids.length==1 && individualconcept_rsid.length==1) // 113488022|121913377 (V600E) && 113488022 (c.1799T>G) --> 113488022 (V600E) && 113488022 (c.1799T>G)
		    							{
		    								OnlyOneRSnumbers.put(individualconcept_rsid[0],"");
		    							}
		    						}
		    						lstr=pre+rsids;
		    		        	}
							}
	        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        			}
	        			else
	        			{
	        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        			}
	        		}
	        		outputSTR=outputSTR_tmp;
	        		
	        		// 113488022|121913377 (V600E) && 113488022 (c.1799T>G) --> 113488022 (V600E) && 113488022 (c.1799T>G)
	        		outputSTR_tmp="";
	        		outputSTR_line=outputSTR.split("\n");
	        		for(int l=0;l<outputSTR_line.length;l++)
	        		{
	        			String lstr=outputSTR_line[l];
	        			String lstr_column[]=lstr.split("\t");
	        			if(lstr_column.length>=5)
	        			{
	        				if(lstr_column[1].matches("[0-9]+") && lstr_column[2].matches("[0-9]+"))
	        				{
		        				String type = lstr_column[4];
		        				int mention_len = lstr_column[3].length();
		        				if(DNAProteinMutation_exists==true || type.matches("(Chromosome|RefSeq|AcidChange|GenomicRegion|CopyNumberVariant)") || mention_len>20) // at least one variant (mention_len>20: natural language mention)
		        				{
		        					pat = Pattern.compile("^(.+RS#:)(.+)$");
			    					mat = pat.matcher(lstr);
			    					if(mat.find())
			    		        	{
			    						String pre = mat.group(1);
			    						String rsids = mat.group(2);
			    						String individualconcept_rsids[]=rsids.split(";");
			    						rsids="";
			    						for(int ics=0;ics<individualconcept_rsids.length;ics++)
			    						{
			    							String individualconcept_rsid[]=individualconcept_rsids[ics].split("\\|");
			    							if(individualconcept_rsid.length>1)
			    							{
			    								for(String OnlyOne : OnlyOneRSnumbers.keySet())
			    								{
			    									if(individualconcept_rsids[ics].matches(".+\\|"+OnlyOne+".*") || individualconcept_rsids[ics].matches(".*"+OnlyOne+"\\|.+"))
			    									{
			    										individualconcept_rsids[ics]=OnlyOne;
			    									}
			    								}
			    							}
			    							if(rsids.equals(""))
		    								{
		    									rsids=individualconcept_rsids[ics];
		    								}
		    								else
		    								{
		    									rsids=rsids+";"+individualconcept_rsids[ics];
		    								}
			    						}
			    						lstr=pre+rsids;
			    		        	}
			    					outputSTR_tmp=outputSTR_tmp+lstr+"\n";
		        				}
	        				}
	        				else
	        				{
	        					outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        				}
	        			}
	        			else
	        			{
	        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        			}
	        		}
	        		outputSTR=outputSTR_tmp;

	        		/**
	        		 * corresponding gene and grouping variants
	        		 */
	        		for(String x:MentionPatternMap.keySet())
	    			{
	        			if(x.matches(pmid+"\t.*"))
	        			{
		        			String y=pmid+"\t"+MentionPatternMap.get(x);
		        			if(!VariantLinking.containsKey(x))
		        			{
		        				VariantLinking.put(x,new HashMap<String,String>());
		        			}
		        			VariantLinking.get(x).put(y,"");
		        			if(!VariantLinking.containsKey(y))
		        			{
		        				VariantLinking.put(y,new HashMap<String,String>());
		        			}
		        			VariantLinking.get(y).put(x,"");
	        			}
	    			}
	        		
	        		for(String x : P2variant.keySet())
	    			{
	        			for(String y : P2variant.get(x).keySet())
	    				{
	        				for(String z : P2variant.get(x).keySet())
		    				{
	    						if(!y.equals(z))
	    						{
	    							if(!VariantLinking.containsKey(y))
	    		        			{
	    								VariantLinking.put(y,new HashMap<String,String>());
	    		        			}
	    							VariantLinking.get(y).put(z,"");
	    		        			
	    		        			if(!VariantLinking.containsKey(z))
	    		        			{
	    		        				VariantLinking.put(z,new HashMap<String,String>());
	    		        			}
	    		        			VariantLinking.get(z).put(y,"");
	    						}
		    				}
	    				}
	    			}

	        		/* - remove
	        		for(String x : WM2variant.keySet())
	    			{
	        			for(String y : WM2variant.get(x).keySet())
	    				{
	        				String y_position=WM2variant.get(x).get(y);
	    					for(String z : WM2variant.get(x).keySet())
		    				{
	    						String z_position=WM2variant.get(x).get(z);
	    						if(!y.equals(z))
	    						{
	    							if(y_position.equals("") || z_position.equals(""))
	    							{
		    							if(!VariantLinking.containsKey(y))
		    		        			{
		    								VariantLinking.put(y,new HashMap<String,String>());
		    		        			}
		    							VariantLinking.get(y).put(z,"");
		    		        			
		    		        			if(!VariantLinking.containsKey(z))
		    		        			{
		    		        				VariantLinking.put(z,new HashMap<String,String>());
		    		        			}
		    		        			VariantLinking.get(z).put(y,"");
	    							}
	    						}
		    				}
	    				}
	    			}
	        		*/
	        		
	        		//VariantLinking: link the protein and DNA mutations
	        		HashMap<String,Boolean> NoPositionVar = new HashMap<String,Boolean>(); // no position var can only group with one variant
	        		for(String tmp1 : VarID2Mention.keySet())
	        		{
	        			String pmid_variantid1[]=tmp1.split("\t");
	        			String pmid1=pmid_variantid1[0];
	        			String variantid1=pmid_variantid1[1];
	        			String component1[]=variantid1.split("\\|",-1);
	        			if(component1.length>=5)
	        			{
	        				String W1=component1[2];
		        			String P1=component1[3];
		        			String M1=component1[4];
		        			if(P1.length()<10 && component1[0].equals("p") && component1[1].equals("SUB")) //1st is ProteinMutation
		        			{
		        				for(String tmp2 : VarID2Mention.keySet())
		    	        		{
		    	        			String pmid_variantid2[]=tmp2.split("\t");
		    	        			String pmid2=pmid_variantid2[0];
		    	        			String variantid2=pmid_variantid2[1];
		    	        			String component2[]=variantid2.split("\\|",-1);
		    	        			if(component2.length>=5)
		    	        			{
		    	        				String W2=component2[2];
			    	        			String P2=component2[3];
			    	        			String M2=component2[4];
			    	        			if(pmid1.equals(pmid2)) // the same pmid
			    	        			{
				    	        			if(P2.length()<10 && component2[0].equals("c") && component2[1].equals("SUB"))//2nd is DNAMutation
				    	        			{
				    	        				if(tmVar.nu2aa_hash.containsKey(W1+"\t"+M1+"\t"+W2+"\t"+M2)) // nu2aa_hash
				    	        				{
				    	        					if(P1.equals("") && (!P2.equals("")) && NoPositionVar.containsKey(tmp1))
		    	        							{
				    	        						if(!VariantLinking.containsKey(tmp1))
			    	        		        			{
				    	        							VariantLinking.put(tmp1,new HashMap<String,String>());
			    	        		        				VariantLinking.get(tmp1).put(tmp2,""); // if the G>G was linked with others, don't link it.
			    	        		        				if(!VariantLinking.containsKey(tmp2))
				    	        		        			{
				    	        		        				VariantLinking.put(tmp2,new HashMap<String,String>());
				    	        		        			}
			    	        		        				VariantLinking.get(tmp2).put(tmp1,""); // if the G>G was linked with others, don't link it.
			    	        		        				NoPositionVar.put(P1,true);
				    	        		        		}
			    	        		        			
		    	        							}
				    	        					else if(P2.equals("") && (!P1.equals("")) && NoPositionVar.containsKey(tmp2))
				    	        					{
				    	        						if(!VariantLinking.containsKey(tmp2))
			    	        		        			{
				    	        							VariantLinking.put(tmp2,new HashMap<String,String>());
			    	        		        				VariantLinking.get(tmp2).put(tmp1,""); // if the G>G was linked with others, don't link it.
			    	        		        				if(!VariantLinking.containsKey(tmp1))
				    	        		        			{
				    	        		        				VariantLinking.put(tmp1,new HashMap<String,String>());
				    	        		        			}
			    	        		        				VariantLinking.get(tmp1).put(tmp2,""); // if the G>G was linked with others, don't link it.
			    	        		        				NoPositionVar.put(P1,true);
					    	        		        	}
				    	        					}
				    	        					else
				    	        					{
				    	        						if(P1.matches("^[0-9]+$") && P2.matches("^[0-9]+$"))
					    	        					{
				    	        							if((Integer.parseInt(P1)-1)*3<Integer.parseInt(P2) && Integer.parseInt(P1)*3>Integer.parseInt(P2))
					    	        						{
					    	        							if(!VariantLinking.containsKey(tmp1))
					    	        		        			{
					    	        		        				VariantLinking.put(tmp1,new HashMap<String,String>());
					    	        		        			}
					    	        		        			VariantLinking.get(tmp1).put(tmp2,"");
					    	        		        			if(!VariantLinking.containsKey(tmp2))
					    	        		        			{
					    	        		        				VariantLinking.put(tmp2,new HashMap<String,String>());
					    	        		        			}
					    	        		        			VariantLinking.get(tmp2).put(tmp1,"");
					    	        						}
					    	        					}
				    	        					}
				    	        				}
				    	        			}
			    	        			}
		    	        			}
		    	        		}
		        			}
	        			}
	        		}
	        		
	        		//implicit relation to direct relation
	        		for(String x:VariantLinking.keySet())
	    			{
	        			if(!x.matches(".*\\|\\|.*"))
	        			{
		        			for(String y:VariantLinking.get(x).keySet())
			    			{
		        				for(String z:VariantLinking.get(x).keySet())
				    			{
		        					if((!y.matches(".*\\|\\|.*")) || (!z.matches(".*\\|\\|.*")))
		    	        			{
			        					if(!y.equals(z))
			    						{
			        						if(!VariantLinking.containsKey(y))
			    		        			{
			    								VariantLinking.put(y,new HashMap<String,String>());
			    		        			}
			    							VariantLinking.get(y).put(z,"");
			    		        			
			    		        			if(!VariantLinking.containsKey(z))
			    		        			{
			    		        				VariantLinking.put(z,new HashMap<String,String>());
			    		        			}
			    		        			VariantLinking.get(z).put(y,"");
			    						}
		    	        			}
				    			}
			    			}
	        			}
	    			}

	        		/*
	        		 *  to group#:
	        		 *  
	        		 *  17003357	c|SUB|C|1858|T -> 0
						17003357	c|Allele|T|1858 -> 0
	        		 */
	        		int group_id=0;
	        		HashMap<String,Integer> VariantGroup = new HashMap<String,Integer>(); 
	        		for(String x:VariantLinking.keySet())
	    			{
	        			boolean found=false;
	        			for(String y:VariantLinking.get(x).keySet())
		    			{
	        				if(VariantGroup.containsKey(y))
	        				{
	        					found=true;
	        					VariantGroup.put(x,VariantGroup.get(y));
	        				}
		    			}
	        			if(found==false)
	        			{
	        				VariantGroup.put(x,group_id);
	        				group_id++;
	        			}
	    			}
	        		for(String VarID : VarID2Mention.keySet()) 
	        		{
	        			if (!VariantGroup.containsKey(VarID))
	        			{
	        				VariantGroup.put(VarID,group_id);
	        				group_id++;
	        			}
	        		}
	        		
	        		//for(String tmpx : VariantGroup.keySet())
	        		//{
	        		//	System.out.println(tmpx+"\t"+VariantGroup.get(tmpx));
	        		//}
	        		
					/*
	        		 *  group# 2 RS#:
	        		 *  
	        		 *  0 -> 2476601
					 */
	        		HashMap<String,Integer> RS2VariantGroup = new HashMap<String,Integer>(); // VariantGroup to RS#
	        		HashMap<Integer,String> VariantGroup2RS = new HashMap<Integer,String>(); // VariantGroup to RS#
	        		for(String s:MentionPatternMap2rs.keySet()) //s : <pmid,variant>
	        		{
	        			if(VariantGroup.containsKey(s))
	        			{
	        				int gid=VariantGroup.get(s);
	        				VariantGroup2RS.put(gid,MentionPatternMap2rs.get(s));
	        				RS2VariantGroup.put("rs"+MentionPatternMap2rs.get(s),gid);
	        			}
	        		}
	        		for(String s:MentionPatternMap2rs_extend.keySet())
	        		{
	        			if(VariantGroup.containsKey(s))
	        			{
	        				int gid=VariantGroup.get(s);
	        				VariantGroup2RS.put(gid,MentionPatternMap2rs_extend.get(s));
	        				RS2VariantGroup.put("rs"+MentionPatternMap2rs_extend.get(s),gid);
	        			}
	        		}
	        		
	        		/*
	        		 *  VariantGroup to GeneID
	        		 */
	        		HashMap<Integer,HashMap<String,String>> sentencelocation_gene2_hash= new HashMap<Integer,HashMap<String,String>>(); // sentence--> gene start --> geneid
					ArrayList<Integer> SentenceOffsets = PMID2SentenceOffsets.get(pmid);
					//Gene in sentence
	        		for(String annotation_gene:annotations_gene.keySet())
        			{
						String annotation_gene_column[]=annotation_gene.split("\t");
        				if(pmid.equals(annotation_gene_column[0]))
        				{
        					int gene_start=Integer.parseInt(annotation_gene_column[1]);
	        				int gene_last=Integer.parseInt(annotation_gene_column[2]);
	        				String gene_id=annotation_gene_column[4];
	        				for(int si=0;si<SentenceOffsets.size();si++) // find sentence location for gene mention
    						{
    							if(gene_start<SentenceOffsets.get(si)) //gene_anno[1] = start
    							{
    								if(!sentencelocation_gene2_hash.containsKey(si-1))
    								{
    									sentencelocation_gene2_hash.put(si-1,new HashMap<String,String>());
    								}
    								sentencelocation_gene2_hash.get(si-1).put(gene_start+"\t"+gene_last,gene_id);
    								break;
    							}
    						}
    					}
        			}
	        		
	        		HashMap<Integer,String> gid2gene_hash= new HashMap<Integer,String>();
					for(int gid=0;gid<group_id;gid++) //group to variant ids
	        		{
						int min_distance=100000000;
	        			String gene_with_min_distance="";
	        			for(String Variant:VariantGroup.keySet())
	        			{
	        				if(VariantGroup.get(Variant)==gid && VarID2Mention.containsKey(Variant)) //Variants in group
	        				{
	        					HashMap<Integer,Integer> Mentions = VarID2Mention.get(Variant);//variant id to mentions
	        					for(int mutation_start : Mentions.keySet()) 
	        					{
	        						int mutation_last=Mentions.get(mutation_start);
	        						int sentencelocation_var=0;
	        						boolean found=false;
	        						for(int si=0;si<SentenceOffsets.size();si++) // find sentence location for variant mention 
	    	    					{
	        							if(mutation_start<SentenceOffsets.get(si))
	    	    						{
	    	    							sentencelocation_var=si-1;
	    	    							found=true;
	    	    							break;
	    	    						}
	    	    					}
	        						if(found==false) // in the last sentence
	        						{
	        							sentencelocation_var=SentenceOffsets.size();
	        						}
	        						HashMap<String,String> genes_in_targetvariant_sentence=new HashMap<String,String>();
	        						if(sentencelocation_gene2_hash.containsKey(sentencelocation_var))
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var);
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_start=Integer.parseInt(gene_start_last_column[0]);
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(mutation_last<gene_start)  // mutation --- gene
	        								{
	        									if(min_distance>gene_start-mutation_last)
	        									{
	        										min_distance=gene_start-mutation_last;
	        										gene_with_min_distance=gene_id;
	        									}
	        								}
	        								else if(gene_last<mutation_start) //gene --- mutation
	        								{
	        									if(min_distance>mutation_start-gene_last)
	        									{
	        										min_distance=mutation_start-gene_last;
	        										gene_with_min_distance=gene_id;
	        									}
	        								}
	        							}
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_var>=1 && sentencelocation_gene2_hash.containsKey(sentencelocation_var-1)) // the gene in previous sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var-1);
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>mutation_start-gene_last+1000)
        									{
        										min_distance=mutation_start-gene_last+1000;
        										gene_with_min_distance=gene_id;
        									}
	        							}
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_var<SentenceOffsets.size()-1 && sentencelocation_gene2_hash.containsKey(sentencelocation_var+1)) // the gene in next sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var+1);
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_start=Integer.parseInt(gene_start_last_column[0]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>gene_start-mutation_last+2000)
        									{
        										min_distance=gene_start-mutation_last+2000;
        										gene_with_min_distance=gene_id;
        									}
	        							}
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_gene2_hash.containsKey(0))// the gene in title sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(0);
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>mutation_start-gene_last+3000)
        									{
        										min_distance=mutation_start-gene_last+3000;
        										gene_with_min_distance=gene_id;
        									}
	        							}
	        							//System.out.println(gid+"\t"+Variant+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else
	        						{
	        							for(int x=sentencelocation_var-1;x>=0;x--) // reach the previous sentence until find genes
	        							{
	        								if(sentencelocation_gene2_hash.containsKey(x))
	        								{
		        								genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(x);
		        								boolean gene_found=false;
		        								for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
			        							{
			        								String gene_start_last_column[]=gene_start_last.split("\t");
			        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
			        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
			        								if(min_distance>mutation_start-gene_last+4000)
		        									{
		        										min_distance=mutation_start-gene_last+4000;
		        										gene_with_min_distance=gene_id;
		        									}
			        								gene_found=true;
			        							}
		        								if(gene_found == true)
		        								{
		        									 break;
		        								}
	        								}
	        							}
	        						}
		        				}
	        				}
	        			}
	        			if(!gene_with_min_distance.equals(""))
	        			{
	        				gid2gene_hash.put(gid,gene_with_min_distance);
	        			}
	        		}
	        		
					/* 
	        		 * Extending the RS# to other group members
	        		 */
					
					outputSTR_tmp="";
	        		outputSTR_line=outputSTR.split("\n");
	        		for(int l=0;l<outputSTR_line.length;l++)
	        		{
	        			String lstr=outputSTR_line[l];
	        			String lstr_column[]=lstr.split("\t");
	        			if(lstr_column.length>=5)
	        			{
	        				if(lstr_column[1].matches("[0-9]+") && lstr_column[2].matches("[0-9]+")) // in case the text contains "\t"
							{
	        					
		        				int mutation_start=Integer.parseInt(lstr_column[1]);
		        				int mutation_last=Integer.parseInt(lstr_column[2]);
		        				String mention=lstr_column[3];
		        				String type=lstr_column[4];
		        				String IDcolumn=lstr_column[5];
		        				pat = Pattern.compile("^(.+);CorrespondingGene:([0-9null]+);(|SameGroup-|GeneNormalized-|Pattern-|Extended-|Recognized-|Mapping-|SNPinText-|Foundbefore-|RefSeq-)RS#:(.+)$");
		    					mat = pat.matcher(IDcolumn);
		    					String RSID="";
		    					String geneid="";
		    					String CorrespondingGene="";
		    					String RSID_extract_type="";
		    					if(mat.find())
		    		        	{
		    						IDcolumn = mat.group(1);
		    						geneid = mat.group(2);
		    						RSID_extract_type = mat.group(3);
		    						RSID = mat.group(4);
		    		        	}
		    					
		    					String RSID_for_gene=RSID.replaceAll("\\(.*\\)$","");
		    					
		    					if(VariantGroup.containsKey(pmid+"\t"+IDcolumn))
	    						{
		    						int GroupID=VariantGroup.get(pmid+"\t"+IDcolumn);
		        					if(VariantGroup2RS.containsKey(GroupID))
		    						{
		        						if(VariantGroup2RS.get(GroupID).equals(RSID))
		        						{
		        							if(RSID_extract_type.equals("")){RSID_extract_type="SameGroup-";}
		        						}
		        						else if(RSID.equals(""))
		        						{
			        						RSID=VariantGroup2RS.get(GroupID);
			        						if(RSID_extract_type.equals("")){RSID_extract_type="SameGroup-";}
		        						}
		        						else
		        						{
		        							VariantGroup.put(pmid+"\t"+IDcolumn,group_id);
		        							VariantGroup2RS.put(group_id,RSID);
		        							group_id++;
		        						}
		    						}
		        					else if(gid2gene_hash.containsKey(GroupID))
		        					{
		        						CorrespondingGene=gid2gene_hash.get(GroupID);
		        					}
		        				}
		    					else
		    					{
		    						int sentencelocation_var=0;
	        						boolean found=false;
	        						for(int si=0;si<SentenceOffsets.size();si++) // find sentence location for variant mention 
	    	    					{
	        							if(mutation_start<SentenceOffsets.get(si))
	    	    						{
	    	    							sentencelocation_var=si-1;
	    	    							found=true;
	    	    							break;
	    	    						}
	    	    					}
	        						if(found==false) // in the last sentence
	        						{
	        							sentencelocation_var=SentenceOffsets.size();
	        						}
	        						int min_distance=100000000;
	        	        			String gene_with_min_distance="";
	        	        			HashMap<String,String> genes_in_targetvariant_sentence=new HashMap<String,String>();
	        						if(sentencelocation_gene2_hash.containsKey(sentencelocation_var))
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var);
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_start=Integer.parseInt(gene_start_last_column[0]);
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(mutation_last<gene_start)  // mutation --- gene
	        								{
	        									if(min_distance>gene_start-mutation_last)
	        									{
	        										min_distance=gene_start-mutation_last;
	        										gene_with_min_distance=gene_id;
	        									}
	        								}
	        								else if(gene_last<mutation_start) //gene --- mutation
	        								{
	        									if(min_distance>mutation_start-gene_last)
	        									{
	        										min_distance=mutation_start-gene_last;
	        										gene_with_min_distance=gene_id;
	        									}
	        								}
	        							}
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_var>=1 && sentencelocation_gene2_hash.containsKey(sentencelocation_var-1)) // the gene in previous sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var-1);
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>mutation_start-gene_last+1000)
	    									{
	    										min_distance=mutation_start-gene_last+1000;
	    										gene_with_min_distance=gene_id;
	    									}
	        							}
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_var<SentenceOffsets.size()-1 && sentencelocation_gene2_hash.containsKey(sentencelocation_var+1)) // the gene in next sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(sentencelocation_var+1);
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_start=Integer.parseInt(gene_start_last_column[0]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>gene_start-mutation_last+2000)
	    									{
	    										min_distance=gene_start-mutation_last+2000;
	    										gene_with_min_distance=gene_id;
	    									}
	        							}
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t-1\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						else if(sentencelocation_gene2_hash.containsKey(0))// the gene in title sentence
	        						{
	        							genes_in_targetvariant_sentence=sentencelocation_gene2_hash.get(0);
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance);
	        							for(String gene_start_last : genes_in_targetvariant_sentence.keySet())
	        							{
	        								String gene_start_last_column[]=gene_start_last.split("\t");
	        								int gene_last=Integer.parseInt(gene_start_last_column[1]);
	        								String gene_id=genes_in_targetvariant_sentence.get(gene_start_last);
	        								if(min_distance>mutation_start-gene_last+10000)
	    									{
	    										min_distance=mutation_start-gene_last+10000;
	    										gene_with_min_distance=gene_id;
	    									}
	        							}
	        							//System.out.println(mention+"\t"+IDcolumn+"\t"+mutation_start+"\t"+sentencelocation_var+"\t"+genes_in_targetvariant_sentence+"\t"+min_distance+"\t"+gene_with_min_distance+"\n");
	        						}
	        						CorrespondingGene=gene_with_min_distance;
		    					}
		    					if(CorrespondingGene.equals(""))
		    					{
		    						int count=0;
		    						int max=0;
		    						String gene_max="";
		    						for(String SingleG : SingleGene.get(pmid).keySet())
		    						{
		    							count++;
		    							if(SingleGene.get(pmid).get(SingleG)>max)
		    							{
		    								max=SingleGene.get(pmid).get(SingleG);
		    								gene_max=SingleG;
		    							}
		    						}
		    						if(count==1 && max>=3)
		    						{
		    							CorrespondingGene=gene_max;
		    						}
		    					}
		    					
		    					String VariantGroupNum="";
		    					HashMap<String,String> VariantGroupNum2geneid_hash=new HashMap<String,String>();
		    					if(VariantGroup.containsKey(pmid+"\t"+IDcolumn))
		    					{
		    						VariantGroupNum=Integer.toString(VariantGroup.get(pmid+"\t"+IDcolumn));
		    					}
		    					else if(RS2VariantGroup.containsKey(IDcolumn))
		    					{
		    						VariantGroupNum=Integer.toString(RS2VariantGroup.get(IDcolumn));
		    					}
		    					else
		    					{
		    						if(type.equals("Chromosome") || type.equals("RefSeq")) {}
		    						else
		    						{
			    						VariantGroup.put(pmid+"\t"+IDcolumn,group_id);
			    						VariantGroupNum=Integer.toString(group_id);
			    						group_id++;
		    						}
		    					}
		    					
		    					Pattern RegEx_RSnum = Pattern.compile("rs([0-9]+)");
		    					Matcher mp_RSnum = RegEx_RSnum.matcher(IDcolumn);
		    					
		    					if (mp_RSnum.find()) 
								{
									RSID = mp_RSnum.group(1);
								}
								if (HideMultipleResult.equals("True")) //chose the most popular RSID
								{
									RSID=PrioritizeRS_by_RS2Frequency(RSID);
								}
								
								/*HGVS format*/
								String ID_HGVS="";
								String component[]=IDcolumn.split("\\|",-1);
								String RSID_tmp=RSID.replaceAll("\\((Expired|Merged)\\)","");
								if(component.length>3 && tmVar.RSandPosition2Seq_hash.containsKey(RSID_tmp+"\t"+component[3]))
								{
									IDcolumn=IDcolumn.replaceAll("^[a-z]\\|",tmVar.RSandPosition2Seq_hash.get(RSID_tmp+"\t"+component[3])+"|");
									component=IDcolumn.split("\\|",-1);
									if(tmVar.RSandPosition2Seq_hash.get(RSID_tmp+"\t"+component[3]).equals("p"))
									{
										type="ProteinMutation";
									}
									else
									{
										type="DNAMutation";
									}
								}
								
								if(component[0].equals("p"))
								{
									if(component[1].equals("SUB") && component.length>=5)
									{
										ID_HGVS=component[0]+"."+component[2]+component[3]+component[4];
									}
									else if(component[1].equals("INS") && component.length>=4) //"c.104insT"	--> "c|INS|104|T"
									{
										ID_HGVS=component[0]+"."+component[2]+"ins"+component[3];
									}
									else if(component[1].equals("DEL") && component.length>=4) //"c.104delT"	--> "c|DEL|104|T"
									{
										ID_HGVS=component[0]+"."+component[2]+"del"+component[3];
									}
									else if(component[1].equals("INDEL") && component.length>=4) //"c.2153_2155delinsTCCTGGTTTA"	-->	"c|INDEL|2153_2155|TCCTGGTTTA"
									{
										ID_HGVS=component[0]+"."+component[2]+"delins"+component[3];
									}
									else if(component[1].equals("DUP") && component.length>=4) //"c.1285-1301dup"	--> "c|DUP|1285_1301||"
									{
										ID_HGVS=component[0]+"."+component[2]+"dup"+component[3];
									}
									else if(component[1].equals("FS") && component.length>=6) //"p.Val35AlafsX25"	-->	"p|FS|V|35|A|25"
									{
										ID_HGVS=component[0]+"."+component[2]+component[3]+component[4]+"fsX"+component[5];
									}
								}
								else if(component[0].equals("c") || component[0].equals("g"))
								{
									if(component[1].equals("SUB") && component.length>=5)
									{
										ID_HGVS=component[0]+"."+component[3]+component[2]+">"+component[4];
									}
									else if(component[1].equals("INS") && component.length>=4)
									{
										ID_HGVS=component[0]+"."+component[2]+"ins"+component[3];
									}
									else if(component[1].equals("DEL") && component.length>=4)
									{
										ID_HGVS=component[0]+"."+component[2]+"del"+component[3];
									}
									else if(component[1].equals("INDEL") && component.length>=4)
									{
										ID_HGVS=component[0]+"."+component[2]+"delins"+component[3];
									}
									else if(component[1].equals("DUP") && component.length>=4)
									{
										ID_HGVS=component[0]+"."+component[2]+"dup"+component[3];
									}
									else if(component[1].equals("FS") && component.length>=6)
									{
										ID_HGVS=component[0]+"."+component[2]+component[3]+component[4]+"fsX"+component[5];
									}
								}
								else if(component[0].equals(""))
								{
									if(component[1].equals("SUB") && component.length>=5)
									{
										ID_HGVS="c."+component[3]+component[2]+">"+component[4];
									}
									else if(component[1].equals("INS") && component.length>=4)
									{
										ID_HGVS="c."+component[2]+"ins"+component[3];
									}
									else if(component[1].equals("DEL") && component.length>=4)
									{
										ID_HGVS="c."+component[2]+"del"+component[3];
									}
									else if(component[1].equals("INDEL") && component.length>=4)
									{
										ID_HGVS="c."+component[2]+"delins"+component[3];
									}
									else if(component[1].equals("DUP") && component.length>=4)
									{
										ID_HGVS="c."+component[2]+"dup"+component[3];
									}
									else if(component[1].equals("FS") && component.length>=6)
									{
										ID_HGVS="p."+component[2]+component[3]+component[4]+"fsX"+component[5];
									}
								}
								if(ID_HGVS.equals("")){}
								else if (type.equals("DNAAcidChange")){ID_HGVS="";}
								else if (type.equals("ProteinAcidChange")){ID_HGVS="";}
								else
								{
									ID_HGVS=";HGVS:"+ID_HGVS;
								}
								
								if(type.equals("DNAMutation"))
								{
									if(IDcolumn.matches("\\|Allele\\|"))
									{
										type="DNAAllele";
									}
								}
								else if(type.equals("ProteinMutation"))
			        			{
									if(IDcolumn.matches("\\|Allele\\|"))
									{
										type="ProteinAllele";
									}
			        			}
								
								if(!RSID.equals("")) // add RSID
		        				{
									if(rs2gene.containsKey(pmid+"\t"+RSID_for_gene))
		        					{
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSID_for_gene)+";"+RSID_extract_type+"RS#:"+RSID+"\n";
		        						VariantGroupNum2geneid_hash.put(VariantGroupNum,rs2gene.get(pmid+"\t"+RSID_for_gene));
			        				}
		        					else if(rs2gene.containsKey(pmid+"\t"+RSID))
		        					{
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSID)+";"+RSID_extract_type+"RS#:"+RSID+"\n";
		        						VariantGroupNum2geneid_hash.put(VariantGroupNum,rs2gene.get(pmid+"\t"+RSID));
		        					}
		        					else
		        					{
		        						ResultSet gene = stmt_rs2gene.executeQuery("SELECT gene FROM rs2gene WHERE rs='"+RSID+"'order by gene asc limit 1");
				    					while ( gene.next() ) 
				    					{
				    						String  gene_id = gene.getString("gene");
				    						rs2gene.put(pmid+"\t"+RSID,gene_id);
				    					}
		        						
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+";CorrespondingGene:"+rs2gene.get(pmid+"\t"+RSID)+";"+RSID_extract_type+"RS#:"+RSID+"\n";
		        						VariantGroupNum2geneid_hash.put(VariantGroupNum,geneid);
				        			}
		        				}
		        				else if(!CorrespondingGene.equals("") && !CorrespondingGene.equals("null") && !type.matches("(Chromosome|RefSeq|GenomicRegion|CopyNumberVariant)")) // add Gene
		        				{
		        					outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+";CorrespondingGene:"+CorrespondingGene+"\n";
		        					VariantGroupNum2geneid_hash.put(VariantGroupNum,CorrespondingGene);
		        				}
		        				else
		        				{	
		        					if(type.equals("Chromosome"))
	    							{
		        						if(DisplayChromosome.equals("True"))
		        						{
		        							outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\t"+IDcolumn+"\n";
		        						}
				        			}
		        					else if(type.equals("RefSeq"))
		        					{
		        						if(DisplayRefSeq.equals("True"))
		        						{
		        							outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\t"+IDcolumn+"\n";
		        						}
		        					}
		        					else if(type.equals("GenomicRegion"))
		        					{
		        						if(DisplayGenomicRegion.equals("True"))
		        						{
		        							outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\t"+IDcolumn+"\n";
		        						}
		        					}
		        					else if(type.equals("CopyNumberVariant"))
		        					{
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\t"+IDcolumn+"\n";
		        					}
		        					else if(CorrespondingGene.equals("null") || CorrespondingGene.equals(""))
		        					{
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+"\n";	
		        					}
		        					else
		        					{
		        						outputSTR_tmp=outputSTR_tmp+pmid+"\t"+mutation_start+"\t"+mutation_last+"\t"+mention+"\t"+type+"\ttmVar:"+IDcolumn+ID_HGVS+";VariantGroup:"+VariantGroupNum+";CorrespondingGene:"+CorrespondingGene+"\n";
		        					}
		        				}
							}
	        				else
	        				{
	        					outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        				}
	        			}
	        			else
	        			{
	        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        			}
	        		}
	        		outputSTR=outputSTR_tmp;
	        		
	        		/**
	        		 *  refine corresponding gene & human gene & species & cA#
	        		 */
	        		outputSTR_tmp="";
	        		outputSTR_line=outputSTR.split("\n");
	        		for(int l=0;l<outputSTR_line.length;l++)
	        		{
	        			String lstr=outputSTR_line[l];
	        			String lstr_column[]=lstr.split("\t");
	        			if(lstr_column.length>=5)
	        			{
	        				if(lstr_column[1].matches("[0-9]+") && lstr_column[2].matches("[0-9]+")) // in case the text contains "\t"
							{
		        				String IDcolumn=lstr_column[5];
		        				
		        				String RSID="";
		    					String gene_id="";
		    					String tax_id="";
		    					String tmVarForm="";
		    					
		        				pat = Pattern.compile("^(.*)tmVar:(.+?);.*CorrespondingGene:([0-9null]+);(|SameGroup-|GeneNormalized-|Pattern-|Extended-|Recognized-|Mapping-|SNPinText-|Foundbefore-|RefSeq-)RS#:([0-9]+).*$");
		    					mat = pat.matcher(IDcolumn);
		    					Pattern pat2 = Pattern.compile("^(.*)CorrespondingGene:([0-9null]+)(.*)$");
		    					Matcher mat2 = pat2.matcher(IDcolumn);
		    					if(mat.find())
		    		        	{
		    						tmVarForm = mat.group(2);
		    						gene_id = mat.group(3);
		    						RSID = mat.group(5);
		    		        	
		    						if(tmVar.Gene2HumanGene_hash.containsKey(gene_id)) //translate to human gene by homologene
		        					{
		    							lstr=lstr.replaceAll("CorrespondingGene:"+gene_id,"OriginalGene:"+gene_id+";CorrespondingGene:"+tmVar.Gene2HumanGene_hash.get(gene_id));
		        					}
		    						else // translate to human gene by NCBI ortholog
		    						{
		    							ResultSet humangene = stmt_gene2humangene.executeQuery("SELECT humangene FROM gene2humangene WHERE gene='"+gene_id+"'");
				    					if ( humangene.next() ) 
				    					{
				    						String humangene_id = humangene.getString("humangene");
				    						lstr=lstr.replaceAll("CorrespondingGene:"+gene_id,"OriginalGene:"+gene_id+";CorrespondingGene:"+humangene_id);
				        				}
		    						}
			    				}
		    					else if(mat2.find())
		    		        	{
		    						gene_id = mat2.group(2);
		    						
		    						if(tmVar.Gene2HumanGene_hash.containsKey(gene_id)) //translate to human gene
		        					{
		    							lstr=lstr.replaceAll("CorrespondingGene:"+gene_id,"OriginalGene:"+gene_id+";CorrespondingGene:"+tmVar.Gene2HumanGene_hash.get(gene_id));
		        					}
		    						else
		    						{
		    							ResultSet humangene = stmt_gene2humangene.executeQuery("SELECT humangene FROM gene2humangene WHERE gene='"+gene_id+"'");
				    					if ( humangene.next() ) 
				    					{
				    						String humangene_id = humangene.getString("humangene");
				    						lstr=lstr.replaceAll("CorrespondingGene:"+gene_id,"OriginalGene:"+gene_id+";CorrespondingGene:"+humangene_id);
				        				}
		    						}
		    					}
		    					
		    					ResultSet tax = stmt_gene2tax.executeQuery("SELECT tax FROM gene2tax WHERE gene='"+gene_id+"'");
		    					if ( tax.next() ) 
		    					{
		    						tax_id = tax.getString("tax");
		    					}
		    					
		    					HashMap<String,String> ATCG_MAP=new HashMap<String,String>();
		    					ATCG_MAP.put("A", "T");
		    					ATCG_MAP.put("C", "G");
		    					ATCG_MAP.put("T", "A");
		    					ATCG_MAP.put("G", "C");
		    					String CAID="";
		    					String num_rs="";
		    					c = DriverManager.getConnection("jdbc:sqlite:Database/RS2CA_tmVarForm.db");
		    					stmt = c.createStatement();
		    					ResultSet count_rs = stmt.executeQuery("SELECT count(CA) as num_rs FROM RS2CA_tmVarForm WHERE RS='"+RSID+"'");
		    					count_rs.next();
		    					num_rs = count_rs.getString("num_rs");
		    					ResultSet rs = stmt.executeQuery("SELECT CA,DNAVariant,ProteinVariant FROM RS2CA_tmVarForm WHERE RS='"+RSID+"'");
		    					while ( rs.next() ) 
		    					{
		    						String  CA = rs.getString("CA");
		    						String  DNAVariant = rs.getString("DNAVariant");
		    						String  ProteinVariant = rs.getString("ProteinVariant");
		    						if(DNAVariant.equals(tmVarForm)) // DNA Variant exact match 
		    						{
		    							CAID=CA; break;
		    						}
		    						else if(ProteinVariant.equals(tmVarForm)) // Protein Variant exact match
		    						{
		    							CAID=CA; break;
		    						}
		    						else
		    						{
		    							String IDcolumn_column[]=tmVarForm.split("\\|",-1);
		    							String DV_column[]=DNAVariant.split("\\|",-1);
		    							String PV_column[]=ProteinVariant.split("\\|",-1);
		    							if(IDcolumn_column.length>4 && DV_column.length>4 && IDcolumn_column[2].equals(DV_column[2]) && IDcolumn_column[4].equals(DV_column[4])) // DNA Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    							else if(IDcolumn_column.length>4 && DV_column.length>4 && IDcolumn_column[2].equals(ATCG_MAP.get(DV_column[2])) && IDcolumn_column[4].equals(ATCG_MAP.get(DV_column[4]))) // DNA Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        			else if(IDcolumn_column.length>4 && DV_column.length>4 && IDcolumn_column[2].equals(DV_column[4]) && IDcolumn_column[4].equals(DV_column[2])) // DNA Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        			else if(IDcolumn_column.length>4 && DV_column.length>4 && IDcolumn_column[2].equals(ATCG_MAP.get(DV_column[4])) && IDcolumn_column[4].equals(ATCG_MAP.get(DV_column[2]))) // DNA Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        			else if(IDcolumn_column.length>4 && PV_column.length>4 && IDcolumn_column[2].equals(PV_column[2]) && IDcolumn_column[4].equals(PV_column[4])) // Protein Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        			else if(IDcolumn_column.length>4 && PV_column.length>4 && IDcolumn_column[2].equals(PV_column[4]) && IDcolumn_column[4].equals(PV_column[2])) // Protein Variant W+M match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        			else if(IDcolumn_column.length>3 && PV_column.length>3 && IDcolumn_column[1].matches("(DEL|INS|INDEL)") && IDcolumn_column[3].equals(PV_column[3])) // DEL/INS Variant W match
		    		        			{
		    		        				CAID=CA; break;
		    		        			}
		    		        		}
		    						
		    					}
		    					
		    					if (!tax_id.equals(""))
		    					{
		    						lstr=lstr+";CorrespondingSpecies:"+tax_id;
		        				}
		    					
		    					if (CAID.equals(""))
		    					{
		    						outputSTR_tmp=outputSTR_tmp+lstr+"\n";
		    					}
		        				else
		        				{
		        					outputSTR_tmp=outputSTR_tmp+lstr+";CA#:"+CAID+"\n";
		        				}
							}
	        				else
		        			{
		        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
		        			}
	        			}
	        			else
	        			{
	        				outputSTR_tmp=outputSTR_tmp+lstr+"\n";
	        			}
	        		}
	        		outputSTR=outputSTR_tmp;
	        		
	        		// hide the types of the normalization methods 
	        		if(DisplayRSnumOnly.equals("True"))
					{
	        			outputSTR=outputSTR.replaceAll(";(|SameGroup-|GeneNormalized-|Pattern-|Extended-|Recognized-|Mapping-|SNPinText-|Foundbefore-|RefSeq-)RS#:", ";RS#:");
					}
	        		
	        		outputSTR=outputSTR.replaceAll(";RS#:[0-9\\|]+(.*RS#:[0-9\\|]+)","$1");
	        		outputSTR=outputSTR.replaceAll("(;CorrespondingGene:[0-9]+.*);CorrespondingGene:[0-9]+","$1");
	        		outputSTR=outputSTR.replaceAll("CorrespondingGene:null","");
	        		outputSTR=outputSTR.replaceAll(";;RS#:",";RS#:");
	        		
	        		outputfile.write(outputSTR+"\n");
					outputSTR="";
					tiabs="";
					
					VariantLinking=new HashMap<String,HashMap<String,String>>();
					VariantGroup=new HashMap<String,Integer>();
					MentionPatternMap=new HashMap<String,String>();
					P2variant=new HashMap<String,HashMap<String,String>>();
					WM2variant=new HashMap<String,HashMap<String,String>>();
				}
				else
				{
					outputSTR=outputSTR+line+"\n";
				}
			}
			inputfile.close();
			outputfile.close();
			
		}
		catch(IOException e1){ System.out.println("[normalization]: "+e1+" Input file is not exist.");}
	}
	/* RS#:121913377|113488022 -> RS#:113488022 */ 
	public String PrioritizeRS_by_RS2Frequency(String RSID)throws IOException 
	{
		String RSIDs[]=RSID.split("\\|");
		int max_freq=0;
		String RSID_with_max_freq="";
		for(int i=0;i<RSIDs.length;i++)
		{
			if(i==0)
			{
				RSID_with_max_freq=RSIDs[i];
				if(	tmVar.RS2Frequency_hash.containsKey(RSIDs[i]))
				{
					max_freq=tmVar.RS2Frequency_hash.get(RSIDs[i]);
				}
			}
			else if(tmVar.RS2Frequency_hash.containsKey(RSIDs[i]) && tmVar.RS2Frequency_hash.get(RSIDs[i])>max_freq)
			{
				max_freq=tmVar.RS2Frequency_hash.get(RSIDs[i]);
				RSID_with_max_freq=RSIDs[i];
			}
		}
		
		return RSID_with_max_freq;
	}
}
