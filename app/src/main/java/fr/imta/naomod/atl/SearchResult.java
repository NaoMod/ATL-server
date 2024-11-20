package fr.imta.naomod.atl;

import java.util.List;

class SearchResult {
    public String name;
    public List<String> atlFile;
    public String matchContext;

    public SearchResult(String name, List<String> atlFiles, String matchContext) {
        this.name = name;
        this.atlFile = atlFiles;
        this.matchContext = matchContext;
    }
}
