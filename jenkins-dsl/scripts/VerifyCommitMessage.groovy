public void main(){
    def proc = '/opt/local/dev_tools/git/latest/bin/git log --format=%B -n 1'.execute()

    String[] commitLines =  proc.text.split('\n')

    if (commitLines != null) {
        String subject = commitLines[0]

        StringBuilder verifyInfo = new StringBuilder()
        StringBuilder trackIdVerifyInfo = new StringBuilder()

        verifyInfo.append(verifyColonInSubjectLine(subject))

        verifyInfo.append(verifyCommitSubjectLine(subject))

        verifyInfo.append(verifyImperativeSubjectLine(subject))

        if (!hasOnlySubject(commitLines)){
            verifyInfo.append(verifySubjectSeparatedFromBody(commitLines[1]))
        }

        verifyInfo.append(verifyLineCouldBeWrapped(commitLines))

        if(verifyInfo.toString() != "") {
            verifyInfo.append('\nHow to write commit message ' +
                    'https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/git.html#Commit_Message')
        }

        if (commitLines.toString().contains('Tracking-Id:')){
            int bodyLineStart = 2
            for (int i = bodyLineStart; i< commitLines.size(); i++){
                trackIdVerifyInfo.append(verifyTrackingIdCommit(commitLines[i]))
            }
        }
        else {
            trackIdVerifyInfo.append('- \'Tracking-Id:\' is missing or invalid, e.g. Tracking-Id: BSUC:11B-11:1, BSUC:MISC, TR:HH12345, PBI:123, CBI:123\n')
        }

        if (trackIdVerifyInfo.toString() == ""){
            trackIdVerifyInfo.append('No issue found')
        }

        writeStringToFile('commitverify.txt', verifyInfo.toString())
        writeStringToFile('trackIdVerify.txt', trackIdVerifyInfo.toString())
    }
}

public String verifyColonInSubjectLine(String subjectLine){
    String trackIdVerifyInfo = ""
    if (subjectLine.contains(':') || subjectLine.trim().startsWith('[')) {
        trackIdVerifyInfo = '- Do not add metadata (colon, bracket) in subject line, add it in the body of the git commit message instead e.g. Tracking-Id: BSUC:12345\n'
    }
    return trackIdVerifyInfo
}

public String verifyCommitSubjectLine(String subject){
    String verifySubjectInfo = ""
    String metaDataName = null
    if (!Character.isUpperCase(subject.charAt(0))) {
        verifySubjectInfo += '- Capitalize the first letter in subject line\n'
    }
    if (subject.length() > 50 && !subject.startsWith("Revert \"")) {
        verifySubjectInfo += '- Limit the subject line to 50 characters\n'
    }
    if (subject.substring(subject.length() - 1).equals('.')) {
        verifySubjectInfo += '- Do not end the subject line with a period\n'
    }
    if (containsMetadata(subject.toUpperCase(), 'BSUC')) {
        metaDataName = 'BSUC'
    }
    else if (containsMetadata(subject.toUpperCase(), 'BUC')) {
        metaDataName = 'BUC'
    }
    else if (containsMetadata(subject.toUpperCase(), 'Tracking-Id')) {
        metaDataName = 'Tracking-Id'
    }
    else if (containsMetadata(subject.toUpperCase(), 'TR')) {
        metaDataName = 'TR'
    }
    else if (containsMetadata(subject.toUpperCase(), 'US')) {
        metaDataName = 'US'
    }
    else if (containsMetadata(subject.toUpperCase(), 'UC')) {
        metaDataName = 'UC'
    }
    else if (containsMetadata(subject.toUpperCase(), 'FT')) {
        metaDataName = 'FT'
    }
    if (metaDataName != null) {
        verifySubjectInfo += '- Do not add metadata ' + metaDataName + ' in subject line, if needed add it in the body e.g. Tracking-Id: BSUC:12345\n'
    }
    if (subject.toUpperCase().equals(subject)) {
        verifySubjectInfo += '- Do not capitalize the whole subject line\n'
    }
    return verifySubjectInfo
}

