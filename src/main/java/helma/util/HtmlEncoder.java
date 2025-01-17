/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.util;

import java.util.*;

/**
 * This is a utility class to encode special characters and do formatting
 * for HTML output.
 */
public final class HtmlEncoder {
    // HTML block tags need to suppress automatic newline to <br>
    // conversion around them to look good. However, they differ
    // in how many newlines around them should ignored. These sets
    // help to treat each tag right in newline conversion.
    static final HashSet internalTags = new HashSet();
    static final HashSet blockTags = new HashSet();
    static final HashSet semiBlockTags = new HashSet();

    static {
        // actual block level elements
        semiBlockTags.add("address");
        blockTags.add("article");
        blockTags.add("aside");
        blockTags.add("blockquote");
        blockTags.add("details");
        blockTags.add("dialog");
        blockTags.add("dd");
        semiBlockTags.add("div");
        blockTags.add("dl");
        blockTags.add("dt");
        blockTags.add("fieldset");
        blockTags.add("figcaption");
        blockTags.add("figure");
        blockTags.add("footer");
        blockTags.add("form");
        blockTags.add("h1");
        blockTags.add("h2");
        blockTags.add("h3");
        blockTags.add("h4");
        blockTags.add("h5");
        blockTags.add("h6");
        blockTags.add("header");
        blockTags.add("hgroup");
        blockTags.add("hr");
        blockTags.add("li");
        blockTags.add("main");
        blockTags.add("nav");
        blockTags.add("ol");
        blockTags.add("p");
        blockTags.add("pre");
        blockTags.add("section");
        semiBlockTags.add("table");
        blockTags.add("ul");

        // additional semi/block elements
        blockTags.add("br");
        blockTags.add("center");
        semiBlockTags.add("dir");
        blockTags.add("frameset");
        blockTags.add("isindex");
        blockTags.add("td");
        semiBlockTags.add("th");

        internalTags.add("menu");
        internalTags.add("noframes");
        internalTags.add("noscript");
        internalTags.add("tbody");
        internalTags.add("tfoot");
        internalTags.add("thead");
        internalTags.add("tr");
    }

    // set of tags that are always empty
    static final HashSet emptyTags = new HashSet();

    static {
        // actual empty elements
        emptyTags.add("area");
        emptyTags.add("base");
        emptyTags.add("br");
        emptyTags.add("col");
        emptyTags.add("embed");
        emptyTags.add("hr");
        emptyTags.add("img");
        emptyTags.add("input");
        emptyTags.add("keygen");
        emptyTags.add("link");
        emptyTags.add("meta");
        emptyTags.add("param");
        emptyTags.add("source");
        emptyTags.add("track");
        emptyTags.add("wbr");

        // Obsolete or deprecated
        emptyTags.add("basefont");
        emptyTags.add("frame");
        emptyTags.add("isindex");
    }

    static final byte TAG_NAME = 0;
    static final byte TAG_SPACE = 1;
    static final byte TAG_ATT_NAME = 2;
    static final byte TAG_ATT_VAL = 3;

    static final byte TEXT = 0;
    static final byte SEMIBLOCK = 1;
    static final byte BLOCK = 2;
    static final byte INTERNAL = 3;

