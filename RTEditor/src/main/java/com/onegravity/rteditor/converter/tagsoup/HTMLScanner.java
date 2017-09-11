// This file is part of TagSoup and is Copyright 2002-2008 by John Cowan.
//
// TagSoup is licensed under the Apache License,
// Version 2.0.  You may obtain a copy of this license at
// http://www.apache.org/licenses/LICENSE-2.0 .  You may also have
// additional legal rights not granted by this license.
//
// TagSoup is distributed in the hope that it will be useful, but
// unless required by applicable law or agreed to in writing, TagSoup
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, either express or implied; not even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// 
// 

package com.onegravity.rteditor.converter.tagsoup;

import java.io.*;

import org.xml.sax.SAXException;
import org.xml.sax.Locator;

/**
 * This class implements a table-driven scanner for HTML, allowing for lots of
 * defects. It implements the Scanner interface, which accepts a Reader object
 * to fetch characters from and a ScanHandler object to report lexical events
 * to.
 */

public class HTMLScanner implements Scanner, Locator {

    // Start of state table
    private static final int S_ANAME = 1;
    private static final int S_APOS = 2;
    private static final int S_AVAL = 3;
    private static final int S_BB = 4;
    private static final int S_BBC = 5;
    private static final int S_BBCD = 6;
    private static final int S_BBCDA = 7;
    private static final int S_BBCDAT = 8;
    private static final int S_BBCDATA = 9;
    private static final int S_CDATA = 10;
    private static final int S_CDATA2 = 11;
    private static final int S_CDSECT = 12;
    private static final int S_CDSECT1 = 13;
    private static final int S_CDSECT2 = 14;
    private static final int S_COM = 15;
    private static final int S_COM2 = 16;
    private static final int S_COM3 = 17;
    private static final int S_COM4 = 18;
    private static final int S_DECL = 19;
    private static final int S_DECL2 = 20;
    private static final int S_DONE = 21;
    private static final int S_EMPTYTAG = 22;
    private static final int S_ENT = 23;
    private static final int S_EQ = 24;
    private static final int S_ETAG = 25;
    private static final int S_GI = 26;
    private static final int S_NCR = 27;
    private static final int S_PCDATA = 28;
    private static final int S_PI = 29;
    private static final int S_PITARGET = 30;
    private static final int S_QUOT = 31;
    private static final int S_STAGC = 32;
    private static final int S_TAG = 33;
    private static final int S_TAGWS = 34;
    private static final int S_XNCR = 35;
    private static final int A_ADUP = 1;
    private static final int A_ADUP_SAVE = 2;
    private static final int A_ADUP_STAGC = 3;
    private static final int A_ANAME = 4;
    private static final int A_ANAME_ADUP = 5;
    private static final int A_ANAME_ADUP_STAGC = 6;
    private static final int A_AVAL = 7;
    private static final int A_AVAL_STAGC = 8;
    private static final int A_CDATA = 9;
    private static final int A_CMNT = 10;
    private static final int A_DECL = 11;
    private static final int A_EMPTYTAG = 12;
    private static final int A_ENTITY = 13;
    private static final int A_ENTITY_START = 14;
    private static final int A_ETAG = 15;
    private static final int A_GI = 16;
    private static final int A_GI_STAGC = 17;
    private static final int A_LT = 18;
    private static final int A_LT_PCDATA = 19;
    private static final int A_MINUS = 20;
    private static final int A_MINUS2 = 21;
    private static final int A_MINUS3 = 22;
    private static final int A_PCDATA = 23;
    private static final int A_PI = 24;
    private static final int A_PITARGET = 25;
    private static final int A_PITARGET_PI = 26;
    private static final int A_SAVE = 27;
    private static final int A_SKIP = 28;
    private static final int A_SP = 29;
    private static final int A_STAGC = 30;
    private static final int A_UNGET = 31;
    private static final int A_UNSAVE_PCDATA = 32;
    private static int[] statetable = {S_ANAME, '/', A_ANAME_ADUP, S_EMPTYTAG,
            S_ANAME, '=', A_ANAME, S_AVAL, S_ANAME, '>', A_ANAME_ADUP_STAGC,
            S_PCDATA, S_ANAME, 0, A_SAVE, S_ANAME, S_ANAME, -1,
            A_ANAME_ADUP_STAGC, S_DONE, S_ANAME, ' ', A_ANAME, S_EQ, S_ANAME,
            '\n', A_ANAME, S_EQ, S_ANAME, '\t', A_ANAME, S_EQ, S_APOS, '\'',
            A_AVAL, S_TAGWS, S_APOS, 0, A_SAVE, S_APOS, S_APOS, -1,
            A_AVAL_STAGC, S_DONE, S_APOS, ' ', A_SP, S_APOS, S_APOS, '\n',
            A_SP, S_APOS, S_APOS, '\t', A_SP, S_APOS, S_AVAL, '\'', A_SKIP,
            S_APOS, S_AVAL, '"', A_SKIP, S_QUOT, S_AVAL, '>', A_AVAL_STAGC,
            S_PCDATA, S_AVAL, 0, A_SAVE, S_STAGC, S_AVAL, -1, A_AVAL_STAGC,
            S_DONE, S_AVAL, ' ', A_SKIP, S_AVAL, S_AVAL, '\n', A_SKIP, S_AVAL,
            S_AVAL, '\t', A_SKIP, S_AVAL, S_BB, 'C', A_SKIP, S_BBC, S_BB, 0,
            A_SKIP, S_DECL, S_BB, -1, A_SKIP, S_DONE, S_BBC, 'D', A_SKIP,
            S_BBCD, S_BBC, 0, A_SKIP, S_DECL, S_BBC, -1, A_SKIP, S_DONE,
            S_BBCD, 'A', A_SKIP, S_BBCDA, S_BBCD, 0, A_SKIP, S_DECL, S_BBCD,
            -1, A_SKIP, S_DONE, S_BBCDA, 'T', A_SKIP, S_BBCDAT, S_BBCDA, 0,
            A_SKIP, S_DECL, S_BBCDA, -1, A_SKIP, S_DONE, S_BBCDAT, 'A', A_SKIP,
            S_BBCDATA, S_BBCDAT, 0, A_SKIP, S_DECL, S_BBCDAT, -1, A_SKIP,
            S_DONE, S_BBCDATA, '[', A_SKIP, S_CDSECT, S_BBCDATA, 0, A_SKIP,
            S_DECL, S_BBCDATA, -1, A_SKIP, S_DONE, S_CDATA, '<', A_SAVE,
            S_CDATA2, S_CDATA, 0, A_SAVE, S_CDATA, S_CDATA, -1, A_PCDATA,
            S_DONE, S_CDATA2, '/', A_UNSAVE_PCDATA, S_ETAG, S_CDATA2, 0,
            A_SAVE, S_CDATA, S_CDATA2, -1, A_UNSAVE_PCDATA, S_DONE, S_CDSECT,
            ']', A_SAVE, S_CDSECT1, S_CDSECT, 0, A_SAVE, S_CDSECT, S_CDSECT,
            -1, A_SKIP, S_DONE, S_CDSECT1, ']', A_SAVE, S_CDSECT2, S_CDSECT1,
            0, A_SAVE, S_CDSECT, S_CDSECT1, -1, A_SKIP, S_DONE, S_CDSECT2, '>',
            A_CDATA, S_PCDATA, S_CDSECT2, 0, A_SAVE, S_CDSECT, S_CDSECT2, -1,
            A_SKIP, S_DONE, S_COM, '-', A_SKIP, S_COM2, S_COM, 0, A_SAVE,
            S_COM2, S_COM, -1, A_CMNT, S_DONE, S_COM2, '-', A_SKIP, S_COM3,
            S_COM2, 0, A_SAVE, S_COM2, S_COM2, -1, A_CMNT, S_DONE, S_COM3, '-',
            A_SKIP, S_COM4, S_COM3, 0, A_MINUS, S_COM2, S_COM3, -1, A_CMNT,
            S_DONE, S_COM4, '-', A_MINUS3, S_COM4, S_COM4, '>', A_CMNT,
            S_PCDATA, S_COM4, 0, A_MINUS2, S_COM2, S_COM4, -1, A_CMNT, S_DONE,
            S_DECL, '-', A_SKIP, S_COM, S_DECL, '[', A_SKIP, S_BB, S_DECL, '>',
            A_SKIP, S_PCDATA, S_DECL, 0, A_SAVE, S_DECL2, S_DECL, -1, A_SKIP,
            S_DONE, S_DECL2, '>', A_DECL, S_PCDATA, S_DECL2, 0, A_SAVE,
            S_DECL2, S_DECL2, -1, A_SKIP, S_DONE, S_EMPTYTAG, '>', A_EMPTYTAG,
            S_PCDATA, S_EMPTYTAG, 0, A_SAVE, S_ANAME, S_EMPTYTAG, ' ', A_SKIP,
            S_TAGWS, S_EMPTYTAG, '\n', A_SKIP, S_TAGWS, S_EMPTYTAG, '\t',
            A_SKIP, S_TAGWS, S_ENT, 0, A_ENTITY, S_ENT, S_ENT, -1, A_ENTITY,
            S_DONE, S_EQ, '=', A_SKIP, S_AVAL, S_EQ, '>', A_ADUP_STAGC,
            S_PCDATA, S_EQ, 0, A_ADUP_SAVE, S_ANAME, S_EQ, -1, A_ADUP_STAGC,
            S_DONE, S_EQ, ' ', A_SKIP, S_EQ, S_EQ, '\n', A_SKIP, S_EQ, S_EQ,
            '\t', A_SKIP, S_EQ, S_ETAG, '>', A_ETAG, S_PCDATA, S_ETAG, 0,
            A_SAVE, S_ETAG, S_ETAG, -1, A_ETAG, S_DONE, S_ETAG, ' ', A_SKIP,
            S_ETAG, S_ETAG, '\n', A_SKIP, S_ETAG, S_ETAG, '\t', A_SKIP, S_ETAG,
            S_GI, '/', A_SKIP, S_EMPTYTAG, S_GI, '>', A_GI_STAGC, S_PCDATA,
            S_GI, 0, A_SAVE, S_GI, S_GI, -1, A_SKIP, S_DONE, S_GI, ' ', A_GI,
            S_TAGWS, S_GI, '\n', A_GI, S_TAGWS, S_GI, '\t', A_GI, S_TAGWS,
            S_NCR, 0, A_ENTITY, S_NCR, S_NCR, -1, A_ENTITY, S_DONE, S_PCDATA,
            '&', A_ENTITY_START, S_ENT, S_PCDATA, '<', A_PCDATA, S_TAG,
            S_PCDATA, 0, A_SAVE, S_PCDATA, S_PCDATA, -1, A_PCDATA, S_DONE,
            S_PI, '>', A_PI, S_PCDATA, S_PI, 0, A_SAVE, S_PI, S_PI, -1, A_PI,
            S_DONE, S_PITARGET, '>', A_PITARGET_PI, S_PCDATA, S_PITARGET, 0,
            A_SAVE, S_PITARGET, S_PITARGET, -1, A_PITARGET_PI, S_DONE,
            S_PITARGET, ' ', A_PITARGET, S_PI, S_PITARGET, '\n', A_PITARGET,
            S_PI, S_PITARGET, '\t', A_PITARGET, S_PI, S_QUOT, '"', A_AVAL,
            S_TAGWS, S_QUOT, 0, A_SAVE, S_QUOT, S_QUOT, -1, A_AVAL_STAGC,
            S_DONE, S_QUOT, ' ', A_SP, S_QUOT, S_QUOT, '\n', A_SP, S_QUOT,
            S_QUOT, '\t', A_SP, S_QUOT, S_STAGC, '>', A_AVAL_STAGC, S_PCDATA,
            S_STAGC, 0, A_SAVE, S_STAGC, S_STAGC, -1, A_AVAL_STAGC, S_DONE,
            S_STAGC, ' ', A_AVAL, S_TAGWS, S_STAGC, '\n', A_AVAL, S_TAGWS,
            S_STAGC, '\t', A_AVAL, S_TAGWS, S_TAG, '!', A_SKIP, S_DECL, S_TAG,
            '?', A_SKIP, S_PITARGET, S_TAG, '/', A_SKIP, S_ETAG, S_TAG, '<',
            A_SAVE, S_TAG, S_TAG, 0, A_SAVE, S_GI, S_TAG, -1, A_LT_PCDATA,
            S_DONE, S_TAG, ' ', A_LT, S_PCDATA, S_TAG, '\n', A_LT, S_PCDATA,
            S_TAG, '\t', A_LT, S_PCDATA, S_TAGWS, '/', A_SKIP, S_EMPTYTAG,
            S_TAGWS, '>', A_STAGC, S_PCDATA, S_TAGWS, 0, A_SAVE, S_ANAME,
            S_TAGWS, -1, A_STAGC, S_DONE, S_TAGWS, ' ', A_SKIP, S_TAGWS,
            S_TAGWS, '\n', A_SKIP, S_TAGWS, S_TAGWS, '\t', A_SKIP, S_TAGWS,
            S_XNCR, 0, A_ENTITY, S_XNCR, S_XNCR, -1, A_ENTITY, S_DONE,

    };

