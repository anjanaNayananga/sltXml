import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anjanag on 8/5/2017.
 */
public class ItemExtractor {
    public Element getElementFromPath(Element root, String path){
        String []tags = path.split("/");
        Element childEl=root;
        for(String s:tags){
            childEl=childEl.getChild(s);
        }
        return childEl;
    }

    public List<Element> getElementListFromPath(Element root, String path){
        String []tags = path.split("/");
        Element childEl=root;
        for(int i=0;i<tags.length-1;i++){
            if(childEl!=null)
                childEl=childEl.getChild(tags[i]);
        }
        if(childEl!=null && childEl.getChildren(tags[tags.length-1])!=null){
            return childEl.getChildren(tags[tags.length-1]) ;
        }else{
            return new ArrayList<Element>();
        }
    }

    public List<Element> getElementListFromPathNS(Element root, String path, Namespace namespase){
        String []tags = path.split("/");
        Element childEl=root;
        for(int i=0;i<tags.length-1;i++){
            if(childEl!=null)
                childEl=childEl.getChild(tags[i], namespase);
        }
        if(childEl!=null && childEl.getChildren(tags[tags.length-1])!=null){
            return childEl.getChildren(tags[tags.length-1]) ;
        }else{
            return new ArrayList<Element>();
        }
    }

    public List<Element> getAllChildrenFromPath(Element root, String path){
        String []tags = path.split("/");
        Element childEl=root;
        for(int i=0;i<tags.length;i++){
            if(childEl!=null)
                childEl=childEl.getChild(tags[i]);
        }
        if(childEl!=null && childEl.getChildren()!=null){
            return childEl.getChildren() ;
        }else{
            return new ArrayList<Element>();
        }
    }
    public Map<String,Element> getRefIdMap(List<Element> elementList){
        Map<String,Element> refIdMap = new HashMap<>();
        for(Element e : elementList){
            refIdMap.put(e.getAttributeValue("key"), e);
        }
        return refIdMap;
    }
}
