package fr.imta.naomod.atl;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transformation {
    public String name;

    public String atlFile;

    public List<NamedFile> libraries = new ArrayList<>();

    public String folderPath;

    public String compiler;

    public String description;

    public Boolean enabled;

    @JsonProperty("input_metamodels")
    public List<NamedFile> inputMetamodels = new ArrayList<>();

    @JsonProperty("output_metamodels")
    public List<NamedFile> outputMetamodels = new ArrayList<>();
}
