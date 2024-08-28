wget https://services.gradle.org/distributions/gradle-2.4-all.zip
unzip gradle-2.4-all.zip
git clone https://github.com/sheehan/job-dsl-playground.git
cd job-dsl-playground
export PATH="${pwd}../gradle-2.4/bin:$PATH"
gradle

echo "All done"
