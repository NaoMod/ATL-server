package fr.imta.naomod.atl;

import java.util.Map;
import java.util.HashMap;

public class Transformation {
    public int id;
    public String name;
    public String atlFile;
    public Map<String, String> SourceMetamodels = new HashMap<>();
    public Map<String, String> TargetMetamodels = new HashMap<>();
    
}
