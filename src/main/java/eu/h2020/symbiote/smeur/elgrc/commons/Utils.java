package eu.h2020.symbiote.smeur.elgrc.commons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

}
