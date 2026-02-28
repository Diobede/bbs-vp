package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.UIDropdown;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.io.File;

public class UIDebugPanel extends UIDashboardPanel
{
    public UIKeyframes keyframes;
    public UIButton button;
    public UIDropdown dropdown;
    public UIDropdown singleDropdown;

    public UIDebugPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.keyframes = new UIKeyframes(null).duration(() -> 40);
        this.keyframes.full(this);

        for (int i = 0; i < 20; i++)
        {
            KeyframeChannel<Double> channel = new KeyframeChannel<>("baboy", KeyframeFactories.DOUBLE);
            UIKeyframeSheet sheet = new UIKeyframeSheet("baboy_" + i, IKey.raw("Baboy " + i), Colors.HSVtoRGB((float) Math.random(), 1F, 1F).getRGBColor(), false, channel, null);

            channel.insert(0L, 0D);
            channel.insert(20L + (long) (Math.random() * 18 - 9), 0D);
            channel.insert(40L, 1D);

            channel.get(1).setDuration(10);

            this.keyframes.addSheet(sheet);
        }

        this.button = new UIButton(IKey.raw("Hello"), (b) ->
        {
            /* File file = BBSMod.getAssetsPath("textures/skin.png");
            Vector2i vector2i = PNGEncoder.readSize(file);

            System.out.println(vector2i);

            // ---

            File file = BBSMod.getAssetsPath("textures/skin.png");
            File out = BBSMod.getAssetsPath("textures/skin_64x64.png");

            try
            {
                OldSkinImporter.convertSkin(file, out);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // ---

            WaveReader waveReader = new WaveReader();
            File assetsFolder = new File(BBSMod.getAssetsFolder(), "audio/cheese.wav");

            try (FileInputStream stream = new FileInputStream(assetsFolder))
            {
                Wave read = waveReader.read(stream);

                System.out.println(read);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // ---

            File file = new File(BBSMod.getExportFolder(), "abc.dat");
            MapType type = new MapType(false);

            for (int i = 0; i < 256; i++)
            {
                type.putInt("K" + i, 0);
            }

            try
            {
                DataStorage.writeToStream(new FileOutputStream(file), type);
                MapType read = (MapType) DataStorage.readFromStream(new FileInputStream(file));

                System.out.println(read);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            } */

            try
            {
                Pixels newP = Pixels.fromSize(32, 512);

                for (int i = 0; i < 16; i++)
                {
                    Link link = Link.assets("textures/abc/sonic_boom_" + i + ".png");
                    Pixels pixels = Pixels.fromPNGStream(BBSMod.getProvider().getAsset(link));

                    newP.draw(pixels, 0, i * 32);

                    pixels.delete();
                }

                newP.rewindBuffer();

                File file = BBSMod.getProvider().getFile(Link.assets("textures/sonic_boom.png"));

                PNGEncoder.writeToFile(newP, file);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        this.button.relative(this).xy(10, 10).w(80);

        this.dropdown = new UIDropdown(IKey.raw("Multi-Select Options"), (selected) ->
        {
            System.out.println("Selected options: " + selected);
        });
        this.dropdown.addOption("option1", Icons.BLOCK, IKey.raw("Option 1"));
        this.dropdown.addOption("option2", Icons.PARTICLE, IKey.raw("Option 2"));
        this.dropdown.addOption("option3", Icons.SPHERE, IKey.raw("Option 3"));
        this.dropdown.addOption("option4", Icons.WRENCH, IKey.raw("Option 4"));
        this.dropdown.addOption("option5", Icons.BOOKMARK, IKey.raw("Option 5"));
        this.dropdown.relative(this).xy(10, 40).w(250);

        this.singleDropdown = new UIDropdown(IKey.raw("Single Select Options"), (selected) ->
        {
            System.out.println("Single selected: " + selected);
        });
        this.singleDropdown.singleSelect();
        this.singleDropdown.addOption("s1", Icons.BLOCK, IKey.raw("Single 1"));
        this.singleDropdown.addOption("s2", Icons.SPHERE, IKey.raw("Single 2"));
        this.singleDropdown.addOption("s3", Icons.WRENCH, IKey.raw("Single 3"));
        this.singleDropdown.relative(this).xy(270, 40).w(250);

        this.add(this.button);
        this.add(this.dropdown, this.singleDropdown);
        // this.add(this.keyframes);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.keyframes.resetViewX();
    }
}