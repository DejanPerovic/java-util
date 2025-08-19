package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastReaderTest {

    private FastReader fastReader;
    private static final int CUSTOM_BUFFER_SIZE = 16;
    private static final int CUSTOM_PUSHBACK_SIZE = 4;

    @AfterEach
    void tearDown() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    // Constructor Tests
    @Test
    void testConstructorWithDefaultSizes() {
        fastReader = new FastReader(new StringReader("test"));
        assertNotNull(fastReader);
    }

    @Test
    void testConstructorWithCustomSizes() {
        fastReader = new FastReader(new StringReader("test"), CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);
        assertNotNull(fastReader);
    }

    @Test
    void testConstructorWithInvalidBufferSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new FastReader(new StringReader("test"), 0, CUSTOM_PUSHBACK_SIZE));
    }

    @Test
    void testConstructorWithNegativeBufferSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new FastReader(new StringReader("test"), -10, CUSTOM_PUSHBACK_SIZE));
    }

    @Test
    void testConstructorWithZeroPushbackSize() {
        // This should NOT throw an exception, since pushbackBufferSize=0 is allowed
        FastReader reader = new FastReader(new StringReader("test"), CUSTOM_BUFFER_SIZE, 0);
        assertNotNull(reader);
    }

    @Test
    void testConstructorWithNegativePushbackSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new FastReader(new StringReader("test"), CUSTOM_BUFFER_SIZE, -5));
    }

    // Basic read() Tests
    @Test
    void testReadSingleChar() throws IOException {
        fastReader = new FastReader(new StringReader("a"));
        assertEquals('a', fastReader.read());
    }

    @Test
    void testReadMultipleChars() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());
    }

    @Test
    void testReadEndOfStream() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        assertEquals(-1, fastReader.read());
    }

    @Test
    void testReadEndOfStreamAfterContent() throws IOException {
        fastReader = new FastReader(new StringReader("a"));
        assertEquals('a', fastReader.read());
        assertEquals(-1, fastReader.read());
    }

    @Test
    void testReadFromClosedReader() throws IOException {
        fastReader = new FastReader(new StringReader("test"));
        fastReader.close();
        assertThrows(IOException.class, () -> fastReader.read());
    }

    // Pushback Tests
    @Test
    void testPushbackAndRead() throws IOException {
        fastReader = new FastReader(new StringReader("bc"));
        fastReader.pushback('a');
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());
    }

    @Test
    void testPushbackMultipleCharsAndRead() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        fastReader.pushback('c');
        fastReader.pushback('b');
        fastReader.pushback('a');
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());
    }

    @Test
    void testPushbackLinefeed() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        fastReader.pushback('\n');
        assertEquals('\n', fastReader.read());
    }

    @Test
    void testPushbackBufferOverflow() throws IOException {
        fastReader = new FastReader(new StringReader(""), CUSTOM_BUFFER_SIZE, 3);
        fastReader.pushback('a');
        fastReader.pushback('b');
        fastReader.pushback('c');
        // This should overflow the pushback buffer of size 3
        assertThrows(IOException.class, () -> fastReader.pushback('d'));
    }

    // Array Read Tests
    @Test
    void testReadIntoCharArray() throws IOException {
        fastReader = new FastReader(new StringReader("abcdef"));
        char[] buffer = new char[4];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(4, read);
        assertEquals('a', buffer[0]);
        assertEquals('b', buffer[1]);
        assertEquals('c', buffer[2]);
        assertEquals('d', buffer[3]);
    }

    @Test
    void testReadIntoCharArrayWithOffset() throws IOException {
        fastReader = new FastReader(new StringReader("abcdef"));
        char[] buffer = new char[6];
        int read = fastReader.read(buffer, 2, 3);
        assertEquals(3, read);
        assertEquals(0, buffer[0]); // Not written
        assertEquals(0, buffer[1]); // Not written
        assertEquals('a', buffer[2]);
        assertEquals('b', buffer[3]);
        assertEquals('c', buffer[4]);
        assertEquals(0, buffer[5]); // Not written
    }

    @Test
    void testReadIntoCharArrayFromPushback() throws IOException {
        fastReader = new FastReader(new StringReader("def"));
        // Push back a few characters
        fastReader.pushback('c');
        fastReader.pushback('b');
        fastReader.pushback('a');

        char[] buffer = new char[6];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(6, read);
        assertEquals('a', buffer[0]);
        assertEquals('b', buffer[1]);
        assertEquals('c', buffer[2]);
        assertEquals('d', buffer[3]);
        assertEquals('e', buffer[4]);
        assertEquals('f', buffer[5]);
    }

    @Test
    void testReadIntoCharArrayFromClosedReader() throws IOException {
        fastReader = new FastReader(new StringReader("test"));
        fastReader.close();
        char[] buffer = new char[4];
        assertThrows(IOException.class, () -> fastReader.read(buffer, 0, buffer.length));
    }

    @Test
    void testReadIntoCharArrayPartialRead() throws IOException {
        fastReader = new FastReader(new StringReader("ab"));
        char[] buffer = new char[4];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(2, read);
        assertEquals('a', buffer[0]);
        assertEquals('b', buffer[1]);
    }

    @Test
    void testReadIntoCharArrayEndOfStream() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        char[] buffer = new char[4];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(-1, read);
    }

    // Tests for reading newlines and specialized movePosition behavior
    @Test
    void testReadNewlineCharacter() throws IOException {
        fastReader = new FastReader(new StringReader("\n"));
        int ch = fastReader.read();
        assertEquals('\n', ch);
    }

    @Test
    void testReadMixOfRegularAndNewlineChars() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb\nc"));
        assertEquals('a', fastReader.read());
        assertEquals('\n', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('\n', fastReader.read());
        assertEquals('c', fastReader.read());
    }

    // Tests with pushback combined with various input states
    @Test
    void testPushbackAndFill() throws IOException {
        // Create a reader with small buffer to force fill() calls
        fastReader = new FastReader(new StringReader("1234567890"), 4, 3);

        // Read initial content
        assertEquals('1', fastReader.read());
        assertEquals('2', fastReader.read());

        // Pushback something - this tests interaction between buffers
        fastReader.pushback('x');

        // Now read: should get pushback first, then continue with input
        assertEquals('x', fastReader.read());
        assertEquals('3', fastReader.read());
        assertEquals('4', fastReader.read());
        // This read should trigger a fill()
        assertEquals('5', fastReader.read());
    }

    @Test
    void testReadLargeContent() throws IOException {
        // Create a string larger than the buffer
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 3; i++) {
            sb.append((char)('a' + i % 26));
        }
        String largeContent = sb.toString();

        fastReader = new FastReader(new StringReader(largeContent), CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);

        // Read all content char by char
        for (int i = 0; i < largeContent.length(); i++) {
            assertEquals(largeContent.charAt(i), fastReader.read());
        }

        // End of stream
        assertEquals(-1, fastReader.read());
    }

    // Testing the array read when mixing pushback and regular buffer content
    @Test
    void testReadArrayMixingBuffers() throws IOException {
        // Create a string larger than the buffer
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 2; i++) {
            sb.append((char)('a' + i % 26));
        }
        String content = sb.toString();

        fastReader = new FastReader(new StringReader(content), CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);

        // Read some initial content
        char[] initialBuffer = new char[CUSTOM_BUFFER_SIZE / 2];
        int readCount = fastReader.read(initialBuffer, 0, initialBuffer.length);
        assertEquals(CUSTOM_BUFFER_SIZE / 2, readCount);

        // Pushback a few characters
        for (int i = 0; i < CUSTOM_PUSHBACK_SIZE; i++) {
            fastReader.pushback((char)('z' - i));
        }

        // Now read a large array - should get pushback content then regular content
        char[] buffer = new char[CUSTOM_BUFFER_SIZE * 2];
        readCount = fastReader.read(buffer, 0, buffer.length);

        // Verify correct content was read
        for (int i = 0; i < CUSTOM_PUSHBACK_SIZE; i++) {
            assertEquals((char)('z' - CUSTOM_PUSHBACK_SIZE + 1 + i), buffer[i]);
        }

        // Verify remaining buffer matches expected content after initial read
        for (int i = 0; i < readCount - CUSTOM_PUSHBACK_SIZE; i++) {
            assertEquals(content.charAt(i + CUSTOM_BUFFER_SIZE / 2),
                    buffer[i + CUSTOM_PUSHBACK_SIZE]);
        }
    }

    // Mock reader to test specific behaviors
    private static class MockReader extends Reader {
        private boolean returnMinusOne = false;
        private boolean throwException = false;

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (throwException) {
                throw new IOException("Simulated read error");
            }
            if (returnMinusOne) {
                return -1;
            }
            // Return some simple data
            for (int i = 0; i < len; i++) {
                cbuf[off + i] = (char)('a' + i % 26);
            }
            return len;
        }

        @Override
        public void close() {
            // No action needed
        }

        void setReturnMinusOne(boolean value) {
            returnMinusOne = value;
        }

        void setThrowException(boolean value) {
            throwException = value;
        }
    }

    @Test
    void testReadWithEmptyFill() throws IOException {
        MockReader mockReader = new MockReader();
        mockReader.setReturnMinusOne(true);

        fastReader = new FastReader(mockReader, CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);

        // This should trigger a fill() that returns -1
        assertEquals(-1, fastReader.read());
    }

    @Test
    void testReadArrayWithPartialFill() throws IOException {
        // Test the case where fill() returns fewer chars than requested
        MockReader mockReader = new MockReader();
        fastReader = new FastReader(mockReader, CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);

        // Read initial content to advance position to limit
        char[] initialBuffer = new char[CUSTOM_BUFFER_SIZE];
        fastReader.read(initialBuffer, 0, initialBuffer.length);

        // Now set the mock to return EOF
        mockReader.setReturnMinusOne(true);

        // Try to read more - should handle the EOF gracefully
        char[] buffer = new char[10];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(-1, read);
    }

    @Test
    void testReadArrayWithAvailableZero() throws IOException {
        // Test when pushbackPosition == pushbackBufferSize (available = 0)
        fastReader = new FastReader(new StringReader("test"), CUSTOM_BUFFER_SIZE, 1);

        // Fill the pushback buffer completely
        fastReader.pushback('x');

        // Read array - this will have available=0 for pushback
        char[] buffer = new char[10];
        int read = fastReader.read(buffer, 0, buffer.length);

        assertEquals(5, read); // 'x' + 'test'
        assertEquals('x', buffer[0]);
        assertEquals('t', buffer[1]);
    }

    // Tests for getLine(), getCol(), and getLastSnippet()
    @Test
    public void testLineAndColumnTrackingOneCharAtATime() throws IOException {
        fastReader = new FastReader(new StringReader("abc\ndef\nghi"));

        // Initial values - line starts at 1 in FastReader
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // Read 'a'
        assertEquals('a', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // Read 'b'
        assertEquals('b', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        // Read 'c'
        assertEquals('c', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(3, fastReader.getCol());

        // Read '\n'
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine()); // Line increments after reading newline
        assertEquals(0, fastReader.getCol());   // Column resets

        // Read 'd'
        assertEquals('d', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // Read 'e'
        assertEquals('e', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        // Read 'f'
        assertEquals('f', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(3, fastReader.getCol());

        // Read '\n'
        assertEquals('\n', fastReader.read());
        assertEquals(3, fastReader.getLine()); // Line increments again
        assertEquals(0, fastReader.getCol());

        // Read 'g'
        assertEquals('g', fastReader.read());
        assertEquals(3, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    @Test
    public void testInitialLineAndColumnValues() throws IOException {
        fastReader = new FastReader(new StringReader("test"));
        assertEquals(1, fastReader.getLine()); // Line starts at 1, not 0
        assertEquals(0, fastReader.getCol());
    }

    @Test
    public void testLineAndColumnTrackingWithRegularChars() throws IOException {
        fastReader = new FastReader(new StringReader("abcdef"));

        // Initially at (1,0) not (0,0)
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // Read 3 chars
        for (int i = 0; i < 3; i++) {
            fastReader.read();
        }

        // Should still be line 1, but column 3
        assertEquals(1, fastReader.getLine());
        assertEquals(3, fastReader.getCol());
    }

    @Test
    public void testLineAndColumnTrackingWithNewlines() throws IOException {
        fastReader = new FastReader(new StringReader("abc\ndef\nghi"));

        // Read first line
        for (int i = 0; i < 4; i++) { // 'a', 'b', 'c', '\n'
            fastReader.read();
        }

        // After reading the first newline, line should be 2
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // Read 'def\n'
        for (int i = 0; i < 4; i++) {
            fastReader.read();
        }

        // After reading the second newline, line should be 3
        assertEquals(3, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // Read 'g'
        fastReader.read();

        // Should be at line 3, column 1
        assertEquals(3, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }
    
    @Test
    public void testLineAndColumnTrackingWithPushback() throws IOException {
        fastReader = new FastReader(new StringReader("def"));

        // Pushback newline and a char
        fastReader.pushback('c');
        fastReader.pushback('\n');
        fastReader.pushback('b');
        fastReader.pushback('a');

        // Read 'a', 'b', '\n'
        for (int i = 0; i < 3; i++) {
            fastReader.read();
        }

        // Should be at line 1, column 0 after reading newline
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // Read 'c'
        fastReader.read();

        // Should be at line 1, column 1
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    @Test
    void testGetLastSnippetEmpty() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        assertEquals("", fastReader.getLastSnippet());
    }

    @Test
    void testGetLastSnippetAfterReading() throws IOException {
        fastReader = new FastReader(new StringReader("abcdefghijklm"));

        // Read 5 characters
        for (int i = 0; i < 5; i++) {
            fastReader.read();
        }

        // Should have "abcde" in the snippet
        assertEquals("abcde", fastReader.getLastSnippet());

        // Read 3 more characters
        for (int i = 0; i < 3; i++) {
            fastReader.read();
        }

        // Should have "abcdefgh" in the snippet
        assertEquals("abcdefgh", fastReader.getLastSnippet());
    }

    @Test
    void testGetLastSnippetWithNewlines() throws IOException {
        fastReader = new FastReader(new StringReader("ab\ncd\nef"));

        // Read all content
        while (fastReader.read() != -1) {
            // Just read everything
        }

        // Verify the full content is in the snippet, including newlines
        assertEquals("ab\ncd\nef", fastReader.getLastSnippet());
    }

    @Test
    void testGetLastSnippetAfterBuffer() throws IOException {
        // Create a string larger than default buffer for testing
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 2; i++) {
            sb.append((char)('a' + i % 26));
        }
        String largeContent = sb.toString();

        fastReader = new FastReader(new StringReader(largeContent), CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);

        // Read half of the content
        for (int i = 0; i < largeContent.length() / 2; i++) {
            fastReader.read();
        }

        // The snippet should contain only what's in the current buffer
        // This is because getLastSnippet only returns content from the current buffer up to position
        String snippet = fastReader.getLastSnippet();

        // Since buffer refills happen, we need to check that the snippet is the expected length
        // and contains the most recent characters read
        assertEquals(CUSTOM_BUFFER_SIZE, snippet.length());

        // The snippet should match the corresponding part of our large content
        int startPos = (largeContent.length() / 2) - CUSTOM_BUFFER_SIZE;
        if (startPos < 0) startPos = 0;
        String expected = largeContent.substring(startPos, largeContent.length() / 2);
        assertEquals(expected, snippet);
    }
}