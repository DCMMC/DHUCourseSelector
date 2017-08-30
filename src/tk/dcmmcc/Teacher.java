package tk.dcmmcc;

import java.net.URL;

/**
 * 封装教师的有关情况
 * Created by DCMMC on 2017/8/28.
 */
public class Teacher extends HyperlinkURL {

    //cannot be instanced by default constructor
    private Teacher() {

    }

    Teacher(String name, URL infoLink) {
        super(name, infoLink);
    }

    public String getName() {
        return super.getTextTitle();
    }

    public URL getInfoLink() {
        return super.getLink();
    }

    public void setInfoLink(URL infoLink) {
        this.link = infoLink;
    }
}///~
