package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.UIDropdown;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.BBSModClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UICustomFormListOverlayPanel extends UIOverlayPanel
{
    public UITextbox name;
    public UIDropdown icon;
    public UIButton pick;
    public UIElement preview;
    public UIButton done;

    private Consumer<CustomFilterData> callback;
    private Set<String> selectedCategories;

    public UICustomFormListOverlayPanel(Consumer<CustomFilterData> callback)
    {
        super(UIKeys.FORMS_FILTER_CUSTOM_TITLE);

        this.callback = callback;
        this.name = new UITextbox(100, (t) -> {});
        this.name.placeholder(UIKeys.FORMS_FILTER_NAME_PLACEHOLDER);
        
        this.icon = new UIDropdown(UIKeys.FORMS_FILTER_ICON_LABEL, (s) -> {});
        this.icon.singleSelect();
        
        for (Field field : Icons.class.getFields())
        {
            if (Icon.class.isAssignableFrom(field.getType()))
            {
                try
                {
                    Icon icon = (Icon) field.get(null);
                    
                    if (icon != null && Icons.ICONS.containsKey(icon.id))
                    {
                        this.icon.addOption(icon.id, icon, IKey.raw(icon.id));
                    }
                }
                catch (Exception e)
                {}
            }
        }

        this.pick = new UIButton(UIKeys.GENERAL_PICK, (b) -> this.openPickOverlay());
        this.done = new UIButton(UIKeys.GENERAL_DONE, (b) -> this.save());
        
        this.preview = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                // Draw a box for preview area
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50 | 0x000000);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100 | 0x444444);
                
                super.render(context);
                
                if (selectedCategories != null && !selectedCategories.isEmpty())
                {
                    context.batcher.text("Selected: " + selectedCategories.size(), this.area.x + 10, this.area.y + 10, Colors.WHITE);
                    // Could list them here...
                }
                else
                {
                    context.batcher.text("No categories selected", this.area.x + 10, this.area.y + 10, Colors.GRAY);
                }
            }
        };
        
        // Layout
        this.name.relative(this.content).xy(10, 10).w(0.4F, -20).h(20);
        this.icon.relative(this.content).x(0.5F, 10).y(10).w(0.5F, -10).h(20); // Right side
        
        this.pick.relative(this.content).xy(10, 40).w(100).h(20);
        
        this.preview.relative(this.content).xy(10, 70).w(1F, -20).h(1F, -110);
        
        this.done.relative(this.content).x(0.5F).y(1F, -30).w(100).anchor(0.5F, 0F).h(20);
        
        this.add(this.name, this.pick, this.preview, this.done, this.icon);
    }
    
    @Override
    public void resize()
    {
        super.resize();
        
        // Ensure icon dropdown is correctly positioned after resize
        this.icon.relative(this.content).x(0.5F, 10).y(10).w(0.5F, -10).h(20);
    }
    
    private void openPickOverlay()
    {
        UIOverlayPanel panel = new UIOverlayPanel(UIKeys.FORMS_FILTER_SELECT_CATEGORIES);
        
        UIFormList list = new UIFormList(new IUIFormList()
        {
            @Override
            public void toggleEditor()
            {}

            @Override
            public void exit()
            {}

            @Override
            public void accept(mchorse.bbs_mod.forms.forms.Form form)
            {}
        });
        
        list.selectionMode();
        list.setupForms(BBSModClient.getFormCategories());
        
        panel.add(list);
        list.relative(panel.content).x(0).y(0).w(1F).h(1F, -30);
        
        UIButton doneBtn = new UIButton(UIKeys.GENERAL_DONE, (b) ->
        {
            this.selectedCategories = list.selectedCategories;
            panel.close();
        });
        
        doneBtn.relative(panel.content).x(0.5F).y(1F, -5).w(100).anchor(0.5F, 1F).h(20);
        panel.add(doneBtn);
        
        UIOverlay.addOverlay(this.getContext(), panel);
    }
    
    private void save()
    {
        if (this.callback != null)
        {
            CustomFilterData data = new CustomFilterData();
            data.name = this.name.getText();
            Set<String> selectedIcon = this.icon.getSelected();
            if (!selectedIcon.isEmpty())
            {
                data.icon = selectedIcon.iterator().next();
            }
            data.categories = this.selectedCategories != null ? new ArrayList<>(this.selectedCategories) : new ArrayList<>();
            
            this.callback.accept(data);
        }
        this.close();
    }
    
    public static class CustomFilterData 
    {
        public String name;
        public String icon;
        public List<String> categories;
    }
}
