import org.jdom2.Element;

import java.util.List;

/**
 * Created by anjanag on 8/5/2017.
 */
public class Items {
    private List<Element> itemList;

    public Items(List<Element> itemList) {
        this.itemList = itemList;
    }

    public List<Element> getItemList() {
        return itemList;
    }

    public void setItemList(List<Element> itemList) {
        this.itemList = itemList;
    }
    public Element getItemAt(int index){
        return itemList.get(index);
    }
}
