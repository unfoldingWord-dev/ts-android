package com.door43.util;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by joel on 2/24/2015.
 */
public class Tar {
    /**
     * Generates a zipped archive of the project
     * @param sourcePath the directory to archive
     * @return the path to the project archive
     */
    public static void tar(String sourcePath, String destPath) throws IOException {
        // build dest
        FileOutputStream dest = new FileOutputStream(destPath);
        TarOutputStream out = new TarOutputStream( new BufferedOutputStream( dest ) );
        tarFolder(null, sourcePath, out);
        out.close();
    }

    private static void tarFolder(String parent, String path, TarOutputStream out) throws IOException {
        BufferedInputStream origin;
        File f = new File(path);
        String files[] = f.list();

        // is file
        if (files == null) {
            files = new String[1];
            files[0] = f.getName();
        }

        parent = ((parent == null) ? (f.isFile()) ? "" : f.getName() + "/" : parent + f.getName() + "/");

        for (int i = 0; i < files.length; i++) {
//            System.out.println("Adding: " + files[i]);
            File fe = f;
            byte data[] = new byte[2048];

            if (f.isDirectory()) {
                fe = new File(f, files[i]);
            }

            if (fe.isDirectory()) {
                String[] fl = fe.list();
                if (fl != null && fl.length != 0) {
                    tarFolder(parent, fe.getPath(), out);
                } else {
                    TarEntry entry = new TarEntry(fe, parent + files[i] + "/");
                    out.putNextEntry(entry);
                }
                continue;
            }

            FileInputStream fi = new FileInputStream(fe);
            origin = new BufferedInputStream(fi);
            TarEntry entry = new TarEntry(fe, parent + files[i]);
            out.putNextEntry(entry);

            int count;

            while ((count = origin.read(data)) != -1) {
                out.write(data, 0, count);
            }

            out.flush();

            origin.close();
        }
    }

    /**
     * Extracts a tar file
     * @param tarPath
     * @throws IOException
     */
    public static void untarTarFile(String tarPath, String destPath) throws IOException {
        File destFolder = new File(destPath);
        destFolder.mkdirs();

        File zf = new File(tarPath);

        TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(zf)));
        untar(tis, destFolder.getAbsolutePath());

        tis.close();

    }

    /**
     * Extracts a tar
     * @param tis
     * @param destFolder
     * @throws IOException
     */
    private static void untar(TarInputStream tis, String destFolder) throws IOException {
        BufferedOutputStream dest = null;

        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
//            System.out.println("Extracting: " + entry.getName());
            int count;
            byte data[] = new byte[2048];

            if (entry.isDirectory()) {
                new File(destFolder + "/" + entry.getName()).mkdirs();
                continue;
            } else {
                int di = entry.getName().lastIndexOf('/');
                if (di != -1) {
                    new File(destFolder + "/" + entry.getName().substring(0, di)).mkdirs();
                }
            }

            FileOutputStream fos = new FileOutputStream(destFolder + "/" + entry.getName());
            dest = new BufferedOutputStream(fos);

            while ((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }
    }
}
