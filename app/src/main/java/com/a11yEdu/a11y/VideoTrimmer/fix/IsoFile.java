package com.a11yEdu.a11y.VideoTrimmer.fix;

import org.mp4parser.BasicContainer;
import org.mp4parser.Box;
import org.mp4parser.BoxParser;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.support.DoNotParseDetail;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@DoNotParseDetail
public class IsoFile extends BasicContainer implements Closeable {
    private final ReadableByteChannel readableByteChannel;
    private FileInputStream fis;

    public IsoFile(String file) throws IOException {
        this(new File(file));
    }

    public IsoFile(File file) throws IOException {
        this.fis = new FileInputStream(file);
        this.readableByteChannel = this.fis.getChannel();
        this.initContainer(this.readableByteChannel, -1L, new PropertyBoxParserImpl(new String[0]));
    }

    public IsoFile(ReadableByteChannel readableByteChannel) throws IOException {
        this(readableByteChannel, new PropertyBoxParserImpl(new String[0]));
    }

    public IsoFile(ReadableByteChannel readableByteChannel, BoxParser boxParser) throws IOException {
        this.readableByteChannel = readableByteChannel;
        this.initContainer(readableByteChannel, -1L, boxParser);
    }

    public static byte[] fourCCtoBytes(String fourCC) {
        byte[] result = new byte[4];
        if (fourCC != null) {
            for (int i = 0; i < Math.min(4, fourCC.length()); ++i) {
                result[i] = (byte) fourCC.charAt(i);
            }
        }

        return result;
    }

    public static String bytesToFourCC(byte[] type) {
        byte[] result = new byte[4];
        if (type != null) {
            System.arraycopy(type, 0, result, 0, Math.min(type.length, 4));
        }

        return new String(result, StandardCharsets.ISO_8859_1);
    }

    public long getSize() {
        return this.getContainerSize();
    }

    public MovieBox getMovieBox() {
        Iterator var2 = this.getBoxes().iterator();

        while (var2.hasNext()) {
            Box box = (Box) var2.next();
            if (box instanceof MovieBox) {
                return (MovieBox) box;
            }
        }

        return null;
    }

    public void getBox(WritableByteChannel os) throws IOException {
        this.writeContainer(os);
    }

    public void close() throws IOException {
        this.readableByteChannel.close();
        if (this.fis != null) {
            this.fis.close();
        }

        Iterator var2 = this.getBoxes().iterator();

        while (var2.hasNext()) {
            Box box = (Box) var2.next();
            if (box instanceof Closeable) {
                ((Closeable) box).close();
            }
        }

    }

    public String toString() {
        return "model(" + this.readableByteChannel + ")";
    }
}