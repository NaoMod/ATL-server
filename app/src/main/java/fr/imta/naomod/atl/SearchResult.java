package fr.imta.naomod.atl;


class SearchResult {
    public String name;
    public String atlFile;
    public String matchContext;

    public SearchResult(String name, String atlFiles, String matchContext) {
        this.name = name;
        this.atlFile = atlFiles;
        this.matchContext = matchContext;
    }
}