private String verifyImperativeSubjectLine(String subject) {
    String[] inWords = subject.split(" ", 2)
    int first = 0

    String originalFirstWord = inWords[first].trim()
    String firstWord = originalFirstWord.toLowerCase()

    String out = ""

    if(firstWord.endsWith('ing') ||
            firstWord.endsWith('ed') ||
            firstWord.endsWith('tion') ||
            firstWord.endsWith('es')) {

        out = '- Use the imperative mood in the subject line. Is \'' + originalFirstWord + '\' in imperative mood? (i.e. \'Fix\' instead of \'Fixes/Fixed/Fixing\')\n'
    }

    return out
}

private boolean containsMetadata(String subject, String metadata) {
    final String colon = ':'
    final String whiteSpace = ' '

    if (subject.startsWith(metadata + colon) || subject.startsWith(metadata + whiteSpace)) {
        return true
    }

    return false
}


public String verifyLineCouldBeWrapped(String[] commitLines){
    String lineCouldBeWrappedInfo = ""
    boolean foundLineThatCouldBeWrapped = false
    for (line in commitLines) {
        if (!line.equals(commitLines[0]) && line.length() > 72 && line.contains(' ')) {
            foundLineThatCouldBeWrapped = true
        }
    }
    if (foundLineThatCouldBeWrapped){
        lineCouldBeWrappedInfo += '- Wrap the body at 72 characters\n'
    }
    return lineCouldBeWrappedInfo
}

public boolean hasOnlySubject(String[] commitLines){
    boolean hasOnlySubject = false
    if (commitLines.size() == 1){
        hasOnlySubject = true
    }
    return hasOnlySubject
}

public String verifySubjectSeparatedFromBody(String secondCommitLine){
    String subjectSeparatedFromBody = ""
    if (secondCommitLine.length() != 0) {
        subjectSeparatedFromBody = '- Separate subject from body with a blank line\n'
    }
    return subjectSeparatedFromBody
}

public String verifyTrackingIdCommit(String trackingIdCommitLine){
    String verifyTrackingIdInfo = ""
    if (trackingIdCommitLine.contains('Tracking-Id:')){
        String lineContent = trackingIdCommitLine.replace("Tracking-Id:", "")
        if (hasManyTrackingIds(lineContent)){
            String[] trackIds = lineContent.split(',')
            for (trackId in trackIds){
                verifyTrackingIdInfo += analyzeSingleTrackId(trackId)
            }
        } else {
            verifyTrackingIdInfo += analyzeSingleTrackId(lineContent)
        }
    }
    return verifyTrackingIdInfo
}

public void writeStringToFile(String fileName, String verifyData){
    File file = new File(fileName)
    if (file.exists()) {
        file.delete()
    }
    file << verifyData
}

public boolean hasManyTrackingIds(String trackIdLine){
    boolean hasManyTrackingIds = false
    if (trackIdLine.contains(',')){
        hasManyTrackingIds = true
    }
    return hasManyTrackingIds
}

public String analyzeSingleTrackId(String trackId){
    String analyzeResult = ""

    boolean validEmptyIdInTrackingIdTypes = trackId.trim().toUpperCase() in ["TEST", "SCM", "EIP", "NONE"]
    if (validEmptyIdInTrackingIdTypes){
        return ""
    }

    if (trackId.contains(':')){
        analyzeResult += analyzeSingleTrackIdTypeAndId(trackId)
        return analyzeResult
    }
    analyzeResult += '- \'' + trackId + '\' is invalid, e.g. \'Tracking-Id: <type>:<id>, <type>:<id>, ...\'\n'
    return analyzeResult
}

private String analyzeSingleTrackIdTypeAndId(String trackId){
    String analyzeResult = ""
    String type = trackId.split(':')[0]
    String id = trackId.split(':')[1]
    if (!(type.trim().toUpperCase() in ["BSUC", "TR", "PBI", "CBI", "SCM", "TEST", "EIP", "US"])){
        analyzeResult += '- \'' + type +  '\' is not valid, should be one of [BSUC, TR, PBI, CBI, SCM, TEST, EIP, US]\n'
    }
    if (id.trim().isEmpty()){
        analyzeResult += '- Tracking-Id: <type>:<id>, <id> is mandatory\n'
    }
    return analyzeResult
}

main()
