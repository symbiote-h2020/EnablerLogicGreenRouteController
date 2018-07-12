package eu.h2020.symbiote.smeur.elgrc.commons;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class Utils {

    public static File zipCompress(File zipMe) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Test String");

        File f = new File(zipMe.getName().concat(".zip"));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(zipMe.getAbsolutePath());
        out.putNextEntry(e);

        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();

        out.close();

        return f;

    }

    public static File targzCompress(File zipMe) throws IOException {
        File outFile = new File(zipMe.getName().replace(".json","") + ".tar.gz");
        List<File> list = new ArrayList<>();
        list.add(zipMe);
        compressFiles(list, outFile);

        return outFile;
    }

    private static void compressFiles(List<File> list, File outFile) throws IOException {
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile))))) {

            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            for (File f : list) {
                System.out.println("send to cmpress: " + f.getName());
                addFileToCompression(taos, f);
            }
        } // all streams are automatically closed here, whether an exception occurs or not
    }

    private static void addFileToCompression(TarArchiveOutputStream taos, File f) throws IOException {
        TarArchiveEntry tae = new TarArchiveEntry(f);
        taos.putArchiveEntry(tae);
        if (!f.isDirectory()) {
            System.out.println("is a file " + f.getName());
            try (FileInputStream fis = new FileInputStream(f)) {
                IOUtils.copy(fis, taos);
            }
            System.out.println("file added");
            taos.flush();
            taos.closeArchiveEntry();
        }
    }


}
