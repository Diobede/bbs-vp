package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.misc.ValueStructureLightSettings;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;

/**
 * StructureForm
 *
 * A lightweight form for rendering structures saved in NBT files.
 * Minimizes files by encapsulating only necessary properties.
 */
public class StructureForm extends Form
{
    /** Relative path within assets to the structure file (.nbt), e.g. "structures/house.nbt" */
    public final ValueString structureFile = new ValueString("structure_file", "");
    /** Tint color applied to rendering (multiplied) */
    public final ValueColor color = new ValueColor("color", Color.white());
    /** Selected biome for coloring (override). Empty to use world biome */
    public final ValueString biomeId = new ValueString("biome_id", "");
    /** Toggle if structure light blocks emit light */
    public final ValueBoolean emitLight = new ValueBoolean("emit_light", false);
    /** Light intensity emitted by structure blocks (0-15) */
    public final ValueInt lightIntensity = new ValueInt("light_intensity", 15);
    /** Unified structure light track (enabled + intensity) */
    public final ValueStructureLightSettings structureLight = new ValueStructureLightSettings("structure_light", new StructureLightSettings(false, 15));
    /** Applies global tint also to Block Entities (chests, signs, etc.) */
    public final ValueBoolean tintBlockEntities = new ValueBoolean("tint_block_entities", false);
    /** Manual pivot in block coordinates (allows decimals) */
    public final ValueFloat pivotX = new ValueFloat("pivot_x", 0f);
    public final ValueFloat pivotY = new ValueFloat("pivot_y", 0f);
    public final ValueFloat pivotZ = new ValueFloat("pivot_z", 0f);

    public StructureForm()
    {
        super();

        this.add(this.structureFile);
        this.add(this.color);
        this.add(this.biomeId);
        this.add(this.emitLight);
        this.add(this.lightIntensity);
        /* Hide Block Entities tint from timeline */
        this.tintBlockEntities.invisible();
        this.add(this.tintBlockEntities);
        this.add(this.structureLight);
        /* Hide scalar tracks from timeline; kept for manual UI */
        this.pivotX.invisible();
        this.pivotY.invisible();
        this.pivotZ.invisible();

        this.add(this.pivotX);
        this.add(this.pivotY);
        this.add(this.pivotZ);

        /* New unified keyframe track and hide separate boolean track */
        this.emitLight.invisible();
        this.lightIntensity.invisible();
    }

    @Override
    protected String getDefaultDisplayName()
    {
        String path = this.structureFile.get();

        if (path == null || path.isEmpty())
        {
            return super.getDefaultDisplayName();
        }

        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String prefix = slash >= 0 ? path.substring(0, slash + 1) : "";
        String name = slash >= 0 ? path.substring(slash + 1) : path;

        String base = name.toLowerCase().endsWith(".nbt") ? name.substring(0, name.length() - 4) : name;

        return prefix + base;
    }

    @Override
    public String getTrackName(String property)
    {
        int slash = property.lastIndexOf('/');
        String prefix = slash == -1 ? "" : property.substring(0, slash + 1);
        String last = slash == -1 ? property : property.substring(slash + 1);

        String mapped = last;
        if ("structure_file".equals(last)) mapped = "structure";
        else if ("biome_id".equals(last)) mapped = "biome";
        /* Show visual name as 'structure_light' instead of 'light' */
        else if ("structure_light".equals(last)) mapped = "structure_light";

        return super.getTrackName(prefix + mapped);
    }
}
