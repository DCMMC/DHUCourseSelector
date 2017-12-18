package tk.dcmmcc.utils;

import java.net.URL;

/**
 * 超链接
 * Created by DCMMC on 2017/8/29.
 */
public class HyperlinkURL {
    //超链接的标题
    protected String textTitle;
    //超链接的链接
    protected URL link = null;

    public HyperlinkURL() {

    }

    public HyperlinkURL(String textTitle, URL link) {
        this.textTitle = textTitle;
        this.link = link;
    }

    /**
     * 是否含有超链接
     * @return 如果含有超链接就返回true
     */
    public boolean hasLink() {
        return link != null;
    }

    public String getTextTitle() {
        return textTitle;
    }

    public URL getLink() {
        return link;
    }
}///~
