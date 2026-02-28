package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.settings.values.core.ValueList;

public class ValueCustomFilters extends ValueList<ValueCustomFilter>
{
    public ValueCustomFilters(String id)
    {
        super(id);
    }

    @Override
    protected ValueCustomFilter create(String id)
    {
        return new ValueCustomFilter(id);
    }

    @Override
    public void add(ValueCustomFilter value)
    {
        this.preNotify();
        super.add(value);
        this.postNotify();
    }
}
