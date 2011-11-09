/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */
package org.knime.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.knime.core.node.KNIMEConstants;

/**
 * This class checks for duplicates in an (almost) arbitrary number of strings.
 * This can be used to check for e.g. unique row keys. The checking is done in
 * two stages: first new keys are added to a set. If the set already contains a
 * key an exception is thrown. If the set gets bigger than the maximum chunk
 * size it is written to disk and the set is cleared. If then after adding all
 * keys {@link #checkForDuplicates()} is called all created chunks are processed
 * and sorted by a merge sort like algorithm. If any duplicate keys are detected
 * during this process an exception is thrown.
 *
 * <p>Note: This implementation is not thread-safe, it's supposed to be used
 * by a single thread only.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class DuplicateChecker {
    /** The default chunk size. */
    public static final int MAX_CHUNK_SIZE = 100000;

    /** The default number of streams open during merging. */
    public static final int MAX_STREAMS = 50;

    private final int m_maxChunkSize;

    private final int m_maxStreams;

    private Set<String> m_chunk = new HashSet<String>();

    private List<File> m_storedChunks = new ArrayList<File>();

    private static final boolean DISABLE_DUPLICATE_CHECK =
        Boolean.getBoolean(
                KNIMEConstants.PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK);

    /** Custom hash set to keep list of to-be-deleted files, see bug 2966:
     * "DuplicateChecker always writes to disc (even for small tables) + temp
     * file names are hashed in core java (increased mem consumption for loops)"
     * for details. */
    private static final Collection<File> TEMP_FILES = new HashSet<File>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                removeTempFiles();
            }
        });
    }

    private static void removeTempFiles() {
        synchronized (TEMP_FILES) {
            for (File f : TEMP_FILES) {
                f.delete();
            }
            TEMP_FILES.clear();
        }
    }

    /**
     * Creates a new duplicate checker with default parameters.
     */
    public DuplicateChecker() {
        this(MAX_CHUNK_SIZE, MAX_STREAMS);
    }

    /**
     * Creates a new duplicate checker.
     *
     * @param maxChunkSize the size of each chunk, i.e. the maximum number of
     *            elements kept in memory
     * @param maxStreams the maximum number of streams that are kept open during
     *            the merge process
     */
    public DuplicateChecker(final int maxChunkSize, final int maxStreams) {
        m_maxChunkSize = maxChunkSize;
        m_maxStreams = maxStreams;
    }

    /**
     * Adds a new key to the duplicate checker.
     *
     * @param s the key
     * @throws DuplicateKeyException if a duplicate within the current chunk has
     *             been detected
     * @throws IOException if an I/O error occurs while writing the chunk to
     *             disk
     */
    public void addKey(final String s) throws DuplicateKeyException,
            IOException {
        if (DISABLE_DUPLICATE_CHECK) {
            return;
        }
        // bug fix #1737: keys may be just wrappers of very large strings ...
        // we make a copy, which consist of the important characters only
        if (!m_chunk.add(new String(s))) {
            throw new DuplicateKeyException(s);
        }
        if (m_chunk.size() >= m_maxChunkSize) {
            writeChunk();
        }
    }

    /**
     * Checks for duplicates in all added keys.
     *
     * @throws DuplicateKeyException if a duplicate key has been detected
     * @throws IOException if an I/O error occurs
     */
    public void checkForDuplicates() throws DuplicateKeyException, IOException {
        if (m_storedChunks.size() == 0) {
            // less than MAX_CHUNK_SIZE keys, no need to write
            // a file because the check for duplicates has already
            // been done in addKey
            return;
        }
        writeChunk();
        checkForDuplicates(m_storedChunks);
    }

    /**
     * Clears the checker, i.e. removes all temporary files and all keys in
     * memory.
     */
    public void clear() {
        for (File f : m_storedChunks) {
            f.delete();
        }
        synchronized (TEMP_FILES) { TEMP_FILES.removeAll(m_storedChunks); }
        m_storedChunks.clear();
        m_chunk.clear();
    }

    /**
     * Checks for duplicates.
     *
     * @param storedChunks the list of chunk files to process
     * @throws NumberFormatException should not happen
     * @throws IOException if an I/O error occurs
     * @throws DuplicateKeyException if a duplicate key has been detected
     */
    private void checkForDuplicates(final List<File> storedChunks)
            throws NumberFormatException, IOException, DuplicateKeyException {
        final int nrChunks =
                (int)Math.ceil(storedChunks.size() / (double)m_maxStreams);
        List<File> newChunks = new ArrayList<File>(nrChunks);

        int chunkCount = 0;
        for (int i = 0; i < nrChunks; i++) {
            BufferedReader[] in =
                new BufferedReader[Math.min(
                        m_maxStreams, storedChunks.size() - chunkCount)];
            if (in.length == 1) {
                // only one (remaining) chunk => no need to merge anything
                newChunks.add(storedChunks.get(chunkCount++));
                break;
            }

            int entries = 0;
            PriorityQueue<Helper> heap = new PriorityQueue<Helper>(in.length);
            for (int j = 0; j < in.length; j++) {
                in[j] = new BufferedReader(new FileReader(
                        storedChunks.get(chunkCount++)));
                int count = Integer.parseInt(in[j].readLine());
                entries += count;

                if (count > 0) {
                    String s = in[j].readLine();
                    heap.add(new Helper(s, j));
                }
            }

            final File f =
                File.createTempFile("KNIME_DuplicateChecker", ".txt");
            synchronized (TEMP_FILES) { TEMP_FILES.add(f); }
            newChunks.add(f);
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(Integer.toString(entries));
            out.newLine();

            String lastKey = null;

            while (entries-- > 0) {
                Helper top = heap.poll();
                if (top.m_s.equals(lastKey)) {
                    out.close();
                    StringBuilder b = new StringBuilder(top.m_s.length());
                    for (int k = 0; k < lastKey.length(); k++) {
                        char c = lastKey.charAt(k);
                        switch (c) {
                        // all sequences starting with '%' are encoded
                        // special characters
                        case '%' :
                            char[] array = new char[2];
                            array[0] = lastKey.charAt(++k);
                            array[1] = lastKey.charAt(++k);
                            int toHex = Integer.parseInt(new String(array), 16);
                            b.append((char)(toHex));
                            break;
                        default :
                            b.append(c);
                        }
                    }
                    throw new DuplicateKeyException(b.toString());
                }
                lastKey = top.m_s;

                if (nrChunks > 1) {
                    out.write(top.m_s);
                    out.newLine();
                }

                String next = in[top.m_streamIndex].readLine();
                if (next != null) {
                    top.m_s = next;
                    heap.add(top);
                }
            }

            out.close();
        }

        if (newChunks.size() > 1) {
            checkForDuplicates(newChunks);
        }
        for (File f : newChunks) {
            f.delete();
        }
        synchronized (TEMP_FILES) { TEMP_FILES.removeAll(newChunks); }
    }

    /**
     * Writes the current chunk to disk and clears the set.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeChunk() throws IOException {
        if (m_chunk.isEmpty()) {
            return;
        }
        String[] sorted = m_chunk.toArray(new String[m_chunk.size()]);
        m_chunk.clear();
        Arrays.sort(sorted);

        File f = File.createTempFile("KNIME_DuplicateChecker", ".txt");
        synchronized (TEMP_FILES) { TEMP_FILES.add(f); }

        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(Integer.toString(sorted.length));
        out.newLine();
        for (String s : sorted) {
            // line breaking characters need to be escaped in order for
            // readLine to work correctly
            StringBuilder buf = new StringBuilder(s.length() + 20);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                case '%':  buf.append("%25"); break;
                case '\n': buf.append("%0A"); break;
                case '\r': buf.append("%0D"); break;
                default: buf.append(c);
                }
            }
            out.write(buf.toString());
            out.newLine();
        }
        out.close();

        m_storedChunks.add(f);
    }

    /**
     * Container to hold a string and the stream index where the string
     * was read from.
     */
    private static final class Helper implements Comparable<Helper> {
        private String m_s;

        private final int m_streamIndex;

        private Helper(final String string, final int streamIdx) {
            m_s = string;
            m_streamIndex = streamIdx;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Helper o) {
            return m_s.compareTo(o.m_s);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_s;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }
}
