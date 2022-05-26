# tmVar 3.0 introduciton

We propose tmVar 3.0: an improved variant recognition and normalization system. Com-pared to its predecessors, tmVar 3.0 recognizes a wider spectrum of variant related entities (e.g., allele and copy number variants), and to group different variant mentions belonging to the same genomic sequence position in an article for improved accuracy. Moreover, tmVar 3.0 provides ad-vanced variant normalization options such as allele-specific identifiers from the ClinGen Allele Registry. tmVar 3.0 exhibits state-of-the-art performance with over 90% in F-measure for variant recognition and normalization, when evaluated on three independent benchmarking datasets. tmVar 3.0 as well annotations for the entire PubMed and PMC datasets are freely available for download on our FTP.

# tmVar 3.0 download from FTP

	https://ftp.ncbi.nlm.nih.gov/pub/lu/tmVar3

# PubTator API to access tmVar 3.0

We host a RESTful API (https://www.ncbi.nlm.nih.gov/research/pubtator/api.html) that users can access the tmVar 3.0 results in PubMed/PMC. The "Process Raw Text" section of the API page also shows the way to submit a raw text for online processing. We provide the sample code in Java, Python and Perl to assist the users to quickly familiar with the API service.
