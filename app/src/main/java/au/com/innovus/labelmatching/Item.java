package au.com.innovus.labelmatching;

/**
 * Created by jorge on 29/03/15.
 */
public class Item {

    private String title;
    private String origin;
    private String category;
    public  Item(){
    }

    @Override
    public String toString() {
        return title + " " + origin + " " + category;
    }
}
