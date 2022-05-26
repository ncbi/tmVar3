[Directory]
A. Introduction of folders
B. Installation
C. Instruction
D. FULL Usage
E. Variant Normalization

#======================================================================================#

A. [Introduction of folders]
	
	Library & Module:
		[lib]
		[CRF++]
	Model & Regular Expression tables:
		[model]
		[RegEx]
	Input data folder:
		[input]
	Output data folder:
		[output]
	TMP folder:
		[tmp]
	
B. [Installation] 

	*Windows Environment
		Users don't need to install CRF++ module if their operation system is windows.
	
	*Linux Environment
		Users need to execute "Installation.sh" to re-compile crf_test first.

		$ ./Installation.sh
		
		If failed by using Installation.sh. Please reinstall CRF++ by below steps:
		
		1) Download CRF++-0.58.tar.gz from https://drive.google.com/folderview?id=0B4y35FiV1wh7fngteFhHQUN2Y1B5eUJBNHZUemJYQV9VWlBUb3JlX0xBdWVZTWtSbVBneU0&usp=drive_web#list
		2) Uncompress the file by 
		
			$ tar -zxvf CRF++-0.58.tar.gz

		3) Move all files and folders under CRF++-0.58 to the CRF folder.

		4) Execute below instructions in CRF folder.
			$ ./configure 
			$ makes
			$ su
			$ make install
					
C. [Instruction]

	$ java -Xmx5G -Xms5G -jar tmVar.jar [InputFolder] [OutputFolder]
	
	Example: java -Xmx5G -Xms5G -jar tmVar.jar input output
	
	Input: User can provide the input data folder route ("input" or "C:\input"). 
	Output: User can provide the output data folder route ("output" or "C:\output"). 


D. [FULL Usage] 

	tmVar is developed to find mutation mentions and individual components. 

	INPUT:
	
		Input file folder. Each input file should follow the PubTator or BioC format(http://bioc.sourceforge.net/). 

	RUN: 

		java -Xmx5G -Xms5G -jar tmVar.jar input output

	OUTPUT:

		Output file folder. Each output file should follow the PubTator or BioC format(http://bioc.sourceforge.net/). 

E. [Variant Normalization]

	Based on the genes (e.g., BRAF) in the text, tmVar3.0 can normalize the variants (e.g., V600E) to dbSNP RS# (e.g., rs113488022) and ClinGen Allele Registry # (e.g., CA123643). To obtatin the genes in the text, user needs to download GNormPlus and process the text. The output files of GNormPlus can be the input files of tmVar.

	1. Prepare the files in PubTator/BioC-XML format.
	
	2. Download GNormPlus via https://www.ncbi.nlm.nih.gov/research/bionlp/Tools/gnormplus/ and install it in your local. 
	
	3. Process your prepared files by GNormPlus and move the output files into the tmVar input folder.
	
	4. Process the the GNormPlus output files by tmVar.

	The Identifier column (or infon in BioC-XML) in tmVar output file contains below information:

		- tmVar : The components (e.g., position) of the variants in tmVar format. Example: p|SUB|V|600|E
		- HGVS : The offical variant form follows HGVS nomenclature. Example: p.V600E
		- VariantGroup : The variants with the same group id are the same variant (but might be in different sequence level).
		- CorrespondingGene : The corresponding gene id
		- CorrespondingSpecies : The corresponding species id
		- RS#: The normalized dbSNP RS#
		- CA#: The normalized ClinGen Allele Registry CA#
	