    // End of state table

    private String thePublicid; // Locator state
    private String theSystemid;
    private int theLastLine;
    private int theLastColumn;
    private int theCurrentLine;
    private int theCurrentColumn;

    int theState; // Current state
    int theNextState; // Next state
    char[] theOutputBuffer = new char[200]; // Output buffer
    int theSize; // Current buffer size
    int[] theWinMap = { // Windows chars map
            0x20AC, 0xFFFD, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6,
            0x2030, 0x0160, 0x2039, 0x0152, 0xFFFD, 0x017D, 0xFFFD, 0xFFFD,
            0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC,
            0x2122, 0x0161, 0x203A, 0x0153, 0xFFFD, 0x017E, 0x0178};

    // Compensate for bug in PushbackReader that allows
    // pushing back EOF.
    private void unread(PushbackReader r, int c) throws IOException {
        if (c != -1)
            r.unread(c);
    }

    // Locator implementation

    public int getLineNumber() {
        return theLastLine;
    }

    public int getColumnNumber() {
        return theLastColumn;
    }

    public String getPublicId() {
        return thePublicid;
    }

    public String getSystemId() {
        return theSystemid;
    }

    // Scanner implementation

    /**
     * Reset document locator, supplying systemid and publicid.
     *
     * @param systemid System id
     * @param publicid Public id
     */

