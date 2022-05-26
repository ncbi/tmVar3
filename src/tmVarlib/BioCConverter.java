//
// tmVar - Java version
// BioC Format Converter
//
package tmVarlib;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.io.BioCDocumentWriter;
import bioc.io.BioCFactory;
import bioc.io.woodstox.ConnectorWoodstox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import java.util.ArrayList;
import java.util.HashMap;

public class BioCConverter 
{
	/*
	 * Contexts in BioC file
	 */
	public ArrayList<String> PMIDs=new ArrayList<String>(); // Type: PMIDs
	public ArrayList<ArrayList<String>> PassageNames = new ArrayList(); // PassageName
	public ArrayList<ArrayList<Integer>> PassageOffsets = new ArrayList(); // PassageOffset
	public ArrayList<ArrayList<String>> PassageContexts = new ArrayList(); // PassageContext
	public ArrayList<ArrayList<ArrayList<String>>> Annotations = new ArrayList(); // Annotation - GNormPlus
	
	public String BioCFormatCheck(String InputFile) throws IOException
	{
		
		ConnectorWoodstox connector = new ConnectorWoodstox();
		BioCCollection collection = new BioCCollection();
		try
		{
			collection = connector.startRead(new InputStreamReader(new FileInputStream(InputFile), "UTF-8"));
		}
		catch (UnsupportedEncodingException | FileNotFoundException | XMLStreamException e) 
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(InputFile), "UTF-8"));
			String line="";
			String status="";
			String Pmid = "";
			boolean tiabs=false;
			Pattern patt = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
			while ((line = br.readLine()) != null)  
			{
				Matcher mat = patt.matcher(line);
				if(mat.find()) //Title|Abstract
	        	{
					if(Pmid.equals(""))
					{
						Pmid = mat.group(1);
					}
					else if(!Pmid.equals(mat.group(1)))
					{
						return "[Error]: "+InputFile+" - A blank is needed between "+Pmid+" and "+mat.group(1)+".";
					}
					status = "tiabs";
					tiabs = true;
	        	}
				else if (line.contains("\t")) //Annotation
	        	{
	        	}
				else if(line.length()==0) //Processing
				{
					if(status.equals(""))
					{
						if(Pmid.equals(""))
						{
							return "[Error]: "+InputFile+" - It's not either BioC or PubTator format.";
						}
						else
						{
							return "[Error]: "+InputFile+" - A redundant blank is after "+Pmid+".";
						}
					}
					Pmid="";
					status="";
				}
			}
			br.close();
			if(tiabs == false)
			{
				return "[Error]: "+InputFile+" - It's not either BioC or PubTator format.";
			}
			if(status.equals(""))
			{
				return "PubTator";
			}
			else
			{
				return "[Error]: "+InputFile+" - The last column missed a blank.";
			}
		}
		return "BioC";
	}
	public void BioC2PubTator(String input,String output) throws IOException, XMLStreamException
	{
		/*
		 * BioC2PubTator
		 */
		HashMap<String, String> pmidlist = new HashMap<String, String>(); // check if appear duplicate pmids
		boolean duplicate = false;
		BufferedWriter PubTatorOutputFormat = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
		ConnectorWoodstox connector = new ConnectorWoodstox();
		BioCCollection collection = new BioCCollection();
		collection = connector.startRead(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		while (connector.hasNext()) 
		{
			BioCDocument document = connector.next();
			String PMID = document.getID();
			if(pmidlist.containsKey(PMID)){System.out.println("\nError: duplicate pmid-"+PMID);duplicate = true;}
			else{pmidlist.put(PMID,"");}
			String Anno="";
			int realpassage_offset=0;
			for (BioCPassage passage : document.getPassages()) 
			{
				if(passage.getInfon("type").toLowerCase().equals("table"))
				{
					String temp=passage.getText().replaceAll(" ", ";");
					temp=temp.replaceAll(";>;", " > ");
					PubTatorOutputFormat.write(PMID+"|"+passage.getInfon("type")+"|"+temp+"\n");
				}
				else
				{
					String temp=passage.getText();
					if(passage.getText().equals(""))
					{
						PubTatorOutputFormat.write(PMID+"|"+passage.getInfon("type")+"|"+"\n");//- No text -
					}
					else
					{
						PubTatorOutputFormat.write(PMID+"|"+passage.getInfon("type")+"|"+temp+"\n");
					}
				}
				for (BioCAnnotation annotation : passage.getAnnotations()) 
				{
					String Annoid = annotation.getInfon("identifier");
					if(Annoid == null)
					{
						Annoid=annotation.getInfon("NCBI Gene");
					}
					if(Annoid == null)
					{
						Annoid = annotation.getInfon("Identifier");
					}
					String Annotype = annotation.getInfon("type");
					int start = annotation.getLocations().get(0).getOffset();
					start=start-(passage.getOffset()-realpassage_offset);
					int last = start + annotation.getLocations().get(0).getLength();
					String AnnoMention=annotation.getText();
					Anno=Anno+PMID+"\t"+start+"\t"+last+"\t"+AnnoMention+"\t"+Annotype+"\t"+Annoid+"\n";
				}
				realpassage_offset=realpassage_offset+passage.getText().length()+1;
			}
			PubTatorOutputFormat.write(Anno+"\n");
		}
		PubTatorOutputFormat.close();
		if(duplicate == true){System.exit(0);}
	}
	public void PubTator2BioC(String input,String output) throws IOException, XMLStreamException
	{
		/*
		 *  PubTator2BioC
		 */
		String parser = BioCFactory.WOODSTOX;
		BioCFactory factory = BioCFactory.newFactory(parser);
		BioCDocumentWriter BioCOutputFormat = factory.createBioCDocumentWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
		BioCCollection biocCollection = new BioCCollection();
		
		//time
		ZoneId zonedId = ZoneId.of( "America/Montreal" );
		LocalDate today = LocalDate.now( zonedId );
		biocCollection.setDate(today.toString());
		
		biocCollection.setKey("BioC.key");//key
		biocCollection.setSource("tmVar");//source
		
		BioCOutputFormat.writeCollectionInfo(biocCollection);
		BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		ArrayList<String> ParagraphType=new ArrayList<String>(); // Type: Title|Abstract
		ArrayList<String> ParagraphContent = new ArrayList<String>(); // Text
		ArrayList<String> annotations = new ArrayList<String>(); // Annotation
		String line;
		String Pmid="";
		while ((line = inputfile.readLine()) != null)  
		{
			Pattern patt = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
			Matcher mat = patt.matcher(line);
			if(mat.find()) //Title|Abstract
	        {
				ParagraphType.add(mat.group(2));
				ParagraphContent.add(mat.group(3));
	        }
			else if (line.contains("\t")) //Annotation
        	{
				String anno[]=line.split("\t");
				annotations.add(anno[1]+"\t"+anno[2]+"\t"+anno[3]+"\t"+anno[4]+"\t"+anno[5]);
        	}
			else if(line.length()==0) //Processing
			{
				BioCDocument biocDocument = new BioCDocument();
				biocDocument.setID(Pmid);
				int startoffset=0;
				for(int i=0;i<ParagraphType.size();i++)
				{
					BioCPassage biocPassage = new BioCPassage();
					Map<String, String> Infons = new HashMap<String, String>();
					Infons.put("type", ParagraphType.get(i));
					biocPassage.setInfons(Infons);
					biocPassage.setText(ParagraphContent.get(i));
					biocPassage.setOffset(startoffset);
					startoffset=startoffset+ParagraphContent.get(i).length()+1;
					for(int j=0;j<annotations.size();j++)
					{
						String anno[]=annotations.get(j).split("\t");
						if(Integer.parseInt(anno[0])<startoffset && Integer.parseInt(anno[0])>=startoffset-ParagraphContent.get(i).length()-1)
						{
							BioCAnnotation biocAnnotation = new BioCAnnotation();
							Map<String, String> AnnoInfons = new HashMap<String, String>();
							AnnoInfons.put("Identifier", anno[4]);
							AnnoInfons.put("type", anno[3]);
							biocAnnotation.setInfons(AnnoInfons);
							BioCLocation location = new BioCLocation();
							location.setOffset(Integer.parseInt(anno[0]));
							location.setLength(Integer.parseInt(anno[1])-Integer.parseInt(anno[0]));
							biocAnnotation.setLocation(location);
							biocAnnotation.setText(anno[2]);
							biocPassage.addAnnotation(biocAnnotation);
						}
					}
					biocDocument.addPassage(biocPassage);
				}
				biocCollection.addDocument(biocDocument);
				ParagraphType.clear();
				ParagraphContent.clear();
				annotations.clear();
				BioCOutputFormat.writeDocument(biocDocument);
			}
		}
		BioCOutputFormat.close();
		inputfile.close();
	}
	public void PubTator2BioC_AppendAnnotation(String inputPubTator,String inputBioc,String output) throws IOException, XMLStreamException
	{
		/*
		 *  PubTator2BioC
		 */
		
		//input: PubTator
		BufferedReader inputfile = new BufferedReader(new InputStreamReader(new FileInputStream(inputPubTator), "UTF-8"));
		HashMap<String, String> ParagraphType_hash = new HashMap<String, String>(); // Type: Title|Abstract
		HashMap<String, String> ParagraphContent_hash = new HashMap<String, String>(); // Text
		HashMap<String, String> annotations_hash = new HashMap<String, String>(); // Annotation
		String Annotation="";
		String Pmid="";
		String line="";
		while ((line = inputfile.readLine()) != null)  
		{
			Pattern patt = Pattern.compile("^([^\\|\\t]+)\\|([^\\|\\t]+)\\|(.*)$");
			Matcher mat = patt.matcher(line);
			if(mat.find()) //Title|Abstract
	        {
				Pmid=mat.group(1);
				ParagraphType_hash.put(Pmid,mat.group(2));
				ParagraphContent_hash.put(Pmid,mat.group(3));
			}
			else if (line.contains("\t")) //Annotation
        	{
				if(Annotation.equals(""))
				{
					Annotation=line;
				}
				else
				{
					Annotation=Annotation+"\n"+line;
				}
        	}
			else if(line.length()==0) //Processing
			{
				annotations_hash.put(Pmid,Annotation);
				Annotation="";
			}
		}
		inputfile.close();
		
		//output
		BioCDocumentWriter BioCOutputFormat = BioCFactory.newFactory(BioCFactory.WOODSTOX).createBioCDocumentWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
		BioCCollection biocCollection_input = new BioCCollection();
		BioCCollection biocCollection_output = new BioCCollection();
		
		//input: BioC
		ConnectorWoodstox connector = new ConnectorWoodstox();
		biocCollection_input = connector.startRead(new InputStreamReader(new FileInputStream(inputBioc), "UTF-8"));
		BioCOutputFormat.writeCollectionInfo(biocCollection_input);
		while (connector.hasNext()) 
		{
			int real_start_passage=0;
			BioCDocument document_output = new BioCDocument();
			BioCDocument document_input = connector.next();
			String PMID=document_input.getID();
			document_output.setID(PMID);
			int annotation_count=0;
			for (BioCPassage passage_input : document_input.getPassages()) 
			{
				String passage_input_Text = passage_input.getText();
				
				BioCPassage passage_output = passage_input;
				//passage_output.clearAnnotations(); //clean the previous annotation
				for (BioCAnnotation annotation : passage_output.getAnnotations()) 
				{
					annotation.setID(""+annotation_count);
					annotation_count++;
				}
				
				int start_passage=passage_input.getOffset();
				int last_passage=passage_input.getOffset()+passage_input.getText().length();
				if(annotations_hash.containsKey(PMID) && !annotations_hash.get(PMID).equals(""))
				{
					String Anno[]=annotations_hash.get(PMID).split("\\n");
					for(int i=0;i<Anno.length;i++)
					{
						String An[]=Anno[i].split("\\t");
						int start=Integer.parseInt(An[1]);
						int last=Integer.parseInt(An[2]);
						start = start + start_passage - real_start_passage;
						last = last + start_passage - real_start_passage;
						String mention=An[3];
						String type=An[4];
						
						String id="";
						if(An.length>5)
						{
							id=An[5];
						}
						if((start>=start_passage && start<last_passage)||(last>=start_passage && last<last_passage))
						{
							BioCAnnotation biocAnnotation = new BioCAnnotation();
							Map<String, String> AnnoInfons = new HashMap<String, String>();
							AnnoInfons.put("Identifier", id);
							AnnoInfons.put("type", type);
							biocAnnotation.setInfons(AnnoInfons);
							
							/*redirect the offset*/
							//location.setOffset(start);
							//location.setLength(last-start);
							String mention_tmp = mention.replaceAll("([^A-Za-z0-9@ ])", "\\\\$1");
							Pattern patt = Pattern.compile("^(.*)("+mention_tmp+")(.*)$");
							Matcher mat = patt.matcher(passage_input_Text);
							if(mat.find())
							{
								String pre=mat.group(1);
								String men=mat.group(2);
								String post=mat.group(3);
								start=pre.length()+start_passage;
								BioCLocation location = new BioCLocation();
								location.setOffset(start);
								location.setLength(men.length());
								biocAnnotation.setLocation(location);
								biocAnnotation.setText(mention);
								biocAnnotation.setID(""+annotation_count);
								annotation_count++;
								passage_output.addAnnotation(biocAnnotation);
								men=men.replaceAll(".", "@");
								passage_input_Text=pre+men+post;
							}
						}
					}
				}
				real_start_passage = real_start_passage + passage_input.getText().length() + 1;
				document_output.addPassage(passage_output);
			}
			biocCollection_output.addDocument(document_output);
			BioCOutputFormat.writeDocument(document_output);
		}
		BioCOutputFormat.close();
	}
	public void BioCReaderWithAnnotation(String input) throws IOException, XMLStreamException
	{
		ConnectorWoodstox connector = new ConnectorWoodstox();
		BioCCollection collection = new BioCCollection();
		collection = connector.startRead(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		
		/*
		 * Per document
		 */
		while (connector.hasNext()) 
		{
			BioCDocument document = connector.next();
			PMIDs.add(document.getID());
			
			ArrayList<String> PassageName= new ArrayList<String>(); // array of Passage name
			ArrayList<Integer> PassageOffset= new ArrayList<Integer>(); // array of Passage offset
			ArrayList<String> PassageContext= new ArrayList<String>(); // array of Passage context
			ArrayList<ArrayList<String>> AnnotationInPMID= new ArrayList(); // array of Annotations in the PassageName
			
			/*
			 * Per Passage
			 */
			for (BioCPassage passage : document.getPassages()) 
			{
				PassageName.add(passage.getInfon("type")); //Paragraph
				
				String txt = passage.getText();
				if(txt.matches("[\t ]+"))
				{
					txt = txt.replaceAll(".","@");
				}
				else
				{
					//if(passage.getInfon("type").toLowerCase().equals("table"))
					//{
					//	txt=txt.replaceAll(" ", "|");
					//}
					txt = txt.replaceAll("ω","w");
					txt = txt.replaceAll("μ","u");
					txt = txt.replaceAll("κ","k");
					txt = txt.replaceAll("α","a");
					txt = txt.replaceAll("γ","g");
					txt = txt.replaceAll("β","b");
					txt = txt.replaceAll("×","x");
					txt = txt.replaceAll("¹","1");
					txt = txt.replaceAll("²","2");
					txt = txt.replaceAll("°","o");
					txt = txt.replaceAll("ö","o");
					txt = txt.replaceAll("é","e");
					txt = txt.replaceAll("à","a");
					txt = txt.replaceAll("Á","A");
					txt = txt.replaceAll("ε","e");
					txt = txt.replaceAll("θ","O");
					txt = txt.replaceAll("•",".");
					txt = txt.replaceAll("µ","u");
					txt = txt.replaceAll("λ","r");
					txt = txt.replaceAll("⁺","+");
					txt = txt.replaceAll("ν","v");
					txt = txt.replaceAll("ï","i");
					txt = txt.replaceAll("ã","a");
					txt = txt.replaceAll("≡","=");
					txt = txt.replaceAll("ó","o");
					txt = txt.replaceAll("³","3");
					txt = txt.replaceAll("〖","[");
					txt = txt.replaceAll("〗","]");
					txt = txt.replaceAll("Å","A");
					txt = txt.replaceAll("ρ","p");
					txt = txt.replaceAll("ü","u");
					txt = txt.replaceAll("ɛ","e");
					txt = txt.replaceAll("č","c");
					txt = txt.replaceAll("š","s");
					txt = txt.replaceAll("ß","b");
					txt = txt.replaceAll("═","=");
					txt = txt.replaceAll("£","L");
					txt = txt.replaceAll("Ł","L");
					txt = txt.replaceAll("ƒ","f");
					txt = txt.replaceAll("ä","a");
					txt = txt.replaceAll("–","-");
					txt = txt.replaceAll("⁻","-");
					txt = txt.replaceAll("〈","<");
					txt = txt.replaceAll("〉",">");
					txt = txt.replaceAll("χ","X");
					txt = txt.replaceAll("Đ","D");
					txt = txt.replaceAll("‰","%");
					txt = txt.replaceAll("·",".");
					txt = txt.replaceAll("→",">");
					txt = txt.replaceAll("←","<");
					txt = txt.replaceAll("ζ","z");
					txt = txt.replaceAll("π","p");
					txt = txt.replaceAll("τ","t");
					txt = txt.replaceAll("ξ","X");
					txt = txt.replaceAll("η","h");
					txt = txt.replaceAll("ø","0");
					txt = txt.replaceAll("Δ","D");
					txt = txt.replaceAll("∆","D");
					txt = txt.replaceAll("∑","S");
					txt = txt.replaceAll("Ω","O");
					txt = txt.replaceAll("δ","d");
					txt = txt.replaceAll("σ","s");
					txt = txt.replaceAll("Φ","F");
					//txt = txt.replaceAll("[^\\~\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\_\\+\\{\\}\\|\\:\"\\<\\>\\?\\`\\-\\=\\[\\]\\;\\'\\,\\.\\/\\r\\n0-9a-zA-Z ]"," ");
				}
				if(passage.getText().equals("") || passage.getText().matches("[ ]+"))
				{
					PassageContext.add("-notext-"); //Context
				}
				else
				{
					PassageContext.add(txt); //Context
				}
				PassageOffset.add(passage.getOffset()); //Offset
				ArrayList<String> AnnotationInPassage= new ArrayList<String>(); // array of Annotations in the PassageName
				
				/*
				 * Per Annotation :
				 * start
				 * last
				 * mention
				 * type
				 * id
				 */
				for (BioCAnnotation Anno : passage.getAnnotations()) 
				{
					int start = Anno.getLocations().get(0).getOffset()-passage.getOffset(); // start
					int last = start + Anno.getLocations().get(0).getLength(); // last
					String AnnoMention=Anno.getText(); // mention
					String Annotype = Anno.getInfon("type"); // type

					String Annoid="";
					Map<String,String> Infons=Anno.getInfons();
					for(String Infon :Infons.keySet())
					{
						if(!Infon.toLowerCase().equals("type"))
						{
							if(Annoid.equals(""))
							{
								Annoid=Infons.get(Infon);
							}
							else
							{
								Annoid=Annoid+";"+Infons.get(Infon);
							}
						}
					}
					
					if(Annoid == "")
					{
						AnnotationInPassage.add(start+"\t"+last+"\t"+AnnoMention+"\t"+Annotype); //paragraph
					}
					else
					{
						AnnotationInPassage.add(start+"\t"+last+"\t"+AnnoMention+"\t"+Annotype+"\t"+Annoid); //paragraph
					}
				}
				AnnotationInPMID.add(AnnotationInPassage);
			}
			PassageNames.add(PassageName);
			PassageContexts.add(PassageContext);
			PassageOffsets.add(PassageOffset);
			Annotations.add(AnnotationInPMID);
		}	
	}
	public void BioCOutput(String input,String output) throws IOException, XMLStreamException
	{
		BioCDocumentWriter BioCOutputFormat = BioCFactory.newFactory(BioCFactory.WOODSTOX).createBioCDocumentWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
		BioCCollection biocCollection_input = new BioCCollection();
		BioCCollection biocCollection_output = new BioCCollection();
		
		//input: BioC
		ConnectorWoodstox connector = new ConnectorWoodstox();
		biocCollection_input = connector.startRead(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		BioCOutputFormat.writeCollectionInfo(biocCollection_input);
		int i=0; //count for pmid
		while (connector.hasNext()) 
		{
			BioCDocument document_output = new BioCDocument();
			BioCDocument document_input = connector.next();
			document_output.setID(document_input.getID());
			int annotation_count=0;
			int j=0; //count for paragraph
			for (BioCPassage passage_input : document_input.getPassages()) 
			{
				BioCPassage passage_output = passage_input;
				passage_output.clearAnnotations(); //clean the previous annotation
				int passage_Offset = passage_input.getOffset();
				String passage_Text = passage_input.getText();
				ArrayList<String> AnnotationInPassage = Annotations.get(i).get(j);
				for(int a=0;a<AnnotationInPassage.size();a++)
				{
					String Anno[]=AnnotationInPassage.get(a).split("\\t",-1);
					int start = Integer.parseInt(Anno[0]);
					int last = Integer.parseInt(Anno[1]);
					String mention = Anno[2];
					String type = Anno[3];
					BioCAnnotation biocAnnotation = new BioCAnnotation();
					Map<String, String> AnnoInfons = new HashMap<String, String>();
					AnnoInfons.put("type", type);
					String identifier="";
					if(Anno.length==5){identifier=Anno[4];}
					if(type.equals("Gene"))
					{
						AnnoInfons.put("NCBI Gene", identifier);
					}
					else if(type.equals("Species"))
					{
						AnnoInfons.put("NCBI Taxonomy", identifier);
					}
					else
					{
						AnnoInfons.put("Identifier", identifier);
					}
					biocAnnotation.setInfons(AnnoInfons);
					BioCLocation location = new BioCLocation();
					location.setOffset(start+passage_Offset);
					location.setLength(last-start);
					biocAnnotation.setLocation(location);
					biocAnnotation.setText(mention);
					biocAnnotation.setID(""+annotation_count);
					annotation_count++;
					passage_output.addAnnotation(biocAnnotation);
				}
				document_output.addPassage(passage_output);
				j++;
			}
			biocCollection_output.addDocument(document_output);
			BioCOutputFormat.writeDocument(document_output);
			i++;
		}
		BioCOutputFormat.close();
	}	
}