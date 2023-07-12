SET CWD=%cd%
CD C:\Development\KNIME\4.6\knime-rdkit\org.rdkit.knime.update\target
MKDIR Reports
CALL C:\Applications\dependency-check\bin\dependency-check.bat --scan "./repository" --out "./reports" --format "HTML" --format "JSON" --prettyPrint --project "RDKit Nodes" --failOnCVSS 7 --suppression "../../owasp-suppressions.xml" --proxyserver "nibr-proxy.global.nibr.novartis.net" --proxyport 2011 --nonProxyHosts "localhost,novartis.net,novartis.intra"
CD %CWD%
