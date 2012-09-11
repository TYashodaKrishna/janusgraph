package com.thinkaurelius.faunus.formats.rexster;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Gets vertices from Rexster via the Gremlin Extension and a custom Gremlin script that iterates
 * vertices and converts them to the Faunus GraphSON that is understood by the Faunus GraphSONUtility.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterRecordReader extends RecordReader<NullWritable, FaunusVertex> {

    private final RexsterConfiguration rexsterConf;

    private final NullWritable key = NullWritable.get();

    /**
     * The current vertex in the reader.
     */
    private FaunusVertex value = null;

    private DataInputStream rexsterInputStream;

    private long splitStart;
    private long splitEnd;

    private long itemsIterated = 0;

    private boolean pathEnabled;

    private FaunusVertex vertex = new FaunusVertex();
    
    public RexsterRecordReader(final RexsterConfiguration conf) {
        this.rexsterConf = conf;
    }

    @Override
    public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        final RexsterInputSplit rexsterInputSplit = (RexsterInputSplit) inputSplit;
        this.splitEnd = rexsterInputSplit.getEnd();
        this.splitStart = rexsterInputSplit.getStart();
        this.pathEnabled = taskAttemptContext.getConfiguration().getBoolean(FaunusCompiler.PATH_ENABLED, false);
        this.openRexsterStream();
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        boolean isNext;

        try {
            this.vertex.readFields(this.rexsterInputStream);
            this.value.enablePath(this.pathEnabled);
            itemsIterated++;
            isNext = true;
        } catch (Exception e) {
            isNext = false;
        }

        return isNext;
    }

    @Override
    public NullWritable getCurrentKey() throws IOException, InterruptedException {
        return this.key;
    }

    @Override
    public FaunusVertex getCurrentValue() throws IOException, InterruptedException {
        return this.value;
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        if (this.splitStart == this.splitEnd) {
            return 0.0f;
        } else {
            // assuming you got the estimate right this progress should be pretty close;
            return Math.min(1.0f, this.itemsIterated / (float) this.rexsterConf.getEstimatedVertexCount());
        }
    }

    @Override
    public void close() throws IOException {
        this.rexsterInputStream.close();
    }

    private void openRexsterStream() {
        try {
            final String uri = String.format("%s?rexster.offset.start=%s&rexster.offset.end=%s",
                    this.rexsterConf.getRestStreamEndpoint(), this.splitStart, this.splitEnd);
            final URL url = new URL(uri);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(0);
            connection.setReadTimeout(0);
            connection.setRequestMethod("GET");

            // worry about rexster auth later
            // connection.setRequestProperty(RexsterTokens.AUTHORIZATION, Authentication.getAuthenticationHeaderValue());

            connection.setDoOutput(true);

            this.rexsterInputStream = new DataInputStream(connection.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
