package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIRecentFormCategory;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.render.DiffuseLighting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import mchorse.bbs_mod.settings.values.ui.ValueCustomFilter;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.forms.UICustomFormListOverlayPanel.CustomFilterData;

public class UIFormList extends UIElement
{
    public enum FilterCategory
    {
        ALL, MODEL, PARTICLE, EXTRA, MOB, CUSTOM
    }

    public IUIFormList palette;

    public UIScrollView forms;

    public UIElement categoryBar;
    public UIIcon modelButton;
    public UIIcon particleButton;
    public UIIcon extraButton;
    public UIIcon mobButton;
    public UIIcon customButton;
    public List<UIIcon> customFilterButtons = new ArrayList<>();
    public Map<UIIcon, CustomFilterData> customFilters = new HashMap<>();
    public CustomFilterData activeCustomFilter;

    public UIElement bar;
    public UITextbox search;
    public UIIcon edit;
    public UIIcon close;

    public boolean selectionMode;
    public Set<String> selectedCategories = new HashSet<>();

    private UIFormCategory recent;
    private List<UIFormCategory> categories = new ArrayList<>();
    private FilterCategory activeFilter = FilterCategory.ALL;

    private long lastUpdate;
    private int lastScroll;

    public UIFormList(IUIFormList palette)
    {
        this.palette = palette;

        this.forms = UI.scrollView(0, 0);
        this.forms.scroll.cancelScrolling();

        /* Category filter buttons */
        this.categoryBar = new UIElement();
        this.modelButton = new UIIcon(Icons.BLOCK, (b) -> this.setFilter(FilterCategory.MODEL));
        this.particleButton = new UIIcon(Icons.PARTICLE, (b) -> this.setFilter(FilterCategory.PARTICLE));
        this.extraButton = new UIIcon(Icons.MORE, (b) -> this.setFilter(FilterCategory.EXTRA));
        this.mobButton = new UIIcon(Icons.CHICKEN, (b) -> this.setFilter(FilterCategory.MOB));
        this.customButton = new UIIcon(Icons.ADD, (b) -> this.openCustomOverlay());

        // Make buttons more compact
        this.modelButton.wh(24, 24);
        this.particleButton.wh(24, 24);
        this.extraButton.wh(24, 24);
        this.mobButton.wh(24, 24);
        this.customButton.wh(24, 24);

        this.modelButton.tooltip(UIKeys.FORMS_FILTER_MODEL, Direction.TOP);
        this.particleButton.tooltip(UIKeys.FORMS_FILTER_PARTICLE, Direction.TOP);
        this.extraButton.tooltip(UIKeys.FORMS_FILTER_EXTRA, Direction.TOP);
        this.mobButton.tooltip(UIKeys.FORMS_FILTER_MOB, Direction.TOP);
        this.customButton.tooltip(UIKeys.FORMS_FILTER_CUSTOM, Direction.TOP);

        // Don't set activeColor - let icons keep their normal color when active
        // The glow effect will provide the visual feedback instead

        this.modelButton.active(this.activeFilter == FilterCategory.MODEL);
        this.particleButton.active(this.activeFilter == FilterCategory.PARTICLE);
        this.extraButton.active(this.activeFilter == FilterCategory.EXTRA);
        this.mobButton.active(this.activeFilter == FilterCategory.MOB);

        this.categoryBar.relative(this).x(0).y(0).w(1F).h(32);

        // Position buttons manually to avoid stretching
        this.modelButton.relative(this.categoryBar).x(10).y(4).wh(24, 24);
        this.particleButton.relative(this.categoryBar).x(38).y(4).wh(24, 24);
        this.extraButton.relative(this.categoryBar).x(66).y(4).wh(24, 24);
        this.mobButton.relative(this.categoryBar).x(94).y(4).wh(24, 24);
        this.customButton.relative(this.categoryBar).x(122).y(4).wh(24, 24);

        this.categoryBar.add(this.modelButton, this.particleButton, this.extraButton, this.mobButton, this.customButton);

        this.bar = new UIElement();
        this.search = new UITextbox(100, this::search).placeholder(UIKeys.FORMS_LIST_SEARCH);
        this.edit = new UIIcon(Icons.EDIT, this::edit);
        this.edit.tooltip(UIKeys.FORMS_LIST_EDIT, Direction.TOP);
        this.close = new UIIcon(Icons.CLOSE, this::close);

        this.forms.relative(this).x(0).y(40).w(1F).h(1F, -70);
        this.bar.relative(this).x(10).y(1F, -30).w(1F, -20).h(20).row().height(20);
        this.close.w(20);

        this.bar.add(this.search, this.edit, this.close);
        this.add(this.forms, this.categoryBar, this.bar);

        this.search.keys().register(Keys.FORMS_FOCUS, this::focusSearch);

        this.markContainer();
        this.setupForms(BBSModClient.getFormCategories());

        for (ValueCustomFilter filterValue : BBSSettings.customFilters.getList())
        {
            CustomFilterData data = new CustomFilterData();
            data.name = filterValue.name.get();
            data.icon = filterValue.icon.get();
            data.categories = new ArrayList<>(filterValue.categories.get());
            
            this.addCustomFilterUI(data);
        }

        this.layoutFilterButtons();
    }

