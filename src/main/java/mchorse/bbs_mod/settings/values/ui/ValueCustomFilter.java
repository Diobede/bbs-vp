package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;

public class ValueCustomFilter extends ValueGroup
{
    public final ValueString name = new ValueString("name", "");
    public final ValueString icon = new ValueString("icon", "");
    public final ValueStringKeys categories = new ValueStringKeys("categories");

    public ValueCustomFilter(String id)
    {
        super(id);

        this.add(this.name);
        this.add(this.icon);
        this.add(this.categories);
    }
}
