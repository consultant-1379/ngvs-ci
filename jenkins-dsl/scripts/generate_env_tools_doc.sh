echo "##################################" >> tools.txt
echo "### ETA controlled tools in RM ###" >> tools.txt
echo "##################################" >> tools.txt
echo "$(java -version 2>&1)" | grep "java version" | awk '{ print "JAVA-Version : ", substr($3, 2, length($3)-2); }' >> tools.txt
echo "$(mvn -version 2>&1)" | grep "Apache Maven" | awk '{ print "Maven-Version : ", substr($3, 1, length($3)); }' >> tools.txt
echo "$(git --version 2>&1)" | grep "git version" | awk '{ print "Git-Version : ", substr($3, 1, length($3)); }' >> tools.txt
echo "Node-Version : $(node -v 2>&1)" >> tools.txt
echo "NPM-Version : $(npm -v 2>&1)" >> tools.txt
echo "$(cdt2 --version 2>&1)" | grep "Client Development Tools" | awk '{ print "CDT-Version : ", $5; }' >> tools.txt
echo "$(cdt2 --version 2>&1)" | grep "cdt-" | sed 's/@/:/g' >> tools.txt
