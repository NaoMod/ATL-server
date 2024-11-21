package fr.imta.naomod.atl;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transformation {
    public String name;
    public List<String> atlFile;

    public String folderPath;

    public String compiler;

    @JsonProperty("input_metamodels")
    public List<Metamodel> inputMetamodels = new ArrayList<>();

    @JsonProperty("output_metamodels")
    public List<Metamodel> outputMetamodels = new ArrayList<>();
    public String description;
}