    static final String newLine = System.getProperty("line.separator");

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of {@code <}, {@code >} and {@code &} are encoded to HTML entities.
     */
    public final static String encode(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        // try to make stringbuffer large enough from the start
        StringBuffer ret = new StringBuffer(Math.round(l * 1.4f));

        encode(str, ret, false, null);

        return ret.toString();
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of {@code <}, {@code >} and {@code &} are encoded to HTML entities.
     */
    public final static void encode(String str, StringBuffer ret) {
        encode(str, ret, false, null);
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of {@code <}, {@code >} and {@code &} are encoded to HTML entities.
     *
     *  @param str the string to encode
     *  @param ret the string buffer to encode to
     *  @param paragraphs if true use p tags for paragraphs, otherwise just use br's
     *  @param allowedTags a set containing the names of allowed tags as strings. All other
     *                     tags will be escaped
     */
    public final static void encode(String str, StringBuffer ret,
                                    boolean paragraphs, Set allowedTags) {
        if (str == null) {
            return;
        }

        int l = str.length();

        // where to insert the <p> tag in case we want to create a paragraph later on
        int paragraphStart = ret.length();

        // what kind of element/text are we leaving and entering?
        // this is one of TEXT|SEMIBLOCK|BLOCK|INTERNAL
        // depending on this information, we decide whether and how to insert
        // paragraphs and line breaks. "entering" a tag means we're at the '<'
        // and exiting means we're at the '>', not that it's a start or close tag.
        byte entering = TEXT;
        byte exiting = TEXT;

        Stack openTags = new Stack();

        // are we currently within a < and a > that consitute some kind of tag?
        // we use tag balancing to know whether we are inside a tag (and should
        // pass things through unchanged) or outside (and should encode stuff).
        boolean insideTag = false;

        // are we inside an HTML tag?
        boolean insideHtmlTag = false;
        boolean insideCloseTag = false;
        byte htmlTagMode = TAG_NAME;

        // if we are inside a <code> tag, we encode everything to make
        // documentation work easier
        boolean insideCodeTag = false;
        boolean insidePreTag = false;

        // are we within a Helma <% macro %> tag? We treat macro tags and
        // comments specially, since we can't rely on tag balancing
        // to know when we leave a macro tag or comment.
        boolean insideMacroTag = false;

        // are we inside an HTML comment?
        boolean insideComment = false;

        // the quotation mark we are in within an HTML or Macro tag, if any
        char htmlQuoteChar = '\u0000';
        char macroQuoteChar = '\u0000';

        // number of newlines met since the last non-whitespace character
        int linebreaks = 0;

        // did we meet a backslash escape?
        boolean escape = false;

        boolean triggerBreak = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            // step one: check if this is the beginning of an HTML tag, comment or
            // Helma macro.
            if (c == '<') {
                if (i < (l - 2)) {
                    if (!insideMacroTag && ('%' == str.charAt(i + 1))) {
                        // this is the beginning of a Helma macro tag
                        if (!insideCodeTag) {
                            insideMacroTag = insideTag = true;
                            macroQuoteChar = '\u0000';
                        }
                    } else if ('!' == str.charAt(i + 1)) {
                        // the beginning of an HTML comment or !doctype?
                        if (!insideCodeTag) {
                            if (str.regionMatches(i + 2, "--", 0, 2)) {
                                insideComment = insideTag = true;
                            } else if (str.regionMatches(true, i+2, "doctype", 0, 7)) {
                                insideHtmlTag = insideTag = true;
                            }
                        }
                    } else if (!insideTag) {
                        // check if this is a HTML tag.
                        insideCloseTag = ('/' == str.charAt(i + 1));
                        int tagStart = insideCloseTag ? (i + 2) : (i + 1);
                        int j = tagStart;

                        while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                            j++;

                        if ((j > tagStart) && (j < l)) {
                            String tagName = str.substring(tagStart, j).toLowerCase();

                            if ("code".equals(tagName) && insideCloseTag &&
                                    insideCodeTag) {
                                insideCodeTag = false;
                            }

                            if (((allowedTags == null) || allowedTags.contains(tagName)) && !insideCodeTag) {
                                insideHtmlTag = insideTag = true;
                                htmlQuoteChar = '\u0000';
                                htmlTagMode = TAG_NAME;

                                exiting = entering;
                                entering = TEXT;

                                if (internalTags.contains(tagName)) {
                                    entering = INTERNAL;
                                } else if (blockTags.contains(tagName)) {
                                    entering = BLOCK;
                                } else if (semiBlockTags.contains(tagName)) {
                                    entering = paragraphs ? BLOCK : SEMIBLOCK;
                                }

                                if (entering > 0) {
                                    triggerBreak = !insidePreTag;
                                }

                                if (insideCloseTag) {
                                    int t = openTags.search(tagName);

                                    if (t == -1) {
                                        i = j;
                                        insideHtmlTag = insideTag = false;

                                        continue;
                                    } else if (t > 1) {
                                        for (int k = 1; k < t; k++) {
                                            Object tag = openTags.pop();
                                            if (!emptyTags.contains(tag)) {
                                                ret.append("</");
                                                ret.append(tag);
                                                ret.append(">");
                                            }
                                        }
                                    }

                                    openTags.pop();
                                } else {
                                    openTags.push(tagName);
                                }

                                if ("code".equals(tagName) && !insideCloseTag) {
                                    insideCodeTag = true;
                                }

                                if ("pre".equals(tagName)) {
                                    insidePreTag = !insideCloseTag;
                                }
                            }
                        }
                    }
                } // if (i < l-2)
            }

            if ((triggerBreak || linebreaks > 0) && !Character.isWhitespace(c)) {

                if (!insideTag) {
                    exiting = entering;
                    entering = TEXT;
                    if (exiting >= SEMIBLOCK) {
                        paragraphStart = ret.length();
                    }
                }

                if (entering != INTERNAL && exiting != INTERNAL) {
                    int swallowBreaks = 0;
                    /*if (paragraphs &&
                          (entering != BLOCK || exiting != BLOCK) &&
                          (exiting < BLOCK) &&
                          (linebreaks > 1) &&
                          paragraphStart < ret.length()) {
                        ret.insert(paragraphStart, "<p>");
                        ret.append("</p>");
                        swallowBreaks = 2;
                    }*/

                    // treat entering a SEMIBLOCK as entering a TEXT
                    int _entering = entering == SEMIBLOCK ? TEXT : entering;
                    for (int k = linebreaks-1; k>=0; k--) {
                        if (k >= swallowBreaks && k >= _entering && k >= exiting) {
                            ret.append("<br class='helma-format'/>");
                        }
                        ret.append(newLine);
                    }
                    if (exiting >= SEMIBLOCK || linebreaks > 1) {
                        paragraphStart = ret.length();
                    }

                }

                linebreaks = 0;
                triggerBreak = false;
            }

            switch (c) {
                case '<':

                    if (insideTag) {
                        ret.append('<');
                    } else {
                        ret.append("&lt;");
                    }

                    break;

                case '&':

                    // check if this is an HTML entity already,
                    // in which case we pass it though unchanged
                    if ((i < (l - 3)) && !insideCodeTag) {
                        // is this a numeric entity?
                        if (str.charAt(i + 1) == '#') {
                            int j = i + 2;

                            while ((j < l) && Character.isDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&");

                                break;
                            }
                        } else {
                            int j = i + 1;

                            while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&");

                                break;
                            }
                        }
                    }

                    // we didn't reach a break, so encode as entity unless inside a tag
                    if (insideMacroTag) {
                        ret.append('&');
                    } else {
                        ret.append("&amp;");
                    }
                    break;

                case '\\':
                    ret.append(c);

                    if (insideTag && !insideComment) {
                        escape = !escape;
                    }

                    break;

                case '"':
                case '\'':
                    ret.append(c);

                    if (!insideComment) {
                        // check if the quote is escaped
                        if (insideMacroTag) {
                            if (escape) {
                                escape = false;
                            } else if (macroQuoteChar == c) {
                                macroQuoteChar = '\u0000';
                            } else if (macroQuoteChar == '\u0000') {
                                macroQuoteChar = c;
                            }
                        } else if (insideHtmlTag) {
                            if (escape) {
                                escape = false;
                            } else if (htmlQuoteChar == c) {
                                htmlQuoteChar = '\u0000';
                                htmlTagMode = TAG_SPACE;
                            } else if (htmlQuoteChar == '\u0000') {
                                htmlQuoteChar = c;
                            }
                        }
                    }

                    break;

                case '\n':
                    if (insideTag || insidePreTag) {
                        ret.append('\n');
                    } else {
                        linebreaks++;
                    }

                    break;
                case '\r':
                    if (insideTag || insidePreTag) {
                        ret.append('\r');
                    }
                    break;

                case '>':

                    // For Helma macro tags and comments, we overrule tag balancing,
                    // i.e. we don't require that '<' and '>' be balanced within
                    // macros and comments. Rather, we check for the matching closing tag.
                    if (insideComment) {
                        ret.append('>');
                        insideComment = !((str.charAt(i - 2) == '-') &&
                                        (str.charAt(i - 1) == '-'));
                    } else if (insideMacroTag) {
                        ret.append('>');
                        insideMacroTag = !((str.charAt(i - 1) == '%') &&
                                         (macroQuoteChar == '\u0000'));
                    } else if (insideHtmlTag) {
                        ret.append('>');

                        // only leave HTML tag if quotation marks are balanced
                        // within that tag.
                        insideHtmlTag = htmlQuoteChar != '\u0000';

                        // Check if this is an empty tag so we don't generate an
                        // additional </close> tag.
                        if (str.charAt(i - 1) == '/') {
                            // this is to avoid misinterpreting tags like
                            // <a href=http://foo/> as empty
                            if (!openTags.empty() && htmlTagMode != TAG_ATT_VAL &&
                                                     htmlTagMode != TAG_ATT_NAME) {
                                openTags.pop();
                            }
                        }

                        exiting = entering;
                        if (exiting > 0) {
                           triggerBreak = !insidePreTag;
                        }

                    } else {
                        ret.append("&gt;");
                    }

                    // check if we still are inside any kind of tag
                    insideTag = insideComment || insideMacroTag || insideHtmlTag;
                    insideCloseTag = insideTag;

                    break;

                default:

                    if (insideHtmlTag && !insideCloseTag) {
                        switch(htmlTagMode) {
                            case TAG_NAME:
                                if (!Character.isLetterOrDigit(c)) {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                            case TAG_SPACE:
                                if (Character.isLetterOrDigit(c)) {
                                    htmlTagMode = TAG_ATT_NAME;
                                }
                                break;
                            case TAG_ATT_NAME:
                                if (c == '=') {
                                    htmlTagMode = TAG_ATT_VAL;
                                } else if (c == ' ') {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                            case TAG_ATT_VAL:
                                if (Character.isWhitespace(c) && htmlQuoteChar == '\u0000') {
                                    htmlTagMode = TAG_SPACE;
                                }
                                break;
                        }
                    }

                    ret.append(c);
                    escape = false;
            }
        }

        // if tags were opened but not closed, close them.
        int o = openTags.size();

        if (o > 0) {
            for (int k = 0; k < o; k++) {
                Object tag = openTags.pop();
                if (!emptyTags.contains(tag)) {
                    ret.append("</");
                    ret.append(tag);
                    ret.append(">");
                }
            }
        }

        // add remaining newlines we may have collected
        int swallowBreaks = 0;
        /*if (paragraphs && entering < BLOCK) {
            ret.insert(paragraphStart, "<p>");
            ret.append("</p>");
            swallowBreaks = 2;
        }*/

        if (linebreaks > 0) {
            for (int i = linebreaks-1; i>=0; i--) {
                if (i >= swallowBreaks && i > exiting) {
                    ret.append("<br class='helma-format' />");
                }
                ret.append(newLine);
            }
        }
    }

    /**
     *
     */
    public final static String encodeFormValue(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, false);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeFormValue(String str, StringBuffer ret) {
        encodeAll(str, ret, false);
    }

    /**
     *
     */
    public final static String encodeAll(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, true);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret) {
        encodeAll(str, ret, true);
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret, boolean encodeNewline) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;");
                    break;

                case '>':
                    ret.append("&gt;");
                    break;

                case '&':
                    ret.append("&amp;");
                    break;

                case '"':
                    ret.append("&quot;");
                    break;

                case '\n':
                    if (encodeNewline) {
                        ret.append("<br class='helma-format' />");
                    }
                    ret.append('\n');
                    break;

                default:
                    ret.append (c);
            }
        }
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public final static String encodeXml(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();
        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));
        encodeXml(str, ret);
        return ret.toString();
    }

    /**
     *
     *
     * @param str ...
     * @param ret ...
     */
    public final static void encodeXml(String str, StringBuffer ret) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;");
                    break;

                case '>':
                    ret.append("&gt;");
                    break;

                case '&':
                    ret.append("&amp;");
                    break;

                case '"':
                    ret.append("&quot;");
                    break;

                case '\'':
                    ret.append("&#39;");
                    break;

                default:

                    if (c < 0x20) {
                        // sort out invalid XML characters below 0x20 - all but 0x9, 0xA and 0xD.
                        // The trick is an adaption of java.lang.Character.isSpace().
                        if (((((1L << 0x9) | (1L << 0xA) | (1L << 0xD)) >> c) & 1L) != 0) {
                            ret.append(c);
                        }
                    } else {
                        ret.append(c);
                    }
            }
        }
    }

    // for testing...
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++)
            System.err.println(encode(args[i]));
    }
}
 // end of class