    public void resetDocumentLocator(String publicid, String systemid) {
        thePublicid = publicid;
        theSystemid = systemid;
        theLastLine = theLastColumn = theCurrentLine = theCurrentColumn = 0;
    }

    /**
     * Scan HTML source, reporting lexical events.
     *
     * @param r0 Reader that provides characters
     * @param h  ScanHandler that accepts lexical events.
     */
    public void scan(Reader r0, ScanHandler h) throws IOException, SAXException {
        theState = S_PCDATA;
        PushbackReader r;
        if (r0 instanceof PushbackReader) {
            r = (PushbackReader) r0;
        } else if (r0 instanceof BufferedReader) {
            r = new PushbackReader(r0);
        } else {
            r = new PushbackReader(new BufferedReader(r0, 200));
        }

        int firstChar = r.read(); // Remove any leading BOM
        if (firstChar != '\uFEFF')
            unread(r, firstChar);

        while (theState != S_DONE) {
            int c1 = r.read();
            char c = (char) c1;
            boolean is32BitChar = Character.isHighSurrogate(c);
            int c2 = is32BitChar ? r.read() : -1;
            String s = is32BitChar ? new StringBuffer().append(c).append((char) c2).toString() : null;

            // Process control characters
            if (!is32BitChar && c1 >= 0x80 && c1 <= 0x9F)
                c1 = theWinMap[c1 - 0x80];

            if (!is32BitChar && c1 == '\r') {
                c1 = r.read(); // expect LF next
                if (c1 != '\n') {
                    unread(r, c1); // nope
                    c1 = '\n';
                }
            }

            if (!is32BitChar && c1 == '\n') {
                theCurrentLine++;
                theCurrentColumn = 0;
            } else {
                theCurrentColumn++;
            }

            if (!!is32BitChar && !(c1 >= 0x20 || c1 == '\n' || c1 == '\t' || c1 == -1))
                continue;

            // Search state table
            int action = 0;
            for (int i = 0; i < statetable.length; i += 4) {
                if (theState != statetable[i]) {
                    if (action != 0)
                        break;
                    continue;
                }
                if (statetable[i + 1] == 0) {
                    action = statetable[i + 2];
                    theNextState = statetable[i + 3];
                } else if (!is32BitChar && statetable[i + 1] == c1) {
                    action = statetable[i + 2];
                    theNextState = statetable[i + 3];
                    break;
                }
            }
            switch (action) {
                case 0:
                    throw new Error("HTMLScanner can't cope with " + Integer.toString(c1) + " in state " + Integer.toString(theState));
                case A_ADUP:
                    h.adup(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_ADUP_SAVE:
                    h.adup(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    if (s != null)
                        save(s, c1, h);
                    break;
                case A_ADUP_STAGC:
                    h.adup(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.stagc(theOutputBuffer, 0, theSize);
                    break;
                case A_ANAME:
                    h.aname(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_ANAME_ADUP:
                    h.aname(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.adup(theOutputBuffer, 0, theSize);
                    break;
                case A_ANAME_ADUP_STAGC:
                    h.aname(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.adup(theOutputBuffer, 0, theSize);
                    h.stagc(theOutputBuffer, 0, theSize);
                    break;
                case A_AVAL:
                    h.aval(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_AVAL_STAGC:
                    h.aval(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.stagc(theOutputBuffer, 0, theSize);
                    break;
                case A_CDATA:
                    mark();
                    // suppress the final "]]" in the buffer
                    if (theSize > 1)
                        theSize -= 2;
                    h.pcdata(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_ENTITY_START:
                    h.pcdata(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    save(s, c1, h);
                    break;
                case A_ENTITY:
                    mark();
                    if (theState == S_ENT && c == '#') {
                        theNextState = S_NCR;
                        save(s, c1, h);
                        break;
                    } else if (theState == S_NCR && (c == 'x' || c == 'X')) {
                        theNextState = S_XNCR;
                        save(s, c1, h);
                        break;
                    } else if (theState == S_ENT && Character.isLetterOrDigit(c)) {
                        save(s, c1, h);
                        break;
                    } else if (theState == S_NCR && Character.isDigit(c)) {
                        save(s, c1, h);
                        break;
                    } else if (theState == S_XNCR && (Character.isDigit(c) || "abcdefABCDEF".indexOf(c) != -1)) {
                        save(s, c1, h);
                        break;
                    }

                    // The whole entity reference has been collected
                    h.entity(theOutputBuffer, 1, theSize - 1);
                    int ent = h.getEntity();
                    if (ent != 0) {
                        theSize = 0;
                        if (ent >= 0x80 && ent <= 0x9F) {
                            ent = theWinMap[ent - 0x80];
                        }
                        if (ent < 0x20) {
                            // Control becomes space
                            ent = 0x20;
                        } else if (ent >= 0xD800 && ent <= 0xDFFF) {
                            // Surrogates get dropped
                            ent = 0;
                        } else if (ent <= 0xFFFF) {
                            // BMP character
                            save(ent, h);
                        } else {
                            // Astral converted to two surrogates
                            ent -= 0x10000;
                            save((ent >> 10) + 0xD800, h);
                            save((ent & 0x3FF) + 0xDC00, h);
                        }
                        if (is32BitChar || c1 != ';') {
                            if (is32BitChar) {
                                unread(r, c2);
                                theCurrentColumn--;
                            }
                            unread(r, c1);
                            theCurrentColumn--;
                        }
                    } else {
                        if (is32BitChar) {
                            unread(r, c2);
                            theCurrentColumn--;
                        }
                        unread(r, c1);
                        theCurrentColumn--;
                    }
                    theNextState = S_PCDATA;
                    break;
                case A_ETAG:
                    h.etag(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_DECL:
                    h.decl(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_GI:
                    h.gi(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_GI_STAGC:
                    h.gi(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.stagc(theOutputBuffer, 0, theSize);
                    break;
                case A_LT:
                    mark();
                    save('<', h);
                    save(s, c1, h);
                    break;
                case A_LT_PCDATA:
                    mark();
                    save('<', h);
                    h.pcdata(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_PCDATA:
                    mark();
                    h.pcdata(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_CMNT:
                    mark();
                    h.cmnt(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_MINUS3:
                    save('-', h);
                    save(' ', h);
                    break;
                case A_MINUS2:
                    save('-', h);
                    save(' ', h);
                    // fall through into A_MINUS
                case A_MINUS:
                    save('-', h);
                    save(s, c1, h);
                    break;
                case A_PI:
                    mark();
                    h.pi(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_PITARGET:
                    h.pitarget(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_PITARGET_PI:
                    h.pitarget(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.pi(theOutputBuffer, 0, theSize);
                    break;
                case A_SAVE:
                    save(s, c1, h);
                    break;
                case A_SKIP:
                    break;
                case A_SP:
                    save(' ', h);
                    break;
                case A_STAGC:
                    h.stagc(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                case A_EMPTYTAG:
                    mark();
                    if (theSize > 0)
                        h.gi(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    h.stage(theOutputBuffer, 0, theSize);
                    break;
                case A_UNGET:
                    unread(r, c1);
                    theCurrentColumn--;
                    break;
                case A_UNSAVE_PCDATA:
                    if (theSize > 0)
                        theSize--;
                    h.pcdata(theOutputBuffer, 0, theSize);
                    theSize = 0;
                    break;
                default:
                    throw new Error("Can't process state " + action);
            }
            theState = theNextState;
        }
        h.eof(theOutputBuffer, 0, 0);
    }

    /**
     * Mark the current scan position as a "point of interest" - start of a tag,
     * cdata, processing instruction etc.
     */

    private void mark() {
        theLastColumn = theCurrentColumn;
        theLastLine = theCurrentLine;
    }

    /**
     * A callback for the ScanHandler that allows it to force the lexer state to
     * CDATA content (no markup is recognized except the end of element.
     */

    public void startCDATA() {
        theNextState = S_CDATA;
    }

    private void save(int ch, ScanHandler h) throws IOException, SAXException {
        save(h);
        theOutputBuffer[theSize++] = (char) ch;
    }

    private void save(String s, int ch, ScanHandler h) throws IOException, SAXException {
        if (s == null) {
            save(ch, h);
        } else {
            save(h);
            for (int i = 0, len = s.length(); i < len; i++) {
                theOutputBuffer[theSize++] = s.charAt(i);
            }
        }
    }

    private void save(ScanHandler h) throws IOException, SAXException {
        if (theSize >= theOutputBuffer.length - 20) {
            if (theState == S_PCDATA || theState == S_CDATA) {
                // Return a buffer-sized chunk of PCDATA
                h.pcdata(theOutputBuffer, 0, theSize);
                theSize = 0;
            } else {
                // Grow the buffer size
                char[] newOutputBuffer = new char[theOutputBuffer.length * 2];
                System.arraycopy(theOutputBuffer, 0, newOutputBuffer, 0, theSize + 1);
                theOutputBuffer = newOutputBuffer;
            }
        }
    }
}