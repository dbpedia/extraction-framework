package org.dbpedia.util;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class AsyncFileWriter implements Runnable, Closeable {
    private final Writer out;
    private final BlockingQueue<Item> queue = new LinkedBlockingQueue<Item>();
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    public AsyncFileWriter(OutputStream out) throws IOException {
        this.out = new BufferedWriter(new OutputStreamWriter(out));
    }

    public AsyncFileWriter append(CharSequence seq) {
        if (!started) {
            throw new IllegalStateException("open() call expected before append()");
        }
        try {
            queue.put(new CharSeqItem(seq));
        } catch (InterruptedException ignored) {
        }
        return this;
    }

    public void open() {
        this.started = true;
        new Thread(this).start();
    }

    public void run() {
        while (!stopped || queue.size() > 0) {
            try {
                Item item = queue.poll(100, TimeUnit.MICROSECONDS);
                if (item != null) {
                    try {
                        item.write(out);
                    } catch (IOException logme) {
                        System.err.println("IOException: " + logme.getMessage());
                    }
                }
            } catch (InterruptedException e) {
            }
        }
        try {
            out.close();
        } catch (IOException ignore) {
        }
    }

    public void close() {
        this.stopped = true;
    }

    private static interface Item {
        void write(Writer out) throws IOException;
    }

    private static class CharSeqItem implements Item {
        private final CharSequence sequence;

        public CharSeqItem(CharSequence sequence) {
            this.sequence = sequence;
        }

        public void write(Writer out) throws IOException {
            out.append(sequence);
        }
    }

    private static class IndentItem implements Item {
        private final int indent;

        public IndentItem(int indent) {
            this.indent = indent;
        }

        public void write(Writer out) throws IOException {
            for (int i = 0; i < indent; i++) {
                out.append(" ");
            }
        }
    }
}