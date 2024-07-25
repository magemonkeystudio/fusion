package studio.magemonkey.fusion.cfg.editors.subeditors;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.util.ChatUT;

public class RecipeEditorCfg {

    private final YamlParser config;

    public RecipeEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "/Editors/SubEditors/RecipeEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Pattern Item Editor"));
    }
}
