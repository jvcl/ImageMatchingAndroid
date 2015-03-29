package au.com.innovus.labelmatching;

/**
 * Created by jorge on 29/03/15.
 */
public class Item {

    private String title;
    private String origin;
    private String category;
    private String description;
    public  Item(){
    }

    @Override
    public String toString() {
        return title + " " + origin + " " + category;
    }

    public String getTitle() {
        return title;
    }

    public String getOrigin() {
        return origin;
    }

    public String getCategory() {
        return category;
    }
    public String getDescription() {
        return description;
    }
}
