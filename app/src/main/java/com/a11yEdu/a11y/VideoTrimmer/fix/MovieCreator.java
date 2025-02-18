package com.a11yEdu.a11y.VideoTrimmer.fix;

import org.mp4parser.boxes.iso14496.part12.SchemeTypeBox;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.mp4parser.muxer.CencMp4TrackImplImpl;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Mp4TrackImpl;
import org.mp4parser.muxer.PiffMp4TrackImpl;
import org.mp4parser.muxer.RandomAccessSource;
import org.mp4parser.tools.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;

public class MovieCreator {

    public MovieCreator() {
    }

    public static Movie build(String file) throws IOException {
        File f = new File(file);
        FileInputStream fis = new FileInputStream(f);
        Movie m = build(fis.getChannel(), new FileRandomAccessSourceImpl(new RandomAccessFile(f, "r")), file);
        fis.close();
        return m;
    }

    public static Movie build(ReadableByteChannel readableByteChannel, RandomAccessSource randomAccessSource, String name) throws IOException {
        IsoFile isoFile = new IsoFile(readableByteChannel);
        Movie m = new Movie();
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        Iterator var6 = trackBoxes.iterator();

        while (true) {
            while (var6.hasNext()) {
                TrackBox trackBox = (TrackBox) var6.next();
                SchemeTypeBox schm = (SchemeTypeBox) Path.getPath(trackBox, "mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schm[0]");
                if (schm != null && (schm.getSchemeType().equals("cenc") || schm.getSchemeType().equals("cbc1"))) {
                    m.addTrack(new CencMp4TrackImplImpl(trackBox.getTrackHeaderBox().getTrackId(), isoFile, randomAccessSource, name + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]"));
                } else if (schm != null && schm.getSchemeType().equals("piff")) {
                    m.addTrack(new PiffMp4TrackImpl(trackBox.getTrackHeaderBox().getTrackId(), isoFile, randomAccessSource, name + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]"));
                } else {
                    m.addTrack(new Mp4TrackImpl(trackBox.getTrackHeaderBox().getTrackId(), isoFile, randomAccessSource, name + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]"));
                }
            }

            m.setMatrix(isoFile.getMovieBox().getMovieHeaderBox().getMatrix());
            return m;
        }
    }
}
