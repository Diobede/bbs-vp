package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Schematic Source Pack
 * 
 * Provides access to .schem and .schematic files from:
 * - .minecraft/config/worldedit/schematics/
 * - .minecraft/schematics/ (Litematica/Axiom)
 */
public class SchematicSourcePack implements ISourcePack
{
    private final List<File> folders = new ArrayList<>();

    public SchematicSourcePack()
    {
        /* Common schematic locations */
        File mc = MinecraftClient.getInstance().runDirectory;
        
        this.addFolder(new File(mc, "config/worldedit/schematics"));
        this.addFolder(new File(mc, "schematics"));
        this.addFolder(new File(mc, "blueprints"));
    }

    private void addFolder(File folder)
    {
        if (folder.exists() && folder.isDirectory())
        {
            this.folders.add(folder);
        }
    }

    @Override
    public String getPrefix()
    {
        return "schematics";
    }

    @Override
    public boolean hasAsset(Link link)
    {
        return this.getFile(link) != null;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        File file = this.getFile(link);

        if (file != null)
        {
            return new FileInputStream(file);
        }

        return null;
    }

    @Override
    public File getFile(Link link)
    {
        if (!link.source.equals("schematics"))
        {
            return null;
        }

        String path = link.path;
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }

        for (File folder : this.folders)
        {
            File file = new File(folder, path);

            if (file.exists())
            {
                return file;
            }
        }

        return null;
    }

    @Override
    public Link getLink(File file)
    {
        for (File folder : this.folders)
        {
            String path = file.getAbsolutePath();
            String folderPath = folder.getAbsolutePath();

            if (path.startsWith(folderPath))
            {
                String relative = path.substring(folderPath.length() + 1).replace(File.separator, "/");
                return new Link("schematics", relative);
            }
        }

        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        if (!link.source.equals("schematics"))
        {
            return;
        }

        for (File folder : this.folders)
        {
            File target = link.path.isEmpty() ? folder : new File(folder, link.path);
            
            if (target.exists() && target.isDirectory())
            {
                this.scanFolder(target, link.path.isEmpty() ? "" : link.path + "/", links, recursive);
            }
        }
    }

    private void scanFolder(File folder, String prefix, Collection<Link> links, boolean recursive)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                if (recursive)
                {
                    this.scanFolder(file, prefix + file.getName() + "/", links, recursive);
                }
            }
            else
            {
                String name = file.getName();
                
                if (name.endsWith(".schem") || name.endsWith(".schematic") || name.endsWith(".nbt"))
                {
                    links.add(new Link("schematics", prefix + name));
                }
            }
        }
    }
}
