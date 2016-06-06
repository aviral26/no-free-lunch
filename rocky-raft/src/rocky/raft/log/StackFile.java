package rocky.raft.log;

import java.io.*;

public class StackFile implements Closeable {

    private static final int INITIAL_LENGTH = 4096; // one file system block

    private static final byte[] ZEROES = new byte[INITIAL_LENGTH];

    static final int HEADER_LENGTH = 16;

    private final RandomAccessFile raf;

    private int fileLength;

    private int elementCount;

    private Element first;

    private Element last;

    private final byte[] buffer = new byte[16];

    public StackFile(File file) throws IOException {
        if (!file.exists()) {
            initialize(file);
        }
        raf = open(file);
        readHeader();
    }

    private static void writeInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }

    private static int readInt(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xff) << 24)
                + ((buffer[offset + 1] & 0xff) << 16)
                + ((buffer[offset + 2] & 0xff) << 8)
                + (buffer[offset + 3] & 0xff);
    }

    private void writeFile(int position, byte[] buffer, int offset, int count) throws IOException {
        raf.seek(position);
        raf.write(buffer, offset, count);
    }

    private void readFile(int position, byte[] buffer, int offset, int count) throws IOException {
        raf.seek(position);
        raf.read(buffer, offset, count);
    }

    private void readHeader() throws IOException {
        raf.seek(0);
        raf.readFully(buffer);
        fileLength = readInt(buffer, 0);
        if (fileLength > raf.length()) {
            throw new IOException(
                    "File is truncated. Expected length: " + fileLength + ", Actual length: " + raf.length());
        } else if (fileLength <= 0) {
            throw new IOException(
                    "File is corrupt; length stored in header (" + fileLength + ") is invalid.");
        }
        elementCount = readInt(buffer, 4);
        int firstOffset = readInt(buffer, 8);
        int lastOffset = readInt(buffer, 12);
        first = readElement(firstOffset);
        last = readElement(lastOffset);
    }

    private void writeHeader(int fileLength, int elementCount, int firstPosition, int lastPosition)
            throws IOException {
        writeInt(buffer, 0, fileLength);
        writeInt(buffer, 4, elementCount);
        writeInt(buffer, 8, firstPosition);
        writeInt(buffer, 12, lastPosition);
        raf.seek(0);
        raf.write(buffer);
    }

    private Element readElement(int position) throws IOException {
        if (position == 0) return Element.NULL;
        readFile(position, buffer, 0, Element.HEADER_LENGTH);
        int length = readInt(buffer, 0);
        int prevLength = readInt(buffer, 4);
        return new Element(position, length, prevLength);
    }

    private static void initialize(File file) throws IOException {
        // Use a temp file so we don't leave a partially-initialized file.
        File tempFile = new File(file.getPath() + ".tmp");
        RandomAccessFile raf = open(tempFile);
        try {
            raf.setLength(INITIAL_LENGTH);
            raf.seek(0);
            byte[] headerBuffer = new byte[16];
            writeInt(headerBuffer, 0, INITIAL_LENGTH);
            raf.write(headerBuffer);
        } finally {
            raf.close();
        }

        // A rename is atomic.
        if (!tempFile.renameTo(file)) {
            throw new IOException("Rename failed!");
        }
    }

    private static RandomAccessFile open(File file) throws FileNotFoundException {
        return new RandomAccessFile(file, "rwd");
    }

    public void push(byte[] data) throws IOException {
        push(data, 0, data.length);
    }

    public synchronized void push(byte[] data, int offset, int count) throws IOException {
        if (data == null) {
            throw new NullPointerException("data == null");
        }
        if ((offset | count) < 0 || count > data.length - offset) {
            throw new IndexOutOfBoundsException();
        }

        expandIfNecessary(count);

        // Insert a new element after the current last element.
        boolean wasEmpty = isEmpty();
        int position = wasEmpty ? HEADER_LENGTH
                : (last.position + Element.HEADER_LENGTH + last.length);
        Element newLast = new Element(position, count, last.length);

        // Write length.
        writeInt(buffer, 0, newLast.length);
        writeInt(buffer, 4, newLast.prevLength);
        writeFile(newLast.position, buffer, 0, Element.HEADER_LENGTH);

        // Write data.
        writeFile(newLast.position + Element.HEADER_LENGTH, data, offset, count);

        // Commit the addition. If wasEmpty, first == last.
        int firstPosition = wasEmpty ? newLast.position : first.position;
        writeHeader(fileLength, elementCount + 1, firstPosition, newLast.position);
        last = newLast;
        elementCount++;
        if (wasEmpty) first = last; // first element
    }

    private int usedBytes() {
        if (elementCount == 0) return HEADER_LENGTH;

        return (last.position - first.position)   // all but last entry
                + Element.HEADER_LENGTH + last.length // last entry
                + HEADER_LENGTH;
    }

    private int remainingBytes() {
        return fileLength - usedBytes();
    }

    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    private void expandIfNecessary(int dataLength) throws IOException {
        int elementLength = Element.HEADER_LENGTH + dataLength;
        int remainingBytes = remainingBytes();
        if (remainingBytes >= elementLength) return;

        // Expand.
        int previousLength = fileLength;
        int newLength;
        // Double the length until we can fit the new data.
        do {
            remainingBytes += previousLength;
            newLength = previousLength << 1;
            previousLength = newLength;
        } while (remainingBytes < elementLength);

        setLength(newLength);

        // Commit the expansion.
        writeHeader(newLength, elementCount, first.position, last.position);

        fileLength = newLength;
    }

    private void setLength(int newLength) throws IOException {
        // Set new file length (considered metadata) and sync it to storage.
        raf.setLength(newLength);
        raf.getChannel().force(true);
    }

    public synchronized byte[] top() throws IOException {
        if (isEmpty()) return null;
        int length = last.length;
        byte[] data = new byte[length];
        readFile(last.position + Element.HEADER_LENGTH, data, 0, length);
        return data;
    }

    public synchronized void top(ElementVisitor visitor) throws IOException {
        if (elementCount > 0) {
            visitor.read(last, new ElementInputStream(last));
        }
    }

    public synchronized int forEach(ElementVisitor visitor) throws IOException {
        int position = first.position;
        for (int i = 0; i < elementCount; i++) {
            Element current = readElement(position);
            boolean shouldContinue = visitor.read(current, new ElementInputStream(current));
            if (!shouldContinue) {
                return i + 1;
            }
            position = current.position + Element.HEADER_LENGTH + current.length;
        }
        return elementCount;
    }

    public synchronized int forEachReverse(ElementVisitor visitor) throws IOException {
        int position = last.position;
        for (int i = 0; i < elementCount; i++) {
            Element current = readElement(position);
            boolean shouldContinue = visitor.read(current, new ElementInputStream(current));
            if (!shouldContinue) {
                return i + 1;
            }
            position = current.position - (Element.HEADER_LENGTH + current.prevLength);
        }
        return elementCount;
    }

    private final class ElementInputStream extends InputStream {
        private int position;
        private int remaining;

        private ElementInputStream(Element element) {
            position = element.position + Element.HEADER_LENGTH;
            remaining = element.length;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if ((offset | length) < 0 || length > buffer.length - offset) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if (remaining == 0) {
                return -1;
            }
            if (length > remaining) length = remaining;
            readFile(position, buffer, offset, length);
            position += length;
            remaining -= length;
            return length;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) return -1;
            raf.seek(position);
            int b = raf.read();
            position++;
            remaining--;
            return b;
        }
    }

    public synchronized int size() {
        return elementCount;
    }

    public void pop() throws IOException {
        pop(1);
    }

    public synchronized void pop(int n) throws IOException {
        if (isEmpty() || n == 0) {
            return;
        }
        if (n < 0) {
            throw new IllegalArgumentException("Cannot remove negative number of elements.");
        }
        if (n == elementCount) {
            clear();
            return;
        }
        if (n > elementCount) {
            throw new IllegalArgumentException("Cannot remove more elements than what are present");
        }

        Element newLast = last;
        for (int i = 0; i < n; i++) {
            int prevPosition = newLast.position - (Element.HEADER_LENGTH + newLast.prevLength);
            newLast = readElement(prevPosition);
        }

        elementCount -= n;

        // Commit the header.
        writeHeader(fileLength, elementCount, first.position, newLast.position);
        last = newLast;
    }

    public synchronized void clear() throws IOException {
        // Commit the header.
        writeHeader(INITIAL_LENGTH, 0, 0, 0);

        // Zero out data.
        raf.seek(HEADER_LENGTH);
        raf.write(ZEROES, 0, INITIAL_LENGTH - HEADER_LENGTH);

        elementCount = 0;
        first = Element.NULL;
        last = Element.NULL;
        if (fileLength > INITIAL_LENGTH) setLength(INITIAL_LENGTH);
        fileLength = INITIAL_LENGTH;
    }

    @Override
    public synchronized void close() throws IOException {
        raf.close();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append('[');
        builder.append("fileLength=").append(fileLength);
        builder.append(", size=").append(elementCount);
        builder.append(", first=").append(first);
        builder.append(", last=").append(last);
        builder.append(", element lengths=[");
        try {
            forEach(new ElementVisitor() {
                boolean first = true;

                @Override
                public boolean read(Element element, InputStream in) throws IOException {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(element);
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.append("]]");
        return builder.toString();
    }

    static class Element {
        static final Element NULL = new Element(0, 0, 0);
        static final int HEADER_LENGTH = 8;
        final int position;
        final int length;
        final int prevLength;

        Element(int position, int length, int prevLength) {
            this.position = position;
            this.length = length;
            this.prevLength = prevLength;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "["
                    + "position = " + position
                    + ", length = " + length
                    + ", prevLength = " + prevLength + "]";
        }
    }

    public interface ElementVisitor {
        boolean read(Element element, InputStream in) throws IOException;
    }
}

