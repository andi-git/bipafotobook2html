package net.bipiafotobook2html;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class BipaFotobook2Html {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private final Path fotobookFile;

    private final Xpp3Dom fotobookXpp3Dom;

    public BipaFotobook2Html(Path fotobookFile) throws IOException, XmlPullParserException {
        this.fotobookFile = fotobookFile;
        System.out.println("fotobook-file is: " + fotobookFile);
        mustExist(fotobookFile);
        System.out.println("fotobook-data is: " + getDataDirAbsolute());
        mustExist(getDataDirAbsolute());
        createXpp3Dom();
        fotobookXpp3Dom = createXpp3Dom();
    }

    private void mustExist(Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("file " + path + " doesn't exist");
        }
    }

    private String getSimpleFileName() {
        return fotobookFile.getFileName().toString();
    }

    private Path getDataDirAbsolute() {
        return fotobookFile.getParent().resolve(getSimpleFileName().replaceAll("\\.", "_") + "-Dateien");
    }

    private String getDataDirRelative() {
        return getSimpleFileName().replaceAll("\\.", "_") + "-Dateien";
    }

    private Xpp3Dom createXpp3Dom() throws IOException, XmlPullParserException {
        Xpp3Dom xpp3Dom;
        Path copy = Paths.get(String.valueOf(fotobookFile.getParent()) + "/work.xml");
        Files.copy(fotobookFile, copy, StandardCopyOption.REPLACE_EXISTING);
        String content = new String(Files.readAllBytes(fotobookFile), CHARSET);
        content = content.replaceAll("<!\\[CDATA\\[<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">", "");
        content = content.replaceAll("\\]\\]>", "");
        Files.write(copy, content.getBytes(CHARSET));
        xpp3Dom = Xpp3DomBuilder.build(Files.newInputStream(copy), CHARSET.toString());
        Files.delete(copy);
        return xpp3Dom;
    }

    public Path convertToHtml() throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title>Converted BiPa-Photobook</title></head><body bgcolor='#c0c0c0'>");
        scan(fotobookXpp3Dom, content);
        content.append("</body></html>");
        Path indexhtml = Paths.get(String.valueOf(fotobookFile.getParent()) + "/index.html");
        Files.write(indexhtml, content.toString().
                replaceAll("\u00c4", "&Auml;").
                replaceAll("\u00e4", "&auml;").
                replaceAll("\u00d6", "&Ouml;").
                replaceAll("\u00f6", "&ouml;").
                replaceAll("\u00dc", "&Uuml;").
                replaceAll("\u00fc", "&uuml;").
                replaceAll("\u00df", "&szlig;").
                getBytes(CHARSET));
        return indexhtml;
    }

    private void scan(Xpp3Dom xpp3Dom, StringBuilder content) {
        for (Xpp3Dom child : xpp3Dom.getChildren()) {
            String name = child.getName();
            if ("area".equals(name)) {
                if ("TEXTAREA".equals(child.getAttribute("areatype"))) {
                    content.append(getParagraphOfTextArea(child.getChild("text")));
                } else if ("IMAGEAREA".equals(child.getAttribute("areatype"))) {
                    String image = getImageOfImageArea(child);
                    if (!"".equals(image)) {
                        String imagePath = getDataDirRelative() + "/" + image;
                        content.append("<a href='").
                                append(imagePath).
                                append("'><img src='").
                                append(imagePath).
                                append("' width='100' height='100' /></a>");
                    }
                }
            } else {
                scan(child, content);
            }
        }
    }

    private String getParagraphOfTextArea(Xpp3Dom xpp3Dom) {
        String string = "";
        for (Xpp3Dom child : xpp3Dom.getChildren()) {
            if ("p".equals(child.getName())) {
                string += child.toString().replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
            } else {
                string += getParagraphOfTextArea(child);
            }
        }
        return string;
    }

    private String getImageOfImageArea(Xpp3Dom xpp3Dom) {
        Xpp3Dom image = xpp3Dom.getChild("image");
        if (image != null && image.getAttribute("filename") != null) {
            return image.getAttribute("filename");
        }
        return "";
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {
        System.out.println("start " + BipaFotobook2Html.class.getName());
        if (args == null || args.length < 1) {
            System.out.println("need file of photobook as first prameter");
            System.exit(0);
        }
        Path file = Paths.get(args[0]);
        BipaFotobook2Html bipaFotobook2Html = new BipaFotobook2Html(file);
        System.out.println("fotobook converted -> see : " + bipaFotobook2Html.convertToHtml());
    }
}