    private void focusSearch()
    {
        this.search.clickItself();
    }

    public UIFormList selectionMode()
    {
        this.selectionMode = true;
        this.categoryBar.removeFromParent();
        this.bar.removeFromParent();
        this.forms.relative(this).x(0).y(0).w(1F).h(1F);

        for (UIFormCategory category : this.categories)
        {
            this.updateCategorySelection(category);
        }

        return this;
    }

    private void updateCategorySelection(UIFormCategory category)
    {
        category.selectionMode = this.selectionMode;
        category.setSelected(this.selectedCategories.contains(category.category.title.get()));
        category.onSelect = (selected) ->
        {
            if (selected)
            {
                this.selectedCategories.add(category.category.title.get());
            }
            else
            {
                this.selectedCategories.remove(category.category.title.get());
            }
        };
    }

    private void addCustomFilterUI(CustomFilterData data)
    {
        UIIcon icon = new UIIcon(Icons.ICONS.get(data.icon), (b) -> this.setCustomFilter(data, (UIIcon) b));
        icon.tooltip(IKey.raw(data.name), Direction.TOP);
        icon.wh(24, 24);
        icon.relative(this.categoryBar).y(4).wh(24, 24);
        
        this.customFilterButtons.add(icon);
        this.customFilters.put(icon, data);
    }

    private void openCustomOverlay()
    {
        UIOverlay.addOverlay(this.getContext(), new UICustomFormListOverlayPanel((data) ->
        {
            ValueCustomFilter value = new ValueCustomFilter("");
            value.name.set(data.name);
            value.icon.set(data.icon);
            value.categories.get().addAll(data.categories);
            
            BBSSettings.customFilters.add(value);
            
            this.addCustomFilterUI(data);
            
            // Re-layout buttons
            this.layoutFilterButtons();
        }));
    }

    private void layoutFilterButtons()
    {
        int startX = 132;
        int x = startX;

        // Move add button to the start of custom section
        this.customButton.relative(this.categoryBar).x(x).y(4).wh(24, 24);
        x += 28;

        for (UIIcon icon : this.customFilterButtons)
        {
            icon.relative(this.categoryBar).x(x).y(4).wh(24, 24);
            
            // Set colors to ensure glow effect matches other icons
            // Assuming inactive is dim and active is bright (or similar to standard icons)
            // But since standard icons use defaults (Inactive=White, Active=LightGray),
            // and user says custom ones DON'T glow like them, maybe custom ones need explicit settings.
            // However, sticking to defaults is safer if we don't know the magic.
            // But let's try to set activeColor explicitly if the user claims it's missing.
            // Actually, let's just make sure they are added to the parent.
            if (icon.getParent() == null)
            {
                this.categoryBar.add(icon);
            }
            x += 28;
        }
        
        // Ensure custom button is rendered correctly relative to others
        // We want custom button to be before custom filters in the list?
        // Actually, z-order doesn't matter much here.
    }

    public void setupForms(FormCategories forms)
    {
        this.categories.clear();
        this.forms.removeAll();

        for (FormCategory category : forms.getAllCategories())
        {
            UIFormCategory uiCategory = category.createUI(this);

            if (this.selectionMode)
            {
                this.updateCategorySelection(uiCategory);
            }

            this.forms.add(uiCategory);
            this.categories.add(uiCategory);

            if (uiCategory instanceof UIRecentFormCategory)
            {
                this.recent = uiCategory;
            }
        }

        this.categories.get(this.categories.size() - 1).marginBottom(40);
        this.resize();

        this.lastUpdate = forms.getLastUpdate();
    }

    private void search(String search)
    {
        search = search.trim();

        for (UIFormCategory category : this.categories)
        {
            if (this.activeFilter != FilterCategory.ALL)
            {
                category.searchWithFilter(search, this::isFormVisible);
            }
            else
            {
                category.search(search);
            }
        }
    }

    private void edit(UIIcon b)
    {
        this.palette.toggleEditor();
    }

    private void close(UIIcon b)
    {
        this.palette.exit();
    }

