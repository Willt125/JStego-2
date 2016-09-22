/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jstego;

import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author signallock
 */
public class Jstego {

    private InputStream is = null;
    private int[][] quantization_table = new int[2][64];
    private int height = 0;
    private int width = 0;
    private int totalMCU = 0;
    private int numOfComponents = 3;
    private int[][][] data = null;
    private int[][] dht_lookup_bitlen = new int[4][256];
    private int[][] dht_lookup_bits = new int[4][256];
    private int[][] dht_lookup_mask = new int[4][256];
    private int[][] dht_lookup_code = new int[4][256];
    private int[] dht_lookup_size = new int[4];
    private int[] dcDHT = new int[3];
    private int[] acDHT = new int[3];
    private int _buffer = 0;
    private int _bufferP = 8;
    private int _prevBuffer = 0;
    private int buffer = 0;
    private JpegInfo JpegObj = null;
    private DCT dct = null;
    private Huffman Huf = null;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private int dataP = 0;
    private int f5_k = 0;
    private long f5_n = 0;
    private int[] f5Buf = new int[65536];
    private int _f5Buffer = 0;
    private int _f5BufferP = 8;
    private String secretFileName = null;
    private long secretFileSize = 0;
    private int stegoType = 0;
    private File infile = null;
    private int[] jpegNaturalOrder = {
        0, 1, 8, 16, 9, 2, 3, 10,
        17, 24, 32, 25, 18, 11, 4, 5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13, 6, 7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63,};

    public Jstego() {
    }

    public Jstego(File infile) throws FileNotFoundException {
        this.infile = infile;
        is = new FileInputStream(infile);
    }

    private int nextMarker() throws IOException {
        int cur = 0;
        do {
            cur = is.read();
        } while (cur != 0xFF);
        do {
            cur = is.read();
        } while (cur == 0xFF);
        return cur;
    }

    private int segmentSize() throws IOException {
        int high = 0;
        int low = 0;
        int size = 0;
        high = is.read();
        low = is.read();
        size = high << 8 | low;
        return size;
    }

    private int locateMarker(int marker) throws IOException {
        marker &= 0x000000FF;
        int size = 0;
        int i = 0;
        int curMarker = nextMarker();
        while (curMarker != marker) {
            size = segmentSize();
            is.skip(size - 2);
            curMarker = nextMarker();
        }
        size = segmentSize();
        return size;
    }

