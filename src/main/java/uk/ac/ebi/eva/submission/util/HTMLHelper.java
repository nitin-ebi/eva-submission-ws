package uk.ac.ebi.eva.submission.util;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HTMLHelper {
    private final StringBuilder htmlBuilder;

    public HTMLHelper() {
        htmlBuilder = new StringBuilder();
    }

    private static String esc(String s) {
        return s == null ? "" : StringEscapeUtils.escapeHtml4(s);
    }

    public HTMLHelper addLineBreak() {
        htmlBuilder.append("<br />");
        return this;
    }

    public HTMLHelper addGap(int count) {
        htmlBuilder.append(IntStream.range(0, count + 1).boxed().map(i -> "<br />").collect(Collectors.joining("")));
        return this;
    }

    public HTMLHelper addText(String text) {
        htmlBuilder.append(esc(text));
        return this;
    }

    public HTMLHelper addTextWithSize(String text, int size) {
        htmlBuilder.append("<span style=\"font-size:" + size + "px;\">" + esc(text) + "</span>");
        return this;
    }

    public HTMLHelper addTextWithColor(String text, String color) {
        htmlBuilder.append("<span style=\"color:" + esc(color) + ";\">" + esc(text) + "</span>");
        return this;
    }

    public HTMLHelper addBoldText(String text) {
        htmlBuilder.append("<b>" + esc(text) + "</b>");
        return this;
    }

    public HTMLHelper addLink(String url, String text) {
        htmlBuilder.append("<a href=\"" + url + "\">" + esc(text) + "</a>");
        return this;
    }

    public HTMLHelper addEmailLink(String email, String text) {
        htmlBuilder.append("<a href=\"mailto:" + email + "\">" + esc(text) + "</a>");
        return this;
    }

    public HTMLHelper addEmailLinkWithSize(String email, String text, int size) {
        htmlBuilder.append("<span style=\"font-size:" + size + "px;\"> <a href=\"mailto:" + email + "\">" + esc(text) + "</a> </span>");
        return this;
    }

    public HTMLHelper addBoldTextWithColor(String text, String color) {
        htmlBuilder.append("<b><span style=\"color:" + esc(color) + ";\">" + esc(text) + "</span></b>");
        return this;
    }

    public String build() {
        return htmlBuilder.toString();
    }
}
