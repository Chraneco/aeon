/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.ultimatesoftware.aeon.extensions.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.Transform;
import org.apache.logging.log4j.util.Strings;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Outputs events as rows in an HTML table on an HTML page.
 * <p>
 * Appenders using this layout should have their encoding set to UTF-8 or UTF-16, otherwise events containing non ASCII
 * characters could result in corrupted log files.
 * </p>
 */
@Plugin(name = "AeonHtmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class AeonHtmlLayout extends AbstractStringLayout {

    /**
     * Default font family: {@value}.
     */
    public static final String DEFAULT_FONT_FAMILY = "arial,sans-serif";

    private static final long serialVersionUID = 1L;
    private static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final String REGEXP = Strings.LINE_SEPARATOR.equals("\n") ? "\n" : Strings.LINE_SEPARATOR + "|\n";
    private static final String REGEXHTML = "((?:&quot;)|(?:\"))?(?:(?:(http)(s)?(:\\/\\/))|(www\\.))+([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b(?:(?!&lt;)(?!&gt;)(?!&quot;)(?:[-a-zA-Z0-9@:%_\\+\\[\\].;~#?\\\\\\/&=]))*)((?:\\s)|(?:&quot;)|(?:\")*)";
    private static final String HTMLAPPENDAGE = "$1<a href=\"http$3://$5$6\">http$3://$5$6</a>$7";
    private static final String DEFAULT_TITLE = "Log4j Log Messages";
    private static final String DEFAULT_CONTENT_TYPE = "text/html";
    private static final String SEMICOLON_CHARSET = "; charset=";
    private static final String CLOSE_TD = "</td>";
    private static final String CLOSE_TD_TR = "</td></tr>";


    // Print no location info by default
    private final boolean locationInfo;
    private final String title;
    private final String contentType;
    private final String font;
    private final String fontSize;
    private final String headerSize;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private AeonHtmlLayout(final boolean locationInfo, final String title, final String contentType, final Charset charset,
                           final String font, final String fontSize, final String headerSize) {
        super(charset);
        this.locationInfo = locationInfo;
        this.title = title;
        this.contentType = addCharsetToContentType(contentType);
        this.font = font;
        this.fontSize = fontSize;
        this.headerSize = headerSize;
    }

    /**
     * Create an HTML Layout.
     *
     * @param locationInfo If "true", location information will be included. The default is false.
     * @param title        The title to include in the file header. If none is specified the default title will be used.
     * @param contentType  The content type. Defaults to "text/html".
     * @param charset      The character set to use. If not specified, the default will be used.
     * @param fontSize     The font size of the text.
     * @param font         The font to use for the text.
     * @return An HTML Layout.
     */
    @PluginFactory
    public static AeonHtmlLayout createLayout(
            @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
            @PluginAttribute(value = "title", defaultString = DEFAULT_TITLE) final String title,
            @PluginAttribute("contentType") String contentType,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute("fontSize") String fontSize,
            @PluginAttribute(value = "fontName", defaultString = DEFAULT_FONT_FAMILY) final String font) {
        final FontSize fs = FontSize.getFontSize(fontSize);
        fontSize = fs.getFontSize();
        final String headerSize = fs.larger().getFontSize();
        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE + SEMICOLON_CHARSET + charset;
        }
        return new AeonHtmlLayout(locationInfo, title, contentType, charset, font, fontSize, headerSize);
    }

    /**
     * Creates an HTML Layout using the default settings.
     *
     * @return an HTML Layout.
     */
    public static AeonHtmlLayout createDefaultLayout() {
        return newBuilder().build();
    }

    /**
     * Function creates and returns a new instance of PluginBuilder.
     *
     * @return new instance of PluginBuilder().
     */
    @PluginBuilderFactory
    public static PluginBuilder newBuilder() {
        return new PluginBuilder();
    }

    private String addCharsetToContentType(final String contentType) {
        if (contentType == null) {
            return DEFAULT_CONTENT_TYPE + SEMICOLON_CHARSET + getCharset();
        }
        return contentType.contains("charset") ? contentType : contentType + SEMICOLON_CHARSET + getCharset();
    }

    /**
     * Format as a String.
     *
     * @param event The Logging Event.
     * @return A String containing the LogEvent as HTML.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder sbuf = getStringBuilder();

        sbuf.append(Strings.LINE_SEPARATOR).append("<tr>").append(Strings.LINE_SEPARATOR);

        sbuf.append("<td>");
        sbuf.append(dateTimeFormatter.format(Instant.ofEpochMilli(event.getTimeMillis())));
        sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);

        final String escapedThread = Transform.escapeHtmlTags(event.getThreadName());
        sbuf.append("<td title=\"").append(escapedThread).append(" thread\">");
        sbuf.append(escapedThread);
        sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);

        sbuf.append("<td title=\"Level\">");
        if (event.getLevel().equals(Level.DEBUG)) {
            sbuf.append("<font color=\"#339933\">");
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
            sbuf.append("</font>");
        } else if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            sbuf.append("<font color=\"#993300\"><strong>");
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
            sbuf.append("</strong></font>");
        } else {
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
        }
        sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);

        String escapedLogger = Transform.escapeHtmlTags(event.getLoggerName());
        if (escapedLogger.isEmpty()) {
            escapedLogger = LoggerConfig.ROOT;
        }
        sbuf.append("<td title=\"").append(escapedLogger).append(" logger\">");
        sbuf.append(escapedLogger);
        sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);

        if (locationInfo) {
            final StackTraceElement element = event.getSource();
            sbuf.append("<td>");
            sbuf.append(Transform.escapeHtmlTags(element.getFileName()));
            sbuf.append(':');
            sbuf.append(element.getLineNumber());
            sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);
        }

        sbuf.append("<td title=\"Message\">");
        sbuf.append(Transform.escapeHtmlTags(event.getMessage().getFormattedMessage()).replaceAll(REGEXHTML, HTMLAPPENDAGE).replaceAll(REGEXP, "<br />"));

        Object[] parameters = event.getMessage().getParameters();
        if (parameters != null && parameters.length > 0 && parameters[0] instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) parameters[0];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", baos);
                String data = Base64.getEncoder().encodeToString(baos.toByteArray());
                sbuf.append("<img src=\"data:image/png;base64,").append(data).append("\" />");
            } catch (IOException e) {
                sbuf.append("Could not decode screenshot.");
            }
        }

        sbuf.append(CLOSE_TD).append(Strings.LINE_SEPARATOR);
        sbuf.append("</tr>").append(Strings.LINE_SEPARATOR);

        if (event.getContextStack() != null && !event.getContextStack().isEmpty()) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Nested Diagnostic Context\">");
            sbuf.append("NDC: ").append(Transform.escapeHtmlTags(event.getContextStack().toString()));
            sbuf.append(CLOSE_TD_TR).append(Strings.LINE_SEPARATOR);
        }

        if (event.getContextData() != null && !event.getContextData().isEmpty()) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Mapped Diagnostic Context\">");
            sbuf.append("MDC: ").append(Transform.escapeHtmlTags(event.getContextData().toString()));
            sbuf.append(CLOSE_TD_TR).append(Strings.LINE_SEPARATOR);
        }

        final Throwable throwable = event.getThrown();
        if (throwable != null) {
            sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\">");
            appendThrowableAsHtml(throwable, sbuf);
            sbuf.append(CLOSE_TD_TR).append(Strings.LINE_SEPARATOR);
        }

        return sbuf.toString();
    }

    /**
     * Gets the type of the Content and returns a string.
     *
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    private void appendThrowableAsHtml(final Throwable throwable, final StringBuilder sbuf) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch (final RuntimeException ex) {
            // Ignore the exception.
        }
        pw.flush();
        final LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        final ArrayList<String> lines = new ArrayList<>();
        try {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (final InterruptedIOException ex) {
            Thread.currentThread().interrupt();
            lines.add(ex.toString());
        } catch (final IOException ex) {
            lines.add(ex.toString());
        }
        boolean first = true;
        for (final String line : lines) {
            if (!first) {
                sbuf.append(TRACE_PREFIX);
            } else {
                first = false;
            }
            sbuf.append(Transform.escapeHtmlTags(line));
            sbuf.append(Strings.LINE_SEPARATOR);
        }
    }

    /**
     * Returns appropriate HTML headers.
     *
     * @return The header as a byte array.
     */
    @Override
    public byte[] getHeader() {
        final StringBuilder sbuf = new StringBuilder();
        sbuf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" ");
        sbuf.append("\"http://www.w3.org/TR/html4/loose.dtd\">");
        sbuf.append(Strings.LINE_SEPARATOR);
        sbuf.append("<html>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<head>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<meta charset=\"").append(getCharset()).append("\"/>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<title>").append(title).append("</title>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<style type=\"text/css\">").append(Strings.LINE_SEPARATOR);
        sbuf.append("<!--").append(Strings.LINE_SEPARATOR);
        sbuf.append("body, table {font-family:").append(font).append("; font-size: ");
        sbuf.append(headerSize).append(";}").append(Strings.LINE_SEPARATOR);
        sbuf.append("th {background: #336699; color: #FFFFFF; text-align: left;}").append(Strings.LINE_SEPARATOR);
        sbuf.append("-->").append(Strings.LINE_SEPARATOR);
        sbuf.append("</style>").append(Strings.LINE_SEPARATOR);
        sbuf.append("</head>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">").append(Strings.LINE_SEPARATOR);
        sbuf.append("<hr size=\"1\" noshade=\"noshade\">").append(Strings.LINE_SEPARATOR);
        sbuf.append("Log session start time ").append(new java.util.Date()).append("<br>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<br>").append(Strings.LINE_SEPARATOR);
        sbuf.append(
                "<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">");
        sbuf.append(Strings.LINE_SEPARATOR);
        sbuf.append("<tr>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<th>Time</th>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<th>Thread</th>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<th>Level</th>").append(Strings.LINE_SEPARATOR);
        sbuf.append("<th>Logger</th>").append(Strings.LINE_SEPARATOR);
        if (locationInfo) {
            sbuf.append("<th>File:Line</th>").append(Strings.LINE_SEPARATOR);
        }
        sbuf.append("<th>Message</th>").append(Strings.LINE_SEPARATOR);
        sbuf.append("</tr>").append(Strings.LINE_SEPARATOR);
        return sbuf.toString().getBytes(getCharset());
    }

    /**
     * Returns the appropriate HTML footers.
     *
     * @return the footer as a byet array.
     */
    @Override
    public byte[] getFooter() {
        // Return empty byte array so we can log continuously rather than have closing tags ruin formatting
        return new byte[0];
    }


    /**
     * Possible font sizes.
     */
    public enum FontSize {
        SMALLER("smaller"), XXSMALL("xx-small"), XSMALL("x-small"), SMALL("small"), MEDIUM("medium"), LARGE("large"),
        XLARGE("x-large"), XXLARGE("xx-large"), LARGER("larger");

        private final String size;

        FontSize(final String size) {
            this.size = size;
        }

        /**
         * Static function that returns the font size given a string as input size.
         *
         * @param size string input of size of font to get.
         * @return if legal size, it returns a fontSize, else it returns enum small.
         */
        public static FontSize getFontSize(final String size) {
            for (final FontSize fontSize : values()) {
                if (fontSize.size.equals(size)) {
                    return fontSize;
                }
            }
            return SMALL;
        }

        /**
         * Gets the current font size as a string.
         *
         * @return the font size as a string.
         */
        public String getFontSize() {
            return size;
        }

        /**
         * Function returns a font size larger than the ordinal size.
         *
         * @return If the ordinal size is not the largest, it returns the font size+1, else the largest size.
         */
        public FontSize larger() {
            return this.ordinal() < XXLARGE.ordinal() ? FontSize.values()[this.ordinal() + 1] : this;
        }
    }

    /**
     * The PluginBuilder class.
     */
    public static class PluginBuilder implements org.apache.logging.log4j.core.util.Builder<AeonHtmlLayout> {

        @PluginBuilderAttribute
        private boolean locationInfo = false;

        @PluginBuilderAttribute
        private String title = DEFAULT_TITLE;

        @PluginBuilderAttribute
        private String contentType = null; // defer default value in order to use specified charset

        @PluginBuilderAttribute
        private Charset charset = StandardCharsets.UTF_8;

        @PluginBuilderAttribute
        private FontSize fontSize = FontSize.SMALL;

        @PluginBuilderAttribute
        private String fontName = DEFAULT_FONT_FAMILY;

        private PluginBuilder() {
        }

        /**
         * Function returns a PluginBuilder with the provided location info as a boolean.
         *
         * @param locationInfo a boolean of locationInfo.
         * @return the new location info.
         */
        public PluginBuilder withLocationInfo(final boolean locationInfo) {
            this.locationInfo = locationInfo;
            return this;
        }

        /**
         * Function returns a PluginBuilder with the provided title info as a string.
         *
         * @param title a string representing a title.
         * @return the PluginBuilder with the new title.
         */
        public PluginBuilder withTitle(final String title) {
            this.title = title;
            return this;
        }

        /**
         * Function returns a PluginBuilder with the provided contentType info as a string.
         *
         * @param contentType a string representing the content type.
         * @return the PluginBuilder with the new content type.
         */
        public PluginBuilder withContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Function returns a PluginBuilder with the provided title info as a string.
         *
         * @param charset a Charset representing a set of chars.
         * @return the PluginBuilder with the new char set.
         */
        public PluginBuilder withCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * Function returns a PluginBuilder with the provided title info as a string.
         *
         * @param fontSize a FontSize representing the font size.
         * @return the PluginBuilder with the new font size.
         */
        public PluginBuilder withFontSize(final FontSize fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        /**
         * Function returns a PluginBuilder with the provided title info as a string.
         *
         * @param fontName a string representing a fontName.
         * @return the PluginBuilder with the new fontName.
         */
        public PluginBuilder withFontName(final String fontName) {
            this.fontName = fontName;
            return this;
        }

        @Override
        public AeonHtmlLayout build() {
            if (contentType == null) {
                contentType = DEFAULT_CONTENT_TYPE + SEMICOLON_CHARSET + charset;
            }
            return new AeonHtmlLayout(locationInfo, title, contentType, charset, fontName, fontSize.getFontSize(),
                    fontSize.larger().getFontSize());
        }
    }
}
