# tmVar 3.0: an improved variant concept recog-nition and normalization tool

We propose tmVar 3.0: an improved variant recognition and normalization system. Com-pared to its predecessors, tmVar 3.0 recognizes a wider spectrum of variant related entities (e.g., allele and copy number variants), and to group different variant mentions belonging to the same genomic sequence position in an article for improved accuracy. Moreover, tmVar 3.0 provides ad-vanced variant normalization options such as allele-specific identifiers from the ClinGen Allele Registry. tmVar 3.0 exhibits state-of-the-art performance with over 90% in F-measure for variant recognition and normalization, when evaluated on three independent benchmarking datasets. tmVar 3.0 as well annotations for the entire PubMed and PMC datasets are freely available for download on our FTP.

# tmVar 3.0 download from FTP

- [tmVar 3.0 software](https://ftp.ncbi.nlm.nih.gov/pub/lu/tmVar3/tmVar3.tar.gz)
- [tmVar 3.0 corpus](https://ftp.ncbi.nlm.nih.gov/pub/lu/tmVar3/tmVar3Corpus.txt)
- [tmVar 3.0 annotation guideline](https://ftp.ncbi.nlm.nih.gov/pub/lu/tmVar3/AnnotationGuideline.rev.docx)

# PubTator API to access tmVar 3.0

We host a RESTful API (https://www.ncbi.nlm.nih.gov/research/pubtator/api.html) that users can access the tmVar 3.0 results in PubMed/PMC. The "Process Raw Text" section of the API page also shows the way to submit a raw text for online processing. We provide the sample code in Java, Python and Perl to assist the users to quickly familiar with the API service.

# Acknowledgments

This research was supported by the Intramural Research Program of the National Library of Medicine (NLM), National Institutes of Health.

# Disclaimer

This tool shows the results of research conducted in the Computational Biology Branch, NCBI. The information produced on this website is not intended for direct diagnostic use or medical decision-making without review and oversight by a clinical professional. Individuals should not change their health behavior solely on the basis of information produced on this website. NIH does not independently verify the validity or utility of the information produced by this tool. If you have questions about the information produced on this website, please see a health care professional. More information about NCBI's disclaimer policy is available.