    private void setFilter(FilterCategory filter)
    {
        if (this.activeFilter == filter)
        {
            this.activeFilter = FilterCategory.ALL;
        }
        else
        {
            this.activeFilter = filter;
        }

        this.updateFilterButtons();
        this.applyFilter();

        // Reset scroll to top when filter changes to avoid blank space
        this.forms.scroll.setScroll(0);
        this.forms.scroll.clamp();
    }

    private void setCustomFilter(CustomFilterData filter, UIIcon button)
    {
        if (this.activeFilter == FilterCategory.CUSTOM && this.activeCustomFilter == filter)
        {
            this.activeFilter = FilterCategory.ALL;
            this.activeCustomFilter = null;
        }
        else
        {
            this.activeFilter = FilterCategory.CUSTOM;
            this.activeCustomFilter = filter;
        }

        this.updateFilterButtons();
        this.applyFilter();

        // Reset scroll to top when filter changes to avoid blank space
        this.forms.scroll.setScroll(0);
        this.forms.scroll.clamp();
    }

    private void updateFilterButtons()
    {
        this.modelButton.active(this.activeFilter == FilterCategory.MODEL);
        this.particleButton.active(this.activeFilter == FilterCategory.PARTICLE);
        this.extraButton.active(this.activeFilter == FilterCategory.EXTRA);
        this.mobButton.active(this.activeFilter == FilterCategory.MOB);

        for (UIIcon icon : this.customFilterButtons)
        {
            icon.active(this.activeFilter == FilterCategory.CUSTOM && this.activeCustomFilter == this.customFilters.get(icon));
        }
    }

    private void applyFilter()
    {
        String currentSearch = this.search.getText();
        List<UIFormCategory> visibleCategories = new ArrayList<>();
        List<UIFormCategory> hiddenCategories = new ArrayList<>();

        // Separate visible and hidden categories
        for (UIFormCategory category : this.categories)
        {
            if (this.activeFilter == FilterCategory.ALL)
            {
                // Show all categories when no filter is active
                category.setVisible(true);
                category.search(currentSearch);
                visibleCategories.add(category);
            }
            else if (this.activeFilter == FilterCategory.CUSTOM)
            {
                if (this.activeCustomFilter != null && this.activeCustomFilter.categories.contains(category.category.title.get()))
                {
                    category.setVisible(true);
                    category.category.visible.set(true);
                    category.search(currentSearch);
                    visibleCategories.add(category);
                }
                else
                {
                    category.setVisible(false);
                    hiddenCategories.add(category);
                }
            }
            else
            {
                // Check if category has any forms matching the filter
                boolean categoryHasMatchingForms = false;

                for (Form form : category.category.getForms())
                {
                    if (this.isFormVisible(form))
                    {
                        categoryHasMatchingForms = true;
                        break;
                    }
                }

                if (categoryHasMatchingForms)
                {
                    // Show and auto-expand categories that have matching forms
                    category.setVisible(true);
                    category.category.visible.set(true);
                    category.searchWithFilter(currentSearch, this::isFormVisible);
                    visibleCategories.add(category);
                }
                else
                {
                    // Hide categories that don't match
                    category.setVisible(false);
                    hiddenCategories.add(category);
                }
            }
        }

        // Reorder categories: visible ones first, then hidden ones
        this.forms.removeAll();

        for (UIFormCategory category : visibleCategories)
        {
            this.forms.add(category);
        }

        for (UIFormCategory category : hiddenCategories)
        {
            this.forms.add(category);
        }

        // Set bottom margin on the last visible category
        if (!visibleCategories.isEmpty())
        {
            visibleCategories.get(visibleCategories.size() - 1).marginBottom(40);
        }

        this.resize();

        // Scroll to top when filter is applied
        if (this.activeFilter != FilterCategory.ALL)
        {
            this.forms.scroll.scrollTo(0);
        }
    }

    private boolean isFormVisible(Form form)
    {
        if (this.activeFilter == FilterCategory.ALL)
        {
            return true;
        }

        FilterCategory formCategory = this.getFormCategory(form);
        return formCategory == this.activeFilter;
    }

    private FilterCategory getFormCategory(Form form)
    {
        if (form instanceof ModelForm)
        {
            return FilterCategory.MODEL;
        }
        else if (form instanceof MobForm)
        {
            return FilterCategory.MOB;
        }
        else if (form instanceof BillboardForm || form instanceof ExtrudedForm || 
                 form instanceof TrailForm)
        {
            return FilterCategory.PARTICLE;
        }
        else if (form instanceof BlockForm || form instanceof ItemForm || 
                 form instanceof LabelForm)
        {
            return FilterCategory.EXTRA;
        }

        return FilterCategory.EXTRA;
    }

