//
// tmVar - Java version
// Feature Extraction
//
package tmVarlib;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class MentionRecognition 
{
	public void FeatureExtraction(String Filename,String FilenameData,String FilenameLoca,String TrainTest)
	{
		/*
		 * Feature Extraction
		 */
		try 
		{
			//input
			BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(Filename), "UTF-8"));
			//output
			BufferedWriter FileLocation = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenameLoca), "UTF-8")); // .location
			BufferedWriter FileData = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FilenameData), "UTF-8")); // .data
			//parameters
			String Pmid="";
			ArrayList<String> ParagraphType=new ArrayList<String>(); // Type: Title|Abstract
			ArrayList<String> ParagraphContent = new ArrayList<String>(); // Text
			ArrayList<String> annotations = new ArrayList<String>(); // Annotation
			HashMap<Integer, String> RegEx_HGVs_hash = new HashMap<Integer, String>(); // RegEx_HGVs_hash
			HashMap<Integer, String> character_hash = new HashMap<Integer, String>();
    		String line;
			while ((line = inputfile.readLine()) != null)  
			{
				
				Pattern pat = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
				Matcher mat = pat.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					Pmid = mat.group(1);
					ParagraphType.add(mat.group(2));
					ParagraphContent.add(mat.group(3));
				}
				else if (line.contains("\t")) //Annotation
	        	{
					String anno[]=line.split("\t");
	        		if(anno.length>=6)
	        		{
	        			String mentiontype=anno[4];
	        			if(mentiontype.equals("Gene"))
	        			{
	        				tmVar.GeneMention=true;
	        			}
	        			
	        			if(TrainTest.equals("Train"))
	        			{
	        				int start= Integer.parseInt(anno[1]);
		        			int last= Integer.parseInt(anno[2]);
		        			String mention=anno[3];
		        			String component=anno[5];
		        			
		        			Matcher m1 = tmVar.Pattern_Component_1.matcher(component);
		        			Matcher m2 = tmVar.Pattern_Component_2.matcher(component);
		        			Matcher m3 = tmVar.Pattern_Component_3.matcher(component);
		        			Matcher m4 = tmVar.Pattern_Component_4.matcher(component);
		        			Matcher m5 = tmVar.Pattern_Component_5.matcher(component);
		        			Matcher m6 = tmVar.Pattern_Component_6.matcher(component);
		        			
		        			for(int s=start;s<last;s++)
		        			{
		        				character_hash.put(s,"I");
		        			}
		        			
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
		        					type[i]=type[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+type[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"A");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\ttype\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<W.length;i++)
		        				{
		        					W[i]=W[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+W[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"W");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\tW\t"+Pmid+"\t"+mention);
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
		        							character_hash.put(j+start,"P");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\tP\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<M.length;i++)
		        				{
		        					M[i]=M[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+M[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"M");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\tM\t"+Pmid+"\t"+mention);
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
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"F");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\tF\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<S.length;i++)
		        				{
		        					S[i]=S[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+S[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"S");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m1\tS\t"+Pmid+"\t"+mention);
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
		        					type[i]=type[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+type[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"A");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m2\tType\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<W.length;i++)
		        				{
		        					W[i]=W[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+W[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"W");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m2\tW\t"+Pmid+"\t"+mention);
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
		        							character_hash.put(j+start,"P");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m2\tP\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<M.length;i++)
		        				{
		        					M[i]=M[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+M[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"M");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m2\tM\t"+Pmid+"\t"+mention);
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
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"F");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m2\tF\t"+Pmid+"\t"+mention);
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
		        					type[i]=type[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+type[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"A");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m3\tType\t"+Pmid+"\t"+mention);
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
		        							character_hash.put(j+start,"P");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m3\tP\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<T.length;i++)
		        				{
		        					T[i]=T[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+T[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"T");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m3\tT\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<M.length;i++)
		        				{
		        					M[i]=M[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+M[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"M");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m3\tM\t"+Pmid+"\t"+mention);
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
		        					type[i]=type[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+type[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"A");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m4\tType\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<W.length;i++)
		        				{
		        					W[i]=W[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+W[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"W");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m4\tW\t"+Pmid+"\t"+mention);
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
		        							character_hash.put(j+start,"P");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m4\tP\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<M.length;i++)
		        				{
		        					M[i]=M[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+M[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"M");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m4\tM\t"+Pmid+"\t"+mention);
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
		        					type[i]=type[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+type[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"A");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m5\tType\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<T.length;i++)
		        				{
		        					T[i]=T[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+T[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"T");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m5\tT\t"+Pmid+"\t"+mention);
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
		        							character_hash.put(j+start,"P");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m5\tP\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        				for(int i=0;i<M.length;i++)
		        				{
		        					M[i]=M[i].replaceAll("([^A-Za-z0-9@])", "\\\\$1");
		        					String patt="^(.*?)("+M[i]+")(.*?)$";
		        					Pattern ptmp = Pattern.compile(patt);
		        					Matcher mtmp = ptmp.matcher(mention_tmp);
		        					if(mtmp.find())
		        					{
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"M");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m5\tM\t"+Pmid+"\t"+mention);
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
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"D");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m5\tD\t"+Pmid+"\t"+mention);
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
		        						for(int j=mtmp.group(1).length();j<(mtmp.group(1).length()+mtmp.group(2).length());j++)
		        						{
		        							character_hash.put(j+start,"R");
		        						}
		        						String mtmp2_tmp="";
		        						for(int j=0;j<mtmp.group(2).length();j++){mtmp2_tmp=mtmp2_tmp+"@";}
		        						mention_tmp=mtmp.group(1)+mtmp2_tmp+mtmp.group(3);
		        					}
		        					else
		        					{
		        						System.out.println("Error! Cannot find component: m6\tType\t"+Pmid+"\t"+mention);
		        					}
		        				}
		        			}
		        			else
			        		{
			        			System.out.println("Error! Annotation component cannot match RegEx. " + mention);
			        		}
		        			annotations.add(anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[4]+"\t"+anno[5]);
	        			}
	        		}
	        		//else
	        		//{
	        		//	System.out.println("Error! annotation column is less than 6: "+line);
	        		//}
	        	}
				else if(line.length()==0) //Processing
				{
					String Document="";
					for(int i=0;i<ParagraphContent.size();i++)
		        	{
						Document=Document+ParagraphContent.get(i)+" ";
		        	}
					String Document_rev=Document;
					
					/*
					 * RegEx result of ProteinMutation - Post is group(5)
					 */
					for(int i=0;i<tmVar.RegEx_ProteinMutation_STR.size();i++)
					{
						Pattern PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_ProteinMutation_STR.get(i));
						Matcher m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						while (m.find()) 
						{
							String pre = m.group(1);
							String mention = m.group(2);
							String post = m.group(5);
							
							{
								Pattern ptmp = Pattern.compile("^(.+)([ ;.,:]+)$");
								Matcher mtmp = ptmp.matcher(mention);
								if(mtmp.find())
								{
									mention=mtmp.group(1);
									post=mtmp.group(2)+post;
								}
								
								if((!mention.contains("(")) && mention.substring(mention.length()-1,mention.length()).equals(")")){	mention=mention.substring(0,mention.length()-1);post=")"+post;}
								if((!mention.contains("[")) && mention.substring(mention.length()-1,mention.length()).equals("]")){	mention=mention.substring(0,mention.length()-1);post="]"+post;}
								if((!mention.contains("{")) && mention.substring(mention.length()-1,mention.length()).equals("}")){	mention=mention.substring(0,mention.length()-1);post="}"+post;}
								
								int start=pre.length();
								int last=pre.length()+mention.length();
								for(int j=start;j<last;j++)
								{
									RegEx_HGVs_hash.put(j,"ProteinMutation");
								}
								String mention_rev="";
								for(int j=0;j<mention.length();j++){mention_rev=mention_rev+"@";}
								Document_rev=pre+mention_rev+post;
							}
							PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_ProteinMutation_STR.get(i));
							m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						}
					}
					/*
					 * RegEx result of DNAMutation - Post is group(4)
					 */
					for(int i=0;i<tmVar.RegEx_DNAMutation_STR.size();i++)
					{
						Pattern PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_DNAMutation_STR.get(i));
						Matcher m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						while (m.find()) 
						{
							String pre = m.group(1);
							String mention = m.group(2);
							String post = m.group(4);
							
							//Pattern pCheck = Pattern.compile("^[cgr][^0-9a-zA-Z]*.[^0-9a-zA-Z]*[0-9]+[^0-9a-zA-Z]*$");
							//Matcher mCheck = pCheck.matcher(mention);
							//if(!mCheck.find())
							{
								Pattern ptmp = Pattern.compile("^(.+)([ ;.,:]+)$");
								Matcher mtmp = ptmp.matcher(mention);
								if(mtmp.find())
								{
									mention=mtmp.group(1);
									post=mtmp.group(2)+post;
								}
								
								if((!mention.contains("(")) && mention.substring(mention.length()-1,mention.length()).equals(")")){	mention=mention.substring(0,mention.length()-1);post=")"+post;}
								if((!mention.contains("[")) && mention.substring(mention.length()-1,mention.length()).equals("]")){	mention=mention.substring(0,mention.length()-1);post="]"+post;}
								if((!mention.contains("{")) && mention.substring(mention.length()-1,mention.length()).equals("}")){	mention=mention.substring(0,mention.length()-1);post="}"+post;}
								
								int start=pre.length();
								int last=pre.length()+mention.length();
								for(int j=start;j<last;j++)
								{
									RegEx_HGVs_hash.put(j,"DNAMutation");
								}
								String mention_rev="";
								for(int j=0;j<mention.length();j++){mention_rev=mention_rev+"@";}
								Document_rev=pre+mention_rev+post;
							}
							PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_DNAMutation_STR.get(i));
							m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						}
					}
					/*
					 * RegEx result of SNP
					 */
					for(int i=0;i<tmVar.RegEx_SNP_STR.size();i++)
					{
						Pattern PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_SNP_STR.get(i));
						Matcher m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						while (m.find()) 
						{
							String pre = m.group(1);
							String mention = m.group(2);
							String post = m.group(4);
							
							{
								Pattern ptmp = Pattern.compile("^(.+)([ ;.,:]+)$");
								Matcher mtmp = ptmp.matcher(mention);
								if(mtmp.find())
								{
									mention=mtmp.group(1);
									post=mtmp.group(2)+post;
								}
								
								if((!mention.contains("(")) && mention.substring(mention.length()-1,mention.length()).equals(")")){	mention=mention.substring(0,mention.length()-1);post=")"+post;}
								if((!mention.contains("[")) && mention.substring(mention.length()-1,mention.length()).equals("]")){	mention=mention.substring(0,mention.length()-1);post="]"+post;}
								if((!mention.contains("{")) && mention.substring(mention.length()-1,mention.length()).equals("}")){	mention=mention.substring(0,mention.length()-1);post="}"+post;}
								
								int start=pre.length();
								int last=pre.length()+mention.length();
								for(int j=start;j<last;j++)
								{
									RegEx_HGVs_hash.put(j,"SNP");
								}
								String mention_rev="";
								for(int j=0;j<mention.length();j++){mention_rev=mention_rev+"@";}
								Document_rev=pre+mention_rev+post;
							}
							PATTERN_RegEx_HGVs = Pattern.compile(tmVar.RegEx_SNP_STR.get(i));
							m = PATTERN_RegEx_HGVs.matcher(Document_rev);
						}
					}
					
					/*
					 * Tokenization for .location
					 */
					Document_rev=Document;
					Document_rev = Document_rev.replaceAll("([A-Z][A-Z])([A-Z][0-9][0-9]+[A-Z][\\W\\-\\_])", "$1 $2"); //PTENK289E
					Document_rev = Document_rev.replaceAll("([0-9])([A-Za-z])", "$1 $2");
					Document_rev = Document_rev.replaceAll("([A-Za-z])([0-9])", "$1 $2");
					Document_rev = Document_rev.replaceAll("([A-Z])([a-z])", "$1 $2");
					Document_rev = Document_rev.replaceAll("([a-z])([A-Z])", "$1 $2");
					Document_rev = Document_rev.replaceAll("(.+)fs", "$1 fs");
					Document_rev = Document_rev.replaceAll("[\t ]+", " ");
					String regex="\\s+|(?=\\p{Punct})|(?<=\\p{Punct})";
					String TokensInDoc[]=Document_rev.split(regex);
					String DocumentTmp=Document;
					int Offset=0;
					
					Document_rev = Document_rev.replaceAll("ω","w");
					Document_rev = Document_rev.replaceAll("μ","u");
					Document_rev = Document_rev.replaceAll("κ","k");
					Document_rev = Document_rev.replaceAll("α","a");
					Document_rev = Document_rev.replaceAll("γ","r");
					Document_rev = Document_rev.replaceAll("β","b");
					Document_rev = Document_rev.replaceAll("×","x");
					Document_rev = Document_rev.replaceAll("¹","1");
					Document_rev = Document_rev.replaceAll("²","2");
					Document_rev = Document_rev.replaceAll("°","o");
					Document_rev = Document_rev.replaceAll("ö","o");
					Document_rev = Document_rev.replaceAll("é","e");
					Document_rev = Document_rev.replaceAll("à","a");
					Document_rev = Document_rev.replaceAll("Á","A");
					Document_rev = Document_rev.replaceAll("ε","e");
					Document_rev = Document_rev.replaceAll("θ","O");
					Document_rev = Document_rev.replaceAll("•",".");
					Document_rev = Document_rev.replaceAll("µ","u");
					Document_rev = Document_rev.replaceAll("λ","r");
					Document_rev = Document_rev.replaceAll("⁺","+");
					Document_rev = Document_rev.replaceAll("ν","v");
					Document_rev = Document_rev.replaceAll("ï","i");
					Document_rev = Document_rev.replaceAll("ã","a");
					Document_rev = Document_rev.replaceAll("≡","=");
					Document_rev = Document_rev.replaceAll("ó","o");
					Document_rev = Document_rev.replaceAll("³","3");
					Document_rev = Document_rev.replaceAll("〖","[");
					Document_rev = Document_rev.replaceAll("〗","]");
					Document_rev = Document_rev.replaceAll("Å","A");
					Document_rev = Document_rev.replaceAll("ρ","p");
					Document_rev = Document_rev.replaceAll("ü","u");
					Document_rev = Document_rev.replaceAll("ɛ","e");
					Document_rev = Document_rev.replaceAll("č","c");
					Document_rev = Document_rev.replaceAll("š","s");
					Document_rev = Document_rev.replaceAll("ß","b");
					Document_rev = Document_rev.replaceAll("═","=");
					Document_rev = Document_rev.replaceAll("£","L");
					Document_rev = Document_rev.replaceAll("Ł","L");
					Document_rev = Document_rev.replaceAll("ƒ","f");
					Document_rev = Document_rev.replaceAll("ä","a");
					Document_rev = Document_rev.replaceAll("–","-");
					Document_rev = Document_rev.replaceAll("⁻","-");
					Document_rev = Document_rev.replaceAll("〈","<");
					Document_rev = Document_rev.replaceAll("〉",">");
					Document_rev = Document_rev.replaceAll("χ","X");
					Document_rev = Document_rev.replaceAll("Đ","D");
					Document_rev = Document_rev.replaceAll("‰","%");
					Document_rev = Document_rev.replaceAll("·",".");
					Document_rev = Document_rev.replaceAll("→",">");
					Document_rev = Document_rev.replaceAll("←","<");
					Document_rev = Document_rev.replaceAll("ζ","z");
					Document_rev = Document_rev.replaceAll("π","p");
					Document_rev = Document_rev.replaceAll("τ","t");
					Document_rev = Document_rev.replaceAll("ξ","X");
					Document_rev = Document_rev.replaceAll("η","h");
					Document_rev = Document_rev.replaceAll("ø","0");
					Document_rev = Document_rev.replaceAll("Δ","D");
					Document_rev = Document_rev.replaceAll("∆","D");
					Document_rev = Document_rev.replaceAll("∑","S");
					Document_rev = Document_rev.replaceAll("Ω","O");
					Document_rev = Document_rev.replaceAll("δ","d");
					Document_rev = Document_rev.replaceAll("σ","s");
					Document_rev = Document_rev.replaceAll("Φ","F");
					//Document_rev = Document_rev.replaceAll("[^0-9A-Za-z\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\_\\+\\-\\=\\{\\}\\|\\[\\]\\\\\\:;'\\<\\>\\?\\,\\.\\/\\' ]+"," ");
					//tmVar.tagger = new MaxentTagger("lib/taggers/english-left3words-distsim.tagger");
					String tagged=tmVar.tagger.tagString(Document_rev).replace("-LRB-", "(").replace("-RRB-", ")").replace("-LSB-", "[").replace("-RSB-", "]");
					String tag_split[]=tagged.split(" ");
	        		HashMap<String,String> POS=new HashMap<String,String>();
	        		for(int p=0;p<tag_split.length;p++)
	                {
	                	String tmp[]=tag_split[p].split("_");
	                	String tmp2[]=tmp[0].replaceAll("\\s+(?=\\p{Punct})", "").split(regex);
	                	for(int q=0;q<tmp2.length;q++)
	                	{
	                		if(tmp2[q].matches("[^0-9a-zA-Z]"))
	                		{
	                			POS.put(tmp2[q], tmp2[q]);
	                		}
	                		else if(tmp2[q].matches("[0-9a-zA-Z]+"))
	                		{
	                			POS.put(tmp2[q], tmp[1]);
	                		}
	                		else
	                		{
	                			POS.put(tmp2[q], tmp2[q]);
	                		}
	                	}
	                }
					
					for(int i=0;i<TokensInDoc.length;i++)
					{
						if(DocumentTmp.length()>0)
						{
							while(DocumentTmp.substring(0,1).matches("[\\t ]"))
							{
								DocumentTmp=DocumentTmp.substring(1);
								Offset++;
							}
							if(DocumentTmp.substring(0,TokensInDoc[i].length()).equals(TokensInDoc[i]))
							{
								if(TokensInDoc[i].length()>0)
								{
									DocumentTmp=DocumentTmp.substring(TokensInDoc[i].length());
									
									/*
									 * Feature Extration
									 */
									//PST
									String pos=POS.get(TokensInDoc[i]);
									if(pos == null || pos.equals(""))
									{
										pos = "_NULL_";
									}
									
									//stemming
									tmVar.stemmer.setCurrent(TokensInDoc[i].toLowerCase());
									tmVar.stemmer.stem();
									String stem=tmVar.stemmer.getCurrent();
									
									//Number of Numbers [0-9]
									String Num_num="";
									String tmp=TokensInDoc[i];
									tmp=tmp.replaceAll("[^0-9]","");
									if(tmp.length()>3){Num_num="N:4+";}else{Num_num="N:"+ tmp.length();}
									
									//Number of Uppercase [A-Z]
									String Num_Uc="";
									tmp=TokensInDoc[i];
									tmp=tmp.replaceAll("[^A-Z]","");
									if(tmp.length()>3){Num_Uc="U:4+";}else{Num_Uc="U:"+ tmp.length();}
									
									//Number of Lowercase [a-z]
									String Num_lc="";
									tmp=TokensInDoc[i];
									tmp=tmp.replaceAll("[^a-z]","");
									if(tmp.length()>3){Num_lc="L:4+";}else{Num_lc="L:"+ tmp.length();}
									
									//Number of ALL char
									String Num_All="";
									if(TokensInDoc[i].length()>3){Num_All="A:4+";}else{Num_All="A:"+ TokensInDoc[i].length();}
									
									//specific character (;:,.->+_)
									String SpecificC="";
									tmp=TokensInDoc[i];
									
									if(TokensInDoc[i].equals(";") || TokensInDoc[i].equals(":") || TokensInDoc[i].equals(",") || TokensInDoc[i].equals(".") || TokensInDoc[i].equals("-") || TokensInDoc[i].equals(">") || TokensInDoc[i].equals("+") || TokensInDoc[i].equals("_"))
									{
										SpecificC="-SpecificC1-";
									}
									else if(TokensInDoc[i].equals("(") || TokensInDoc[i].equals(")"))
									{
										SpecificC="-SpecificC2-";
									}
									else if(TokensInDoc[i].equals("{") || TokensInDoc[i].equals("}"))
									{
										SpecificC="-SpecificC3-";
									}
									else if(TokensInDoc[i].equals("[") || TokensInDoc[i].equals("]"))
									{
										SpecificC="-SpecificC4-";
									}
									else if(TokensInDoc[i].equals("\\") || TokensInDoc[i].equals("/"))
									{
										SpecificC="-SpecificC5-";
									}
									else
									{
										SpecificC="__nil__";
									}
	
									//chromosomal keytokens
									String ChroKey="";
									tmp=TokensInDoc[i];
									String pattern_ChroKey="^(q|p|qter|pter|XY|t)$";
									Pattern pattern_ChroKey_compile = Pattern.compile(pattern_ChroKey);
									Matcher pattern_ChroKey_compile_Matcher = pattern_ChroKey_compile.matcher(tmp);
									if(pattern_ChroKey_compile_Matcher.find())
									{
										ChroKey="-ChroKey-";
									}
									else
									{
										ChroKey="__nil__";
									}
									
									//Mutation type
									String MutatType="";
									tmp=TokensInDoc[i];
									tmp=tmp.toLowerCase();
									String pattern_MutatType="^(del|ins|dup|tri|qua|con|delins|indel)$";
									String pattern_FrameShiftType="(fs|fsX|fsx)";
									Pattern pattern_MutatType_compile = Pattern.compile(pattern_MutatType);
									Pattern pattern_FrameShiftType_compile = Pattern.compile(pattern_FrameShiftType);
									Matcher pattern_MutatType_compile_Matcher = pattern_MutatType_compile.matcher(tmp);
									Matcher pattern_FrameShiftType_compile_Matcher = pattern_FrameShiftType_compile.matcher(tmp);
									if(pattern_MutatType_compile_Matcher.find())
									{
										MutatType="-MutatType-";
									}
									else if(pattern_FrameShiftType_compile_Matcher.find())
									{
										MutatType="-FrameShiftType-";
									}
									else
									{
										MutatType="__nil__";
									}
									
									//Mutation word
									String MutatWord="";
									tmp=TokensInDoc[i];
									tmp=tmp.toLowerCase();
									String pattern_MutatWord="^(deletion|delta|elta|insertion|repeat|inversion|deletions|insertions|repeats|inversions)$";
									Pattern pattern_MutatWord_compile = Pattern.compile(pattern_MutatWord);
									Matcher pattern_MutatWord_compile_Matcher = pattern_MutatWord_compile.matcher(tmp);
									if(pattern_MutatWord_compile_Matcher.find())
									{
										MutatWord="-MutatWord-";
									}
									else
									{
										MutatWord="__nil__";
									}
									
									//Mutation article & basepair
									String MutatArticle="";
									tmp=TokensInDoc[i];
									tmp=tmp.toLowerCase();
									String pattern_base="^(single|a|one|two|three|four|five|six|seven|eight|nine|ten|[0-9]+)$";
									String pattern_Byte="^(kb|mb)$";
									String pattern_bp="(base|bases|pair|amino|acid|acids|codon|postion|postions|bp|nucleotide|nucleotides)";
									Pattern pattern_base_compile = Pattern.compile(pattern_base);
									Pattern pattern_Byte_compile = Pattern.compile(pattern_Byte);
									Pattern pattern_bp_compile = Pattern.compile(pattern_bp);
									Matcher pattern_base_compile_Matcher = pattern_base_compile.matcher(tmp);
									Matcher pattern_Byte_compile_Matcher = pattern_Byte_compile.matcher(tmp);
									Matcher pattern_bp_compile_Matcher = pattern_bp_compile.matcher(tmp);
									if(pattern_base_compile_Matcher.find())
									{
										MutatArticle="-Base-";
									}
									else if(pattern_Byte_compile_Matcher.find())
									{
										MutatArticle="-Byte-";
									}
									else if(pattern_bp_compile_Matcher.find())
									{
										MutatArticle="-bp-";
									}
									else
									{
										MutatArticle="__nil__";
									}
									
									//Type1
									String Type1="";
									tmp=TokensInDoc[i];
									tmp=tmp.toLowerCase();
									String pattern_Type1="^[cgrm]$";
									String pattern_Type1_2="^(ivs|ex|orf)$";
									Pattern pattern_Type1_compile = Pattern.compile(pattern_Type1);
									Pattern pattern_Type1_2_compile = Pattern.compile(pattern_Type1_2);
									Matcher pattern_Type1_compile_Matcher = pattern_Type1_compile.matcher(tmp);
									Matcher pattern_Type1_2_compile_Matcher = pattern_Type1_2_compile.matcher(tmp);
									if(pattern_Type1_compile_Matcher.find())
									{
										Type1="-Type1-";
									}
									else if(pattern_Type1_2_compile_Matcher.find())
									{
										Type1="-Type1_2-";
									}
									else
									{
										Type1="__nil__";
									}
									
									//Type2
									String Type2="";
									tmp=TokensInDoc[i];
									
									if(tmp.equals("p"))
									{
										Type2="-Type2-";
									}
									else
									{
										Type2="__nil__";
									}
									
									//DNA symbols
									String DNASym="";
									tmp=TokensInDoc[i];
									String pattern_DNASym="^[ATCGUatcgu]$";
									Pattern pattern_DNASym_compile = Pattern.compile(pattern_DNASym);
									Matcher pattern_DNASym_compile_Matcher = pattern_DNASym_compile.matcher(tmp);
									if(pattern_DNASym_compile_Matcher.find())
									{
										DNASym="-DNASym-";
									}
									else
									{
										DNASym="__nil__";
									}
									
									//Protein symbols
									String ProteinSym="";
									String lastToken="";
									tmp=TokensInDoc[i];
									if(i>0){lastToken=TokensInDoc[i-1];}
									String pattern_ProteinSymFull="(glutamine|glutamic|leucine|valine|isoleucine|lysine|alanine|glycine|aspartate|methionine|threonine|histidine|aspartic|asparticacid|arginine|asparagine|tryptophan|proline|phenylalanine|cysteine|serine|glutamate|tyrosine|stop|frameshift)";
									String pattern_ProteinSymTri="^(cys|ile|ser|gln|met|asn|pro|lys|asp|thr|phe|ala|gly|his|leu|arg|trp|val|glu|tyr|fs|fsx)$";
									String pattern_ProteinSymTriSub="^(ys|le|er|ln|et|sn|ro|ys|sp|hr|he|la|ly|is|eu|rg|rp|al|lu|yr)$";
									String pattern_ProteinSymChar="^[CISQMNPKDTFAGHLRWVEYX]$";
									String pattern_lastToken="^[CISQMNPKDTFAGHLRWVEYX]$";
									Pattern pattern_ProteinSymFull_compile = Pattern.compile(pattern_ProteinSymFull);
									Matcher pattern_ProteinSymFull_compile_Matcher = pattern_ProteinSymFull_compile.matcher(tmp);
									Pattern pattern_ProteinSymTri_compile = Pattern.compile(pattern_ProteinSymTri);
									Matcher pattern_ProteinSymTri_compile_Matcher = pattern_ProteinSymTri_compile.matcher(tmp);
									Pattern pattern_ProteinSymTriSub_compile = Pattern.compile(pattern_ProteinSymTriSub);
									Matcher pattern_ProteinSymTriSub_compile_Matcher = pattern_ProteinSymTriSub_compile.matcher(tmp);
									Pattern pattern_ProteinSymChar_compile = Pattern.compile(pattern_ProteinSymChar);
									Matcher pattern_ProteinSymChar_compile_Matcher = pattern_ProteinSymChar_compile.matcher(tmp);
									Pattern pattern_lastToken_compile = Pattern.compile(pattern_lastToken);
									Matcher pattern_lastToken_compile_Matcher = pattern_lastToken_compile.matcher(lastToken);
									
									if(pattern_ProteinSymFull_compile_Matcher.find())
									{
										ProteinSym="-ProteinSymFull-";
									}
									else if(pattern_ProteinSymTri_compile_Matcher.find())
									{
										ProteinSym="-ProteinSymTri-";
									}
									else if(pattern_ProteinSymTriSub_compile_Matcher.find() && pattern_lastToken_compile_Matcher.find() && !Document.substring(Offset-1,Offset).equals(" "))
									{
										ProteinSym="-ProteinSymTriSub-";
									}
									else if(pattern_ProteinSymChar_compile_Matcher.find())
									{
										ProteinSym="-ProteinSymChar-";
									}
									else
									{
										ProteinSym="__nil__";
									}
									
									//RS
									String RScode="";
									tmp=TokensInDoc[i];
									String pattern_RScode="^(rs|RS|Rs)$";
									Pattern pattern_RScode_compile = Pattern.compile(pattern_RScode);
									Matcher pattern_RScode_compile_Matcher = pattern_RScode_compile.matcher(tmp);
									if(pattern_RScode_compile_Matcher.find())
									{
										RScode="-RScode-";
									}
									else
									{
										RScode="__nil__";
									}
									
									//Patterns
									String Pattern1=TokensInDoc[i];
									String Pattern2=TokensInDoc[i];
									String Pattern3=TokensInDoc[i];
									String Pattern4=TokensInDoc[i];
									Pattern1=Pattern1.replaceAll("[A-Z]","A");
									Pattern1=Pattern1.replaceAll("[a-z]","a");
									Pattern1=Pattern1.replaceAll("[0-9]","0");
									Pattern1="P1:"+Pattern1;
									Pattern2=Pattern2.replaceAll("[A-Za-z]","a");
									Pattern2=Pattern2.replaceAll("[0-9]","0");
									Pattern2="P2:"+Pattern2;
									Pattern3=Pattern3.replaceAll("[A-Z]+","A");
									Pattern3=Pattern3.replaceAll("[a-z]+","a");
									Pattern3=Pattern3.replaceAll("[0-9]+","0");
									Pattern3="P3:"+Pattern3;
									Pattern4=Pattern4.replaceAll("[A-Za-z]+","a");
									Pattern4=Pattern4.replaceAll("[0-9]+","0");
									Pattern4="P4:"+Pattern4;
									
									//prefix
									String prefix="";
									tmp=TokensInDoc[i];
									if(tmp.length()>=1){ prefix=tmp.substring(0, 1);}else{prefix="__nil__";}
									if(tmp.length()>=2){ prefix=prefix+" "+tmp.substring(0, 2);}else{prefix=prefix+" __nil__";}
									if(tmp.length()>=3){ prefix=prefix+" "+tmp.substring(0, 3);}else{prefix=prefix+" __nil__";}
									if(tmp.length()>=4){ prefix=prefix+" "+tmp.substring(0, 4);}else{prefix=prefix+" __nil__";}
									if(tmp.length()>=5){ prefix=prefix+" "+tmp.substring(0, 5);}else{prefix=prefix+" __nil__";}
									
									
									//suffix
									String suffix="";
									tmp=TokensInDoc[i];
									if(tmp.length()>=1){ suffix=tmp.substring(tmp.length()-1, tmp.length());}else{suffix="__nil__";}
									if(tmp.length()>=2){ suffix=suffix+" "+tmp.substring(tmp.length()-2, tmp.length());}else{suffix=suffix+" __nil__";}
									if(tmp.length()>=3){ suffix=suffix+" "+tmp.substring(tmp.length()-3, tmp.length());}else{suffix=suffix+" __nil__";}
									if(tmp.length()>=4){ suffix=suffix+" "+tmp.substring(tmp.length()-4, tmp.length());}else{suffix=suffix+" __nil__";}
									if(tmp.length()>=5){ suffix=suffix+" "+tmp.substring(tmp.length()-5, tmp.length());}else{suffix=suffix+" __nil__";}
									
									/*
									 * Print out: .data
									 */
									FileData.write(TokensInDoc[i]+" "+stem+" "+pos+" "+Num_num+" "+Num_Uc+" "+Num_lc+" "+Num_All+" "+SpecificC+" "+ChroKey+" "+MutatType+" "+MutatWord+" "+MutatArticle+" "+Type1+" "+Type2+" "+DNASym+" "+ProteinSym+" "+RScode+" "+Pattern1+" "+Pattern2+" "+Pattern3+" "+Pattern4+" "+prefix+" "+suffix);
									if(RegEx_HGVs_hash.containsKey(Offset))
									{
										FileData.write(" "+RegEx_HGVs_hash.get(Offset));
									}
									else
									{
										FileData.write(" O");
									}
									if(TrainTest.equals("Train")) // Test
									{
										if(character_hash.containsKey(Offset))
										{
											FileData.write(" "+character_hash.get(Offset));
										}
										else
										{
											FileData.write(" O");
										}
									}
									FileData.write("\n");
									
									/*
									 * Print out: .location
									 */
									FileLocation.write(Pmid+"\t"+TokensInDoc[i]+"\t"+(Offset+1)+"\t"+(Offset+TokensInDoc[i].length())+"\n");
									
									Offset=Offset+TokensInDoc[i].length();
								}
							}
							else
							{
								System.out.println("Error! String not match: '"+DocumentTmp.substring(0,TokensInDoc[i].length())+"'\t'"+TokensInDoc[i]+"'");
							}
						}
					}
	
					FileLocation.write("\n"); 
					FileData.write("\n");
					
					ParagraphType.clear();
					ParagraphContent.clear();
					annotations.clear();
					RegEx_HGVs_hash.clear();
					character_hash.clear();
				}
			}
			
			inputfile.close();	
			FileLocation.close();
			FileData.close();
		}
		catch(IOException e1){ System.out.println("[MR]: Input file is not exist.");}
	}

	/*
	 * Testing by CRF++
	 */
	public void CRF_test(String FilenameData,String FilenameOutput,String TrainTest) throws IOException 
	{
		File f = new File(FilenameOutput);
        BufferedWriter fr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
		
		Runtime runtime = Runtime.getRuntime();
	    
		String OS=System.getProperty("os.name").toLowerCase();
		String model="MentionExtractionUB.Model";
		
		String cmd="./CRF/crf_test -m CRF/"+model+" -o "+FilenameOutput+" "+FilenameData;
		if(TrainTest.equals("Test_FullText"))
		{
			model="MentionExtractionUB.fulltext.Model";
		}
	    if(OS.contains("windows"))
	    {
	    	cmd ="CRF/crf_test -m CRF/"+model+" -o "+FilenameOutput+" "+FilenameData;
	    }
	    else //if(OS.contains("nux")||OS.contains("nix"))
	    {
	    	cmd ="./CRF/crf_test -m CRF/"+model+" -o "+FilenameOutput+" "+FilenameData;
	    }
	    
	    try {
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
	
	/*
	 * Learning model by CRF++
	 */
	public void CRF_learn(String FilenameData) throws IOException 
	{
		Process process = null;
	    String line = null;
	    InputStream is = null;
	    InputStreamReader isr = null;
	    BufferedReader br = null;
	    
	    Runtime runtime = Runtime.getRuntime();
	    String OS=System.getProperty("os.name").toLowerCase();
		String cmd="./CRF/crf_learn -f 3 -c 4.0 CRF/template_UB "+FilenameData+" CRF/MentionExtractionUB.Model.new"; 
	    if(OS.contains("windows"))
	    {
	    	cmd ="CRF/crf_learn -f 3 -c 4.0 CRF/template_UB "+FilenameData+" CRF/MentionExtractionUB.Model.new"; 
	    }
	    else //if(OS.contains("nux")||OS.contains("nix"))
	    {
	    	cmd ="./CRF/crf_learn -f 3 -c 4.0 CRF/template_UB "+FilenameData+" CRF/MentionExtractionUB.Model.new"; 
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
}