    public void huffmanDecode() throws Exception {
        if (nextMarker() != 0xD8) {
            throw new Exception();
        }
        int curMarker = 0xDB;
        int size = locateMarker(curMarker);
        int qt_id = 0;
        int i = 0;
        while (curMarker == 0xDB) {
            qt_id = is.read();
            if (qt_id > 1) {
                throw new Exception();
            }
            for (i = 0; i < 64; ++i) {
                quantization_table[qt_id][i] = is.read();
            }
            if (size > 67) {
                qt_id = is.read();
                if (qt_id > 1) {
                    throw new Exception();
                }
                for (i = 0; i < 64; ++i) {
                    quantization_table[qt_id][i] = is.read();
                }
            }
            curMarker = nextMarker();
            size = segmentSize();
        }
        if (curMarker != 0xC0) {
            size = locateMarker(0xC0);
        }
        is.skip(1);
        int height_high = is.read();
        int height_low = is.read();
        int width_high = is.read();
        int width_low = is.read();
        height = height_high << 8 | height_low;
        width = width_high << 8 | width_low;
        totalMCU = ((width - 1) / 8 + 1) * ((height - 1) / 8 + 1);
        numOfComponents = is.read();
        if (numOfComponents != 3) {
            throw new Exception();
        }
        data = new int[totalMCU][3][64];
        curMarker = 0xC4;
        size = locateMarker(curMarker);
        int table_ind = 0;
        int dht_ind = 0;
        int table_set = 0;
        int tmp_mask, tmp_bits, tmp_code;
        int code_val = 0;
        int[] dht_code_list = new int[256];
        int[] dht_codes_len = new int[20];
        int ind_len = 0;
        int ind_code = 0;
        int bit_len = 0;
        int code_ind;
        while (curMarker == 0xC4) {
            size -= 2;
            while (size > 0) {
                table_ind = 0;
                dht_ind = 0;
                table_set = is.read();
                --size;
                table_set = ((table_set & 0xF0) >> 3) | (table_set & 0x0F);
                for (i = 0; i < 256; ++i) {
                    dht_code_list[i] = 0xFF;
                }
                for (i = 1; i <= 16; ++i) {
                    dht_codes_len[i] = is.read();
                }
                size -= 16;
                for (ind_len = 1; ind_len <= 16; ++ind_len) {
                    for (ind_code = 0; ind_code < dht_codes_len[ind_len]; ++ind_code) {
                        dht_code_list[dht_ind++] = is.read();
                        --size;
                    }
                }
                code_val = 0;
                dht_ind = 0;
                for (bit_len = 1; bit_len <= 16; ++bit_len) {
                    for (code_ind = 1; code_ind <= dht_codes_len[bit_len]; ++code_ind) {
                        tmp_mask = ((1 << bit_len) - 1) << (32 - bit_len);
                        tmp_bits = code_val << (32 - bit_len);
                        tmp_code = dht_code_list[dht_ind];
                        dht_lookup_bitlen[table_set][table_ind] = bit_len;
                        dht_lookup_bits[table_set][table_ind] = tmp_bits;
                        dht_lookup_mask[table_set][table_ind] = tmp_mask;
                        dht_lookup_code[table_set][table_ind] = tmp_code;
                        ++table_ind;
                        ++code_val;
                        ++dht_ind;
                    }
                    code_val <<= 1;
                }
                dht_lookup_size[table_set] = table_ind;
            }
            curMarker = nextMarker();
            size = segmentSize();
        }
        if (curMarker != 0xDA) {
            size = locateMarker(0xDA);
        }
        if (is.read() != 3) {
            throw new Exception();
        }
        int id = is.read();
        int tmp = is.read();
        dcDHT[id - 1] = (tmp >> 4) & 0x0F;
        acDHT[id - 1] = tmp & 0x0F;
        id = is.read();
        tmp = is.read();
        dcDHT[id - 1] = (tmp >> 4) & 0x0F;
        acDHT[id - 1] = tmp & 0x0F;
        id = is.read();
        tmp = is.read();
        dcDHT[id - 1] = (tmp >> 4) & 0x0F;
        acDHT[id - 1] = tmp & 0x0F;
        if (is.read() != 0) {
            throw new Exception();
        }
        if (is.read() != 0x3F) {
            throw new Exception();
        }
        if (is.read() != 0) {
            throw new Exception();
        }

        _bufferP = 8;
        flushBuffer(16);
        int component = 0;
        boolean done = false;
        boolean isDC = true;
        int ind = 0;
        int set = 0;
        int curP = 0;
        int j = 0;
        int code = 0;
        int[] prec = new int[3];
        prec[0] = 0;
        prec[1] = 0;
        prec[2] = 0;
        int value = 0;
        int scan_buff = 0;

        for (i = 0; i < totalMCU; ++i) {
            for (component = 0; component < 3; ++component) {
                for (j = 0; j < 64; ++j) {
                    data[i][component][j] = 0;
                }
            }
        }

        for (i = 0; i < totalMCU; ++i) {
            component = 0;
            curP = 0;
            isDC = true;
            while (component < 3) {
                scan_buff = buffer << 16;
                done = false;
                if (isDC) {
                    set = dcDHT[component];
                } else {
                    set = acDHT[component] | 2;
                }
                ind = 0;
                while (!done) {
                    if ((scan_buff & dht_lookup_mask[set][ind]) == dht_lookup_bits[set][ind]) {
                        code = dht_lookup_code[set][ind];
                        flushBuffer(dht_lookup_bitlen[set][ind]);
                        done = true;
                    }
                    ++ind;
                    if (ind >= dht_lookup_size[set]) {
                        throw new Exception();
                    }
                }
                if (isDC) {
                    if (code > 0) {
                        value = getValue(code);
                        flushBuffer(code);
                        if ((value >> (code - 1)) == 0) {
                            value ^= 0xFFFF;
                            value &= ((1 << code) - 1);
                            value = -value;
                        }
                    } else {
                        value = 0;
                    }
                    value += prec[component];
                    prec[component] = value;
                    data[i][component][curP] = value;
                    ++curP;
                    isDC = false;
                } else {
                    if (code == 0) {
                        ++component;
                        curP = 0;
                        isDC = true;
                    } else if (code == 0xF0) {
                        curP += 15;
                    } else {
                        curP += ((code >> 4) & 0x0F);
                        value = getValue(code & 0x0F);
                        flushBuffer(code & 0x0F);
                        if (value >> ((code & 0x0F) - 1) == 0) {
                            value ^= 0xFFFF;
                            value &= ((1 << (code & 0x0F)) - 1);
                            value = -value;
                        }
                        data[i][component][curP] = value;
                        ++curP;
                    }
                }
            }
        }
        int[] temp = new int[64];
        for (i = 0; i < totalMCU; ++i) {
            for (component = 0; component < 3; ++component) {
                for (j = 0; j < 64; ++j) {
                    temp[jpegNaturalOrder[j]] = data[i][component][j];
                }
                for (j = 0; j < 64; ++j) {
                    data[i][component][j] = temp[j];
                }
            }
        }
    }

