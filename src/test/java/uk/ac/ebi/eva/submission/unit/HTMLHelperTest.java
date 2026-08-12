package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.eva.submission.util.HTMLHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HTMLHelperTest {

    @Test
    public void testAddLineBreak() {
        String expectedHTML = "<br />";
        String actualHTML = new HTMLHelper().addLineBreak().build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddGap() {
        // adding a gap of 3 lines means you need to add 4 <br /> tags,
        // one for taking the cursor to the new line and the rest for the actual gap
        String expectedHTML = "<br /><br /><br /><br />";
        String actualHTML = new HTMLHelper().addGap(3).build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddText() {
        String expectedHTML = "sample text";
        String actualHTML = new HTMLHelper().addText("sample text").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddTextWithSize() {
        String expectedHTML = "<span style=\"font-size:5px;\">sample text</span>";
        String actualHTML = new HTMLHelper().addTextWithSize("sample text", 5).build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddTextWithColour() {
        String expectedHTML = "<span style=\"color:red;\">sample text</span>";
        String actualHTML = new HTMLHelper().addTextWithColor("sample text", "red").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddBoldText() {
        String expectedHTML = "<b>sample text</b>";
        String actualHTML = new HTMLHelper().addBoldText("sample text").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddLink() {
        String expectedHTML = "<a href=\"abc@example.com\">abc</a>";
        String actualHTML = new HTMLHelper().addLink("abc@example.com", "abc").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddEmailLink() {
        String expectedHTML = "<a href=\"mailto:abc@example.com\">abc@example.com</a>";
        String actualHTML = new HTMLHelper().addEmailLink("abc@example.com", "abc@example.com").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testAddEmailLinkWithSize() {
        String expectedHTML = "<span style=\"font-size:5px;\"> <a href=\"mailto:abc@example.com\">abc@example.com</a> </span>";
        String actualHTML = new HTMLHelper().addEmailLinkWithSize("abc@example.com", "abc@example.com", 5).build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void addBoldTextWithColour() {
        String expectedHTML = "<b><span style=\"color:red;\">sample text</span></b>";
        String actualHTML = new HTMLHelper().addBoldTextWithColor("sample text", "red").build();
        assertEquals(expectedHTML, actualHTML);
    }

    @Test
    public void testMultiLineHTML() {
        String expectedHTML = "Dear John," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "submission id: 12345<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:red;\">FAILED</span></b>" +
                "<br /><br /><br />";
        String actualHTMl = new HTMLHelper()
                .addText("Dear " + "John" + ",")
                .addGap(1)
                .addText("Here is the update for your submission: ")
                .addGap(1)
                .addText("submission id: " + 12345)
                .addLineBreak()
                .addText("Submission Status: " + "UPLOADED")
                .addLineBreak()
                .addText("Result: ")
                .addBoldTextWithColor("FAILED", "red")
                .addGap(2)
                .build();

        assertEquals(expectedHTML, actualHTMl);
    }

}
