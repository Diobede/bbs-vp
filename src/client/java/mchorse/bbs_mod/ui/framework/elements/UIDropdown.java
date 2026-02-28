package mchorse.bbs_mod.ui.framework.elements;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
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
public class UIDropdown extends UIElement
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
    private boolean singleSelect = false;

    private Scroll scroll;
    private int maxVisibleOptions = 5;

    public static final int HEADER_HEIGHT = 20;
    public static final int OPTION_HEIGHT = 20;

    public UIDropdown(IKey title, Consumer<Set<String>> callback)
    {
        super();

        this.title = title;
        this.callback = callback;
        this.h(HEADER_HEIGHT);

        this.scroll = new Scroll(new Area());
        this.scroll.scrollItemSize = OPTION_HEIGHT;
        this.scroll.scrollSpeed = OPTION_HEIGHT;
    }

    public UIDropdown maxVisibleOptions(int max)
    {
        this.maxVisibleOptions = max;
        return this;
    }

    public UIDropdown addOption(String id, Icon icon, IKey label)
    {
        this.options.add(new Option(id, icon, label));
        this.updateHeight();

        return this;
    }

    public UIDropdown singleSelect()
    {
        this.singleSelect = true;
        
        return this;
    }

    public UIDropdown setExpanded(boolean expanded)
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

    public UIDropdown setSelected(Set<String> selected)
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
            int optionsHeight = this.options.size() * OPTION_HEIGHT;
            int maxOptionsHeight = this.maxVisibleOptions * OPTION_HEIGHT;
            
            height += Math.min(optionsHeight, maxOptionsHeight);
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
            this.scroll.scrollTo(0); // Reset scroll on toggle

            return true;
        }

        // Click on option
        if (this.expanded)
        {
            if (this.scroll.mouseClicked(context))
            {
                return true;
            }

            int scrollY = (int) this.scroll.getScroll();
            int optionIndex = (relativeY - HEADER_HEIGHT + scrollY) / OPTION_HEIGHT;

            if (optionIndex >= 0 && optionIndex < this.options.size())
            {
                // Check if click is within the visible area (excluding scrollbar if needed, but simple check is enough for now)
                int maxVisibleHeight = this.maxVisibleOptions * OPTION_HEIGHT;
                if (relativeY - HEADER_HEIGHT < maxVisibleHeight)
                {
                    Option option = this.options.get(optionIndex);

                    if (this.singleSelect)
                    {
                        this.selected.clear();
                        this.selected.add(option.id);
                        this.expanded = false;
                        this.updateHeight();
                    }
                    else
                    {
                        if (this.selected.contains(option.id))
                        {
                            this.selected.remove(option.id);
                        }
                        else
                        {
                            this.selected.add(option.id);
                        }
                    }

                    if (this.callback != null)
                    {
                        this.callback.accept(this.getSelected());
                    }

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }
    
    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (this.expanded && this.scroll.mouseScroll(context))
        {
            return true;
        }
        
        return super.subMouseScrolled(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.scroll.mouseReleased(context);
        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();

        // Update scroll bounds
        int optionsHeight = this.options.size() * OPTION_HEIGHT;
        int maxOptionsHeight = this.maxVisibleOptions * OPTION_HEIGHT;
        int visibleHeight = Math.min(optionsHeight, maxOptionsHeight);
        
        this.scroll.area.copy(this.area);
        this.scroll.area.y += HEADER_HEIGHT;
        this.scroll.area.h = visibleHeight;
        this.scroll.setSize(this.options.size());
        this.scroll.clamp();
        this.scroll.drag(context);

        // Render header (Textbox style)
        int h = HEADER_HEIGHT;
        int x = this.area.x;
        int y = this.area.y;
        int w = this.area.w;

        // Background
        context.batcher.box(x, y, x + w, y + h, 0xff000000);

        // Border
        int borderColor = this.expanded ? (0xff000000 + BBSSettings.primaryColor.get()) : 0xffaaaaaa;
        context.batcher.outline(x, y, x + w, y + h, borderColor);

        // Render expand/collapse icon
        Icon arrowIcon = this.expanded ? Icons.MOVE_DOWN : Icons.MOVE_UP;
        int iconX = x + w - 16;
        context.batcher.icon(arrowIcon, iconX, y + 6, 0F, 0F);

        // Render title
        String titleText = this.title.get();
        Icon selectedIcon = null;

        if (this.singleSelect && !this.selected.isEmpty())
        {
            String id = this.selected.iterator().next();

            for (Option option : this.options)
            {
                if (option.id.equals(id))
                {
                    titleText = option.label.get();
                    selectedIcon = option.icon;
                    break;
                }
            }
        }

        int textX = x + 4;

        if (selectedIcon != null)
        {
            context.batcher.icon(selectedIcon, x + 4, y + 2, 0F, 0F);
            textX += 20;
        }

        int textY = y + (h - font.getHeight()) / 2;
        context.batcher.text(titleText, textX, textY, Colors.WHITE);

        // Render options if expanded
        if (this.expanded)
        {
            // Background for options
            context.batcher.box(x, y + h, x + w, y + h + visibleHeight, 0xff000000);

            // Border for options
            context.batcher.outline(x, y + h, x + w, y + h + visibleHeight, borderColor);
            
            // Clip options area
            context.batcher.clip(this.scroll.area, context);
            
            int scrollY = (int) this.scroll.getScroll();

            for (int i = 0; i < this.options.size(); i++)
            {
                Option option = this.options.get(i);
                int optionY = y + h + i * OPTION_HEIGHT - scrollY;
                
                // Optimization: Skip rendering if out of view
                if (optionY + OPTION_HEIGHT < y + h || optionY > y + h + visibleHeight)
                {
                    continue;
                }
                
                boolean isSelected = this.selected.contains(option.id);
                boolean isHovered = this.area.isInside(context) && context.mouseY >= optionY && context.mouseY < optionY + OPTION_HEIGHT;
                
                // Adjust hover check for scrolling area
                if (context.mouseY < y + h || context.mouseY >= y + h + visibleHeight)
                {
                    isHovered = false;
                }

                // Render option background
                int bgColor = 0;

                if (isSelected)
                {
                    bgColor = Colors.A50 | BBSSettings.primaryColor.get();
                }

                if (isHovered)
                {
                    bgColor = Colors.A25 | 0xffffff;
                }

                if (bgColor != 0)
                {
                    context.batcher.box(x, optionY, x + w, optionY + OPTION_HEIGHT, bgColor);
                }

                // Render selection indicator (checkmark or box)
                if (isSelected)
                {
                    context.batcher.icon(Icons.CHECKMARK_OUTLINE, x + 6, optionY + 2, 0F, 0F);
                }
                else
                {
                    // Render empty checkbox outline
                    context.batcher.icon(Icons.OUTLINE, x + 6, optionY + 2, 0F, 0F);
                }

                // Render option icon
                if (option.icon != null)
                {
                    context.batcher.icon(option.icon, x + 28, optionY + 2, 0F, 0F);
                }

                // Render option label
                String label = option.label.get();
                context.batcher.text(label, x + 48, optionY + 6, Colors.WHITE, true);
            }
            
            context.batcher.unclip(context);
            
            this.scroll.renderScrollbar(context.batcher);
        }

        super.render(context);
    }
}