    public void selectCategory(UIFormCategory category, Form form, boolean notify)
    {
        this.deselect();

        category.selected = form;

        if (notify)
        {
            this.palette.accept(form);
        }
    }

    public void deselect()
    {
        for (UIFormCategory category : this.categories)
        {
            category.selected = null;
        }
    }

    public UIFormCategory getSelectedCategory()
    {
        for (UIFormCategory category : this.categories)
        {
            if (category.selected != null)
            {
                return category;
            }
        }

        return null;
    }

    public Form getSelected()
    {
        UIFormCategory category = this.getSelectedCategory();

        return category == null ? null : category.selected;
    }

    public void setSelected(Form form)
    {
        boolean found = false;

        this.deselect();

        for (UIFormCategory category : this.categories)
        {
            int index = category.category.getForms().indexOf(form);

            if (index == -1)
            {
                category.selected = null;
            }
            else
            {
                found = true;

                category.select(category.category.getForms().get(index), false);
            }
        }

        if (!found && form != null)
        {
            Form copy = FormUtils.copy(form);

            this.recent.category.addForm(copy);
            this.recent.select(copy, false);
        }

        // Reset scroll to top when search changes to avoid blank space
        this.forms.scroll.setScroll(0);
        this.forms.scroll.clamp();
    }

    @Override
    public void render(UIContext context)
    {
        FormCategories categories = BBSModClient.getFormCategories();

        if (this.lastScroll >= 0)
        {
            this.forms.scroll.scrollTo(this.lastScroll);

            this.lastScroll = -1;
        }

        if (this.lastUpdate != categories.getLastUpdate())
        {
            this.lastScroll = (int) this.forms.scroll.getScroll();

            Form selected = this.getSelected();

            this.setupForms(categories);
            this.setSelected(selected);
        }

        DiffuseLighting.enableGuiDepthLighting();

        // Render background for category filter bar before other elements
        this.categoryBar.area.render(context.batcher, Colors.A50 | 0x000000);

        // Bottom outline only, thicker (1px)
        int outlineColor = BBSSettings.primaryColor.get() | Colors.A100;
        context.batcher.box(this.categoryBar.area.x, this.categoryBar.area.ey() - 1, 
                           this.categoryBar.area.ex(), this.categoryBar.area.ey(), 
                           outlineColor);

        // Separator between standard and custom filters
        int separatorX = this.categoryBar.area.x + 125;
        context.batcher.box(separatorX, this.categoryBar.area.y + 6, separatorX + 2, this.categoryBar.area.ey() - 6, Colors.WHITE);

        // Add glow effect for active filter buttons BEFORE rendering the UI elements
        if (this.activeFilter != FilterCategory.ALL)
        {
            UIIcon activeButton = null;
            switch (this.activeFilter)
            {
                case MODEL: activeButton = this.modelButton; break;
                case PARTICLE: activeButton = this.particleButton; break;
                case EXTRA: activeButton = this.extraButton; break;
                case MOB: activeButton = this.mobButton; break;
                case CUSTOM:
                    for (Map.Entry<UIIcon, CustomFilterData> entry : this.customFilters.entrySet())
                    {
                        if (entry.getValue() == this.activeCustomFilter)
                        {
                            activeButton = entry.getKey();
                            break;
                        }
                    }
                    break;
            }

            if (activeButton != null)
            {
                // Use the same highlight style as UIDashboardPanels
                int color = BBSSettings.primaryColor.get();

                // Bottom highlight bar
                context.batcher.box(activeButton.area.x, activeButton.area.ey() - 2, 
                                   activeButton.area.ex(), activeButton.area.ey(), 
                                   Colors.A100 | color);

                // Gradient background
                context.batcher.gradientVBox(activeButton.area.x, activeButton.area.y, 
                                            activeButton.area.ex(), activeButton.area.ey() - 2, 
                                            color, Colors.A75 | color);
            }
        }

        super.render(context);

        DiffuseLighting.disableGuiDepthLighting();

        /* Render form's display name and ID */
        Form selected = this.getSelected();

        if (selected != null)
        {
            String displayName = selected.getDisplayName();
            String id = selected.getFormId();
            FontRenderer font = context.batcher.getFont();

            int w = Math.max(font.getWidth(displayName), font.getWidth(id));
            int x = this.search.area.x;
            int y = this.search.area.y - 24;

            context.batcher.box(x, y, x + w + 8, this.search.area.y, Colors.A50);
            context.batcher.textShadow(displayName, x + 4, y + 4);
            context.batcher.textShadow(id, x + 4, y + 14, Colors.LIGHTEST_GRAY);
        }
    }
}