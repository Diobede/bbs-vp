package mchorse.bbs_mod.ui.framework.elements;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Multi-select dropdown UI element
 * 
 * Allows users to select multiple options from a collapsible list.
 * Each option has an icon and a label.
 */
public class UIMultiSelectDropdown extends UIElement
{
    public static class Option
    {
        public Icon icon;
        public IKey label;
        public String id;

        public Option(String id, Icon icon, IKey label)
        {
            this.id = id;
            this.icon = icon;
            this.label = label;
        }
    }

    private IKey title;
    private List<Option> options = new ArrayList<>();
    private Set<String> selected = new HashSet<>();
    private boolean expanded = false;
    private Consumer<Set<String>> callback;

    public static final int HEADER_HEIGHT = 20;
    public static final int OPTION_HEIGHT = 20;

    public UIMultiSelectDropdown(IKey title, Consumer<Set<String>> callback)
    {
        super();

        this.title = title;
        this.callback = callback;
        this.h(HEADER_HEIGHT);
    }

    public UIMultiSelectDropdown addOption(String id, Icon icon, IKey label)
    {
        this.options.add(new Option(id, icon, label));
        this.updateHeight();

        return this;
    }

    public UIMultiSelectDropdown setExpanded(boolean expanded)
    {
        this.expanded = expanded;
        this.updateHeight();

        return this;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public Set<String> getSelected()
    {
        return new HashSet<>(this.selected);
    }

    public UIMultiSelectDropdown setSelected(Set<String> selected)
    {
        this.selected.clear();
        this.selected.addAll(selected);

        return this;
    }

    public boolean isSelected(String id)
    {
        return this.selected.contains(id);
    }

    private void updateHeight()
    {
        int height = HEADER_HEIGHT;

        if (this.expanded)
        {
            height += this.options.size() * OPTION_HEIGHT;
        }

        this.h(height);

        UIElement parent = this.getParentContainer();

        if (parent != null)
        {
            parent.resize();
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return super.subMouseClicked(context);
        }

        int relativeY = context.mouseY - this.area.y;

        // Click on header
        if (relativeY < HEADER_HEIGHT)
        {
            this.expanded = !this.expanded;
            this.updateHeight();

            return true;
        }

        // Click on option
        if (this.expanded)
        {
            int optionIndex = (relativeY - HEADER_HEIGHT) / OPTION_HEIGHT;

            if (optionIndex >= 0 && optionIndex < this.options.size())
            {
                Option option = this.options.get(optionIndex);

                if (this.selected.contains(option.id))
                {
                    this.selected.remove(option.id);
                }
                else
                {
                    this.selected.add(option.id);
                }

                if (this.callback != null)
                {
                    this.callback.accept(this.getSelected());
                }

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        FontRenderer font = context.batcher.getFont();

        // Render header background
        int headerColor = Colors.A100 | 0x444444;

        if (this.area.isInside(context) && context.mouseY - this.area.y < HEADER_HEIGHT)
        {
            headerColor = Colors.mulRGB(headerColor, 0.85F);
        }

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + HEADER_HEIGHT, headerColor);

        // Render expand/collapse icon
        Icon arrowIcon = this.expanded ? Icons.MOVE_DOWN : Icons.MOVE_UP;
        context.batcher.icon(arrowIcon, this.area.x + 10, this.area.y + 5, 0.5F, 0F);

        // Render title
        context.batcher.text(this.title.get(), this.area.x + 26, this.area.y + 6, Colors.WHITE, true);

        // Render options if expanded
        if (this.expanded)
        {
            for (int i = 0; i < this.options.size(); i++)
            {
                Option option = this.options.get(i);
                int y = this.area.y + HEADER_HEIGHT + i * OPTION_HEIGHT;
                boolean isSelected = this.selected.contains(option.id);
                boolean isHovered = this.area.isInside(context) && context.mouseY >= y && context.mouseY < y + OPTION_HEIGHT;

                // Render option background
                int bgColor = isSelected ? (Colors.A50 | BBSSettings.primaryColor.get()) : (Colors.A100 | 0x333333);

                if (isHovered)
                {
                    bgColor = Colors.mulRGB(bgColor, 0.85F);
                }

                context.batcher.box(this.area.x, y, this.area.ex(), y + OPTION_HEIGHT, bgColor);

                // Render selection indicator (checkmark or box)
                if (isSelected)
                {
                    context.batcher.icon(Icons.VISIBLE, this.area.x + 6, y + 2, 0F, 0F);
                }
                else
                {
                    // Render empty checkbox outline
                    int checkX = this.area.x + 6;
                    int checkY = y + 2;
                    context.batcher.outline(checkX, checkY, checkX + 16, checkY + 16, Colors.A100, 1);
                }

                // Render option icon
                if (option.icon != null)
                {
                    context.batcher.icon(option.icon, this.area.x + 28, y + 2, 0F, 0F);
                }

                // Render option label
                String label = option.label.get();
                context.batcher.text(label, this.area.x + 48, y + 6, Colors.WHITE, true);
            }
        }
    }
}