    private int flushBuffer(int numBits) throws IOException {
        buffer <<= numBits;
        buffer |= getBits(numBits);
        buffer &= 0xFFFF;
        return buffer;
    }

    private int getValue(int numBits) {
        int retval = 0;
        retval = buffer >> (16 - numBits);
        retval &= ((1 << numBits) - 1);
        return retval;
    }

    private int getBits(int numBits) throws IOException {
        int retval = 0;
        while (numBits > 0) {
            retval <<= 1;
            if (_bufferP >= 8) {
                _buffer = is.read();
                if (_buffer == 0 && _prevBuffer == 0xFF) {
                    _buffer = is.read();
                }
                _prevBuffer = _buffer;
                _bufferP = 0;
            }
            retval |= (((_buffer & (1 << (7 - _bufferP))) > 0) ? 1 : 0);
            ++_bufferP;
            --numBits;
        }
        return retval;
    }

    public void huffmanEncode(File outfile) throws IOException {
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outfile));
        WriteHeaders(os);
        WriteCompressedData(os);
        WriteEOI(os);
        os.flush();
    }

    public void generateCover(File infile, File cover) throws IOException {
        Image image = ImageIO.read(infile);
        OutputStream cover_os = new FileOutputStream(cover);
        JpegEncoder je = new JpegEncoder(image, 80, cover_os);
        JpegObj = je.getJpegInfo();
        imageWidth = je.getImageWidth();
        imageHeight = je.getImageHeight();
        dct = je.getDct();
        Huf = new Huffman(imageWidth, imageHeight);
        je.Compress();
        is = new FileInputStream(cover);
    }

    private void WriteHeaders(BufferedOutputStream out) {
        int i, j, index, offset, length;
        int tempArray[];

// the SOI marker
        byte[] SOI = {(byte) 0xFF, (byte) 0xD8};
        WriteMarker(SOI, out);

// The order of the following headers is quiet inconsequential.
// the JFIF header
        byte JFIF[] = new byte[18];
        JFIF[0] = (byte) 0xff;
        JFIF[1] = (byte) 0xe0;
        JFIF[2] = (byte) 0x00;
        JFIF[3] = (byte) 0x10;
        JFIF[4] = (byte) 0x4a;
        JFIF[5] = (byte) 0x46;
        JFIF[6] = (byte) 0x49;
        JFIF[7] = (byte) 0x46;
        JFIF[8] = (byte) 0x00;
        JFIF[9] = (byte) 0x01;
        JFIF[10] = (byte) 0x00;
        JFIF[11] = (byte) 0x00;
        JFIF[12] = (byte) 0x00;
        JFIF[13] = (byte) 0x01;
        JFIF[14] = (byte) 0x00;
        JFIF[15] = (byte) 0x01;
        JFIF[16] = (byte) 0x00;
        JFIF[17] = (byte) 0x00;
        WriteArray(JFIF, out);

// Comment Header
        String comment = new String();
        comment = JpegObj.getComment();
        length = comment.length();
        byte COM[] = new byte[length + 4];
        COM[0] = (byte) 0xFF;
        COM[1] = (byte) 0xFE;
        COM[2] = (byte) ((length >> 8) & 0xFF);
        COM[3] = (byte) (length & 0xFF);
        java.lang.System.arraycopy(JpegObj.Comment.getBytes(), 0, COM, 4, JpegObj.Comment.length());
        WriteArray(COM, out);

// The DQT header
// 0 is the luminance index and 1 is the chrominance index
        byte DQT[] = new byte[134];
        DQT[0] = (byte) 0xFF;
        DQT[1] = (byte) 0xDB;
        DQT[2] = (byte) 0x00;
        DQT[3] = (byte) 0x84;
        offset = 4;
        for (i = 0; i < 2; i++) {
            DQT[offset++] = (byte) ((0 << 4) + i);
            tempArray = (int[]) dct.quantum[i];
            for (j = 0; j < 64; j++) {
                DQT[offset++] = (byte) tempArray[jpegNaturalOrder[j]];
            }
        }
        WriteArray(DQT, out);

// Start of Frame Header
        byte SOF[] = new byte[19];
        SOF[0] = (byte) 0xFF;
        SOF[1] = (byte) 0xC0;
        SOF[2] = (byte) 0x00;
        SOF[3] = (byte) 17;
        SOF[4] = (byte) JpegObj.Precision;
        SOF[5] = (byte) ((JpegObj.imageHeight >> 8) & 0xFF);
        SOF[6] = (byte) ((JpegObj.imageHeight) & 0xFF);
        SOF[7] = (byte) ((JpegObj.imageWidth >> 8) & 0xFF);
        SOF[8] = (byte) ((JpegObj.imageWidth) & 0xFF);
        SOF[9] = (byte) JpegObj.NumberOfComponents;
        index = 10;
        for (i = 0; i < SOF[9]; i++) {
            SOF[index++] = (byte) JpegObj.CompID[i];
            SOF[index++] = (byte) ((JpegObj.HsampFactor[i] << 4) + JpegObj.VsampFactor[i]);
            SOF[index++] = (byte) JpegObj.QtableNumber[i];
        }
        WriteArray(SOF, out);

// The DHT Header
        byte DHT1[], DHT2[], DHT3[], DHT4[];
        int bytes, temp, oldindex, intermediateindex;
        length = 2;
        index = 4;
        oldindex = 4;
        DHT1 = new byte[17];
        DHT4 = new byte[4];
        DHT4[0] = (byte) 0xFF;
        DHT4[1] = (byte) 0xC4;
        for (i = 0; i < 4; i++) {
            bytes = 0;
            DHT1[index++ - oldindex] = (byte) ((int[]) Huf.bits.elementAt(i))[0];
            for (j = 1; j < 17; j++) {
                temp = ((int[]) Huf.bits.elementAt(i))[j];
                DHT1[index++ - oldindex] = (byte) temp;
                bytes += temp;
            }
            intermediateindex = index;
            DHT2 = new byte[bytes];
            for (j = 0; j < bytes; j++) {
                DHT2[index++ - intermediateindex] = (byte) ((int[]) Huf.val.elementAt(i))[j];
            }
            DHT3 = new byte[index];
            java.lang.System.arraycopy(DHT4, 0, DHT3, 0, oldindex);
            java.lang.System.arraycopy(DHT1, 0, DHT3, oldindex, 17);
            java.lang.System.arraycopy(DHT2, 0, DHT3, oldindex + 17, bytes);
            DHT4 = DHT3;
            oldindex = index;
        }
        DHT4[2] = (byte) (((index - 2) >> 8) & 0xFF);
        DHT4[3] = (byte) ((index - 2) & 0xFF);
        WriteArray(DHT4, out);


// Start of Scan Header
        byte SOS[] = new byte[14];
        SOS[0] = (byte) 0xFF;
        SOS[1] = (byte) 0xDA;
        SOS[2] = (byte) 0x00;
        SOS[3] = (byte) 12;
        SOS[4] = (byte) JpegObj.NumberOfComponents;
        index = 5;
        for (i = 0; i < SOS[4]; i++) {
            SOS[index++] = (byte) JpegObj.CompID[i];
            SOS[index++] = (byte) ((JpegObj.DCtableNumber[i] << 4) + JpegObj.ACtableNumber[i]);
        }
        SOS[index++] = (byte) JpegObj.Ss;
        SOS[index++] = (byte) JpegObj.Se;
        SOS[index++] = (byte) ((JpegObj.Ah << 4) + JpegObj.Al);
        WriteArray(SOS, out);

    }

    private void WriteMarker(byte[] data, BufferedOutputStream out) {
        try {
            out.write(data, 0, 2);
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        }
    }

    private void WriteArray(byte[] data, BufferedOutputStream out) {
        int i, length;
        try {
            length = (((int) (data[2] & 0xFF)) << 8) + (int) (data[3] & 0xFF) + 2;
            out.write(data, 0, length);
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        }
    }

    private void WriteCompressedData(BufferedOutputStream outStream) {
        int offset, i, j, r, c, a, b, temp = 0;
        int comp, xpos, ypos, xblockoffset, yblockoffset;
        float inputArray[][];
        float dctArray1[][] = new float[8][8];
        double dctArray2[][] = new double[8][8];
        int dctArray3[] = new int[8 * 8];

        /*
         * This method controls the compression of the image.
         * Starting at the upper left of the image, it compresses 8x8 blocks
         * of data until the entire image has been compressed.
         */

        int lastDCvalue[] = new int[JpegObj.NumberOfComponents];
        int zeroArray[] = new int[64]; // initialized to hold all zeros
        int Width = 0, Height = 0;
        int nothing = 0, not;
        int MinBlockWidth, MinBlockHeight;
// This initial setting of MinBlockWidth and MinBlockHeight is done to
// ensure they start with values larger than will actually be the case.
        MinBlockWidth = ((imageWidth % 8 != 0) ? (int) (Math.floor((double) imageWidth / 8.0) + 1) * 8 : imageWidth);
        MinBlockHeight = ((imageHeight % 8 != 0) ? (int) (Math.floor((double) imageHeight / 8.0) + 1) * 8 : imageHeight);
        for (comp = 0; comp < JpegObj.NumberOfComponents; comp++) {
            MinBlockWidth = Math.min(MinBlockWidth, JpegObj.BlockWidth[comp]);
            MinBlockHeight = Math.min(MinBlockHeight, JpegObj.BlockHeight[comp]);
        }
        xpos = 0;
        for (r = 0; r < MinBlockHeight; r++) {
            for (c = 0; c < MinBlockWidth; c++) {
                xpos = c * 8;
                ypos = r * 8;
                for (comp = 0; comp < JpegObj.NumberOfComponents; comp++) {
                    Width = JpegObj.BlockWidth[comp];
                    Height = JpegObj.BlockHeight[comp];
                    inputArray = (float[][]) JpegObj.Components[comp];

                    for (i = 0; i < JpegObj.VsampFactor[comp]; i++) {
                        for (j = 0; j < JpegObj.HsampFactor[comp]; j++) {
                            xblockoffset = j * 8;
                            yblockoffset = i * 8;
                            for (a = 0; a < 8; a++) {
                                for (b = 0; b < 8; b++) {

// I believe this is where the dirty line at the bottom of the image is
// coming from.  I need to do a check here to make sure I'm not reading past
// image data.
// This seems to not be a big issue right now. (04/04/98)

//                                    dctArray1[a][b] = inputArray[ypos + yblockoffset + a][xpos + xblockoffset + b];
                                    dctArray1[a][b] = inputArray[ypos + yblockoffset + a][xpos + xblockoffset + b];
                                }
                            }
// The following code commented out because on some images this technique
// results in poor right and bottom borders.
//                        if ((!JpegObj.lastColumnIsDummy[comp] || c < Width - 1) && (!JpegObj.lastRowIsDummy[comp] || r < Height - 1)) {
//                            dctArray2 = dct.forwardDCT(dctArray1);
//                            dctArray3 = dct.quantizeBlock(dctArray2, JpegObj.QtableNumber[comp]);
//                        }
//                        else {
//                           zeroArray[0] = dctArray3[0];
//                           zeroArray[0] = lastDCvalue[comp];
//                           dctArray3 = zeroArray;
//                        }
//                            Huf.HuffmanBlockEncoder(outStream, dctArray3, lastDCvalue[comp], JpegObj.DCtableNumber[comp], JpegObj.ACtableNumber[comp]);
                            Huf.HuffmanBlockEncoder(outStream, data[r * MinBlockWidth + c][comp], lastDCvalue[comp], JpegObj.DCtableNumber[comp], JpegObj.ACtableNumber[comp]);
//                            lastDCvalue[comp] = dctArray3[0];
                            lastDCvalue[comp] = data[r * MinBlockWidth + c][comp][0];
                        }
                    }
                }
            }
        }
        Huf.flushBuffer(outStream);
    }

    private void WriteEOI(BufferedOutputStream out) {
        byte[] EOI = {(byte) 0xFF, (byte) 0xD9};
        WriteMarker(EOI, out);
    }

    private void jstegHideBit(int bit) {
        bit &= 1;
        while (data[dataP / 64][0][dataP % 64] == 0 || data[dataP / 64][0][dataP % 64] == 1 || data[dataP / 64][0][dataP % 64] == -1 || ((dataP % 64) == 0)) {
            ++dataP;
        }
        if (bit != bitTest(data[dataP / 64][0][dataP % 64], 0)) {
            if (data[dataP / 64][0][dataP % 64] > 0) {
                ++data[dataP / 64][0][dataP % 64];
            } else {
                --data[dataP / 64][0][dataP % 64];
            }
        }
        ++dataP;
    }

    public void jstegHide(File secret) throws Exception {
        String fileName = secret.getName();
        byte[] fileNameBytes = fileName.getBytes();
        long fileSize = secret.length();
        if (fileNameBytes.length > 255) {
            throw new Exception();
        }
        if (jstegMaxFileSize() < (1 + fileNameBytes.length + 8 + fileSize)) {
            throw new Exception();
        }

        dataP = 0;
        int i = 0;
        for (i = 0; i < 8; ++i) {
            jstegHideBit(bitTest(fileNameBytes.length, i));
        }

        int j = 0;
        for (i = 0; i < fileNameBytes.length; ++i) {
            for (j = 0; j < 8; ++j) {
                jstegHideBit(bitTest(fileNameBytes[i], j));
            }
        }

        for (i = 0; i < 64; ++i) {
            jstegHideBit(bitTest(fileSize, i));
        }

        for (i = 0; i < 2; ++i) {
            jstegHideBit(bitTest(1, i));
        }

        InputStream sis = new FileInputStream(secret);
        int secret_buffer = 0;
        for (i = 0; i < fileSize; ++i) {
            secret_buffer = sis.read();
            for (j = 0; j < 8; ++j) {
                jstegHideBit(bitTest(secret_buffer, j));
            }
        }
    }

    private int bitTest(long num, int bitNum) {
        return (int) ((num >> bitNum) & 1);
    }

    private int jstegSeekBit() {
        int retval = 0;
        while (data[dataP / 64][0][dataP % 64] == 0 || data[dataP / 64][0][dataP % 64] == 1 || data[dataP / 64][0][dataP % 64] == -1 || ((dataP % 64) == 0)) {
            ++dataP;
        }
        retval = bitTest(data[dataP / 64][0][dataP % 64], 0);
        ++dataP;
        return retval;
    }

    public void jstegSeek() throws FileNotFoundException, IOException, Exception {
        int i = 0, j = 0;

        OutputStream sos = new FileOutputStream(new File(secretFileName));
        byte[] secret_out_buffer = new byte[1];
        for (i = 0; i < secretFileSize; ++i) {
            secret_out_buffer[0] = 0;
            for (j = 0; j < 8; ++j) {
                secret_out_buffer[0] = (byte) setBit(secret_out_buffer[0], j, jstegSeekBit());
            }
            sos.write(secret_out_buffer);
        }
        sos.flush();
    }

    private long setBit(long num, int bitNum, int value) {
        value &= 1;
        if (value == 1) {
            num |= 1L << bitNum;
        } else {
            long temp = 1L << bitNum;
            temp ^= 0xFFFFFFFFFFFFFFFFL;
            num &= temp;
        }
        return num;
    }

    public int jstegMaxFileSize() {
        int retval = 0;
        dataP = 0;
        while (dataP < (totalMCU * 64)) {
            while ((dataP < (totalMCU * 64)) && (data[dataP / 64][0][dataP % 64] == 0 || data[dataP / 64][0][dataP % 64] == 1 || data[dataP / 64][0][dataP % 64] == -1 || ((dataP % 64) == 0))) {
                ++dataP;
            }
            if (dataP < (totalMCU * 64)) {
                ++retval;
                ++dataP;
            }
        }
        retval /= 8;
        return retval;
    }

    private int f5NextF() {
        int retval = 0;
        int i = 0;
        int temp = 0;
        for (i = 0; i < f5_n; ++i) {
            while (((dataP % 64) == 0) || (data[dataP / 64][0][dataP % 64] == 0)) {
                ++dataP;
            }
            f5Buf[i] = dataP;
            if ((data[dataP / 64][0][dataP % 64] % 2) == 0) {
                temp = 0;
            } else {
                temp = 1;
            }
            retval ^= (temp * (i + 1));
            ++dataP;
        }
        return retval;
    }

    private void f5HideValue(int value) {
        value &= ((1 << f5_k) - 1);
        int f = f5NextF();
        int c = f ^ value;
        if (c != 0) {
            --c;
            if (data[f5Buf[c] / 64][0][f5Buf[c] % 64] > 0) {
                ++data[f5Buf[c] / 64][0][f5Buf[c] % 64];
            } else {
                --data[f5Buf[c] / 64][0][f5Buf[c] % 64];
            }
        }
    }

    private int curF() {
        int retval = 0;
        int i = 0;
        int temp = 0;
        for (i = 0; i < f5_n; ++i) {
            if (data[f5Buf[i] / 64][0][f5Buf[i] % 64] % 2 == 0) {
                temp = 0;
            } else {
                temp = 1;
            }
            retval ^= temp * (i + 1);
        }
        return retval;
    }

    private int f5MaxK(long fileSize) {
        long total = 0;
        dataP = 0;

        while (dataP < (totalMCU * 64)) {
            while ((dataP < (totalMCU * 64)) && (((dataP % 64) == 0) || (data[dataP / 64][0][dataP % 64] == 0))) {
                ++dataP;
            }
            if (dataP < (totalMCU * 64)) {
                ++total;
                ++dataP;
            }
        }

        int k = 1;
        while (((double) k / (fileSize * 8) >= ((double) (Math.pow(2, k) - 1)) / total)) {
            ++k;
        }
        --k;
//        if (k > 2) {
//            k = (int) (k * 0.8);
//        }
        return k;
    }

    public void f5Hide(File secret) throws Exception {
        String fileName = secret.getName();
        byte[] fileNameBytes = fileName.getBytes();
        long fileSize = secret.length();

        if (fileNameBytes.length > 255) {
            throw new Exception();
        }

        dataP = 0;
        f5_k = f5MaxK(fileSize + 10);
        f5_n = Math.round(Math.pow(2, f5_k)) - 1;
        dataP = 0;
        int i = 0;
        for (i = 0; i < 8; ++i) {
            jstegHideBit(bitTest(fileNameBytes.length, i));
        }

        int j = 0;
        for (i = 0; i < fileNameBytes.length; ++i) {
            for (j = 0; j < 8; ++j) {
                jstegHideBit(bitTest(fileNameBytes[i], j));
            }
        }

        for (i = 0; i < 64; ++i) {
            jstegHideBit(bitTest(fileSize, i));
        }

        for (i = 0; i < 2; ++i) {
            jstegHideBit(bitTest(2, i));
        }

        for (i = 0; i < 8; ++i) {
            jstegHideBit(bitTest(f5_k, i));
        }

        InputStream sis = new FileInputStream(secret);
        _f5Buffer = 0;
        _f5BufferP = 8;
        for (i = 1; i <= ((fileSize * 8 + f5_k - 1) / f5_k); ++i) {
            if ((i * f5_k) <= (fileSize * 8)) {
                f5HideValue(f5GetBits(sis, f5_k));
            } else {
                f5HideValue(f5GetBits(sis, (int) (fileSize * 8 + f5_k - i * f5_k)));
            }
        }
    }

    private int f5GetBits(InputStream sis, int numBits) throws IOException {
        int retval = 0;
        while (numBits > 0) {
            retval <<= 1;
            if (_f5BufferP >= 8) {
                _f5Buffer = sis.read();
                _f5BufferP = 0;
            }
            retval |= (((_f5Buffer & (1 << (7 - _f5BufferP))) > 0) ? 1 : 0);
            ++_f5BufferP;
            --numBits;
        }
        return retval;
    }

    public void f5Seek() throws Exception {
        int i = 0;

        f5_k = 0;
        for (i = 0; i < 8; ++i) {
            f5_k = (int) setBit(f5_k, i, jstegSeekBit());
        }
        f5_n = Math.round(Math.pow(2, f5_k)) - 1;

        _f5Buffer = 0;
        _f5BufferP = 0;
        OutputStream sos = new FileOutputStream(new File(secretFileName));

        for (i = 1; i <= ((secretFileSize * 8 + f5_k - 1) / f5_k); ++i) {
            if ((i * f5_k) <= (secretFileSize * 8)) {
                f5PutValue(sos, f5NextF(), f5_k);
            } else {
                f5PutValue(sos, f5NextF(), (int) secretFileSize * 8 + f5_k - i * f5_k);
            }
        }
    }

    private void f5PutValue(OutputStream sos, int value, int numBits) throws IOException {
        value &= ((1 << numBits) - 1);
        int i = 0;
        byte[] tmp = new byte[1];
        for (i = (numBits - 1); i >= 0; --i) {
            _f5Buffer = (int) setBit(_f5Buffer, 7 - _f5BufferP, bitTest(value, i));
            ++_f5BufferP;
            tmp[0] = (byte) _f5Buffer;
            if (_f5BufferP >= 8) {
                sos.write(tmp);
                sos.flush();
                _f5Buffer = 0;
                _f5BufferP = 0;
            }
        }
    }

    public void getSecretFileInfo() {
        int length = 0;
        int i = 0;

        dataP = 0;
        for (i = 0; i < 8; ++i) {
            length = (int) setBit(length, i, jstegSeekBit());
        }

        byte[] fileNameBytes = new byte[length];
        int j = 0;
        for (i = 0; i < length; ++i) {
            for (j = 0; j < 8; ++j) {
                fileNameBytes[i] = (byte) setBit(fileNameBytes[i], j, jstegSeekBit());
            }
        }
        String fileName = new String(fileNameBytes);
        String dir = infile.getPath();
        String infileName = infile.getName();
        dir = dir.subSequence(0, dir.length() - infileName.length()).toString();
        secretFileName = dir + "/" + fileName;

        long fileSize = 0;
        for (i = 0; i < 64; ++i) {
            fileSize = setBit(fileSize, i, jstegSeekBit());
        }
        secretFileSize = fileSize;

        int type = 0;
        for (i = 0; i < 2; ++i) {
            type = (int) setBit(type, i, jstegSeekBit());
        }
        stegoType = type;
    }

    public void jstegoSeek() throws Exception {
        dataP = 0;
        getSecretFileInfo();
        if (stegoType == 1) {
            jstegSeek();
        } else if (stegoType == 2) {
            f5Seek();
        } else {
            throw new Exception();
        }
    }

    public int getStegoType() {
        return stegoType;
    }
}
