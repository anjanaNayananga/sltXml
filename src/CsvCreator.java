//import com.sun.java.util.jar.pack.Instruction;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by anjanag on 8/5/2017.
 */
public class CsvCreator {
    Element root;

    public void createCsv(File inputFile) {
        try {
            ItemExtractor ix = new ItemExtractor();
            SAXBuilder saxBuilder = new SAXBuilder();

            Document document = null;
            try {
                document = saxBuilder.build(inputFile);
            } catch (JDOMException e) {
                e.printStackTrace();
            }

            System.out.println("Root element :"
                    + document.getRootElement().getName());

            root = document.getRootElement();

            Element doc = new ItemExtractor().getElementFromPath(root, "Body/Document");

            List<Element> catalogList = ix.getElementListFromPath(root, "Body/Document/BusinessDocument/CatalogAction/CatalogActionDetails/CatalogItem");
            Map<String, Element> refIdMap = ix.getRefIdMap(catalogList);
            System.out.println("----------------------------");

//          iterate through all catalog items
            for (Element e : catalogList) {

//              check rules and continue if item is not eligible
                if (!isAvailableByTheRules(e))
                    continue;

                String productId = getProductId(e);
                String parentEntityId = "";
                String entityId = getEntityId(refIdMap, e.getAttributeValue("key"));
                ActionCodeValue actionCode = ActionCodeValue.ADD_PROD;
                String strActionCode = ix.getElementFromPath(e, "ActionCode/Code/Value").getValue();
                actionCode = strActionCode.equals("AddProduct") ? ActionCodeValue.ADD_PROD : ActionCodeValue.CHANGE_PROD;

//              if there is a parent then take the parent entity id
                if (hasBssParent(e, actionCode))
                    parentEntityId = getEntityId(refIdMap, getParentRefKey(e, actionCode));

                String baseName = getEntityType(e);
                String mdmEntityId = getEntityId(refIdMap, getMdmRefKey(e));

                System.out.println(baseName + " " + productId + " " + parentEntityId + " " + mdmEntityId);
                try {
                    File outFile = new File("src\\Results\\" + baseName + ".csv");
                    FileWriter fw ;
                    List<String> entityData = new ArrayList<>();
                    Items itemList = new Items(getItemDataFromRemoteFile(mdmEntityId));

//                  relevant entity data list is taken from corresponding enum class.
                    switch (baseName) {
                        case "Product": {
                            entityData = Arrays.asList(ProductSpecValue.getNames(ProductSpecValue.class));
                            break;
                        }
                        case "Characteristic": {
                            entityData = Arrays.asList(CharacteristicValue.getNames(CharacteristicValue.class));
                            break;
                        }
//                      ToDo : add case to ProductFamily
                    }

//                  if file does not exist, headers will be added
                    if (!outFile.exists()) {
                        fw = new FileWriter("src\\Results\\" + baseName + ".csv");
                        fw.write("PRODUCT_ID ,ENTITY_ID, PARENT_ENTITY_ID,");
                        for (Element item : itemList.getItemList()) {
                            String colName = item.getAttribute("name").getValue();
                            if (entityData.contains(colName)) {
                                System.out.print(colName + ",");
                                fw.write(colName + " ,");
                            }
                        }
                        System.out.println();
                        fw.append("\n");
                    }else {
                        fw = new FileWriter(outFile, true);
                    }

                    fw.append(productId + "," + entityId + "," + parentEntityId + ",");
                    for (Element item : itemList.getItemList()) {
//                      only required items will be taken from item list
                        if (entityData.contains(item.getAttribute("name").getValue())) {
                            String finalVal = "";
                            if (item.getName().equals("Attribute")) {
                                finalVal = item.getValue().toString().trim().replace("\n", "") + ",";
                            } else if (item.getName().equals("MultiValueAttribute")) {
                                List<Element> valueList = item.getChildren();
                                if (valueList != null) {
                                    String values = "";
                                    for (int i = 0; i < valueList.size(); i++) {
                                        values += valueList.get(i).getValue().toString().trim().replace("\n", "");
                                        if (valueList.size() - 1 == i) {
                                            break;
                                        }
                                        values += "|";
                                    }
                                    values += " ,";
                                    finalVal = values;
                                } else {
                                    finalVal = " ,";
                                }
                            }
                            fw.append(finalVal);
                            System.out.print(finalVal);
                        }
                    }
                    System.out.println();
                    fw.append("\n");
                    fw.close();
                } catch (Exception ex) {
                    System.out.println(e);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAvailableByTheRules(Element catalogItem) {
        ItemExtractor itemExtractor = new ItemExtractor();

        //rule 01
        String baseName = itemExtractor.getElementFromPath(catalogItem, "MasterCatalog/RevisionID/BaseName").getValue();
        if (baseName == null || !baseName.equals("BSS_VIEW"))
            return false;

        //rule 02
        Element actionCode = itemExtractor.getElementFromPath(catalogItem, "ActionCode");
        if (actionCode == null)
            return false;

        return true;
    }

    public boolean hasBssParent(Element catalogItem, ActionCodeValue value) {
        ItemExtractor itemExtractor = new ItemExtractor();
        List<Element> relationship = itemExtractor.getAllChildrenFromPath(catalogItem, "RelationshipData");
        switch (value) {
            case ADD_PROD:
                relationship = itemExtractor.getAllChildrenFromPath(catalogItem, "RelationshipData");
                break;
            case CHANGE_PROD:
                relationship = itemExtractor.getAllChildrenFromPath(catalogItem, "ActionLog/ChangedRelationshipData");
                break;
        }
        for (Element e : relationship) {
            if (itemExtractor.getElementFromPath(e, "RelationType").getValue().equals("BELONGS_TO_BSS_PARENT"))
                return true;
        }
        return false;
    }

    public String getEntityId(Map<String, Element> refIdMap, String ref) {
        String entityId = "";
        ItemExtractor ix = new ItemExtractor();
        Element e = refIdMap.get(ref);
        List<Element> atrList = ix.getElementListFromPath(e, "ItemData/Attribute");
        for (Element el : atrList) {
            if (el.getAttributeValue("name").equals("ENTITY_ID")) {
                entityId = ix.getElementFromPath(el, "Value").getValue();
                break;
            }
        }
        return entityId;
    }

    public String getEntityType(Element element) {
        String entityType = "";
        ItemExtractor ix = new ItemExtractor();
        List<Element> atrList = ix.getElementListFromPath(element, "ItemData/Attribute");
        for (Element el : atrList) {
            if (el.getAttributeValue("name").equals("ENTITY_TYPE")) {
                entityType = ix.getElementFromPath(el, "Value").getValue();
                break;
            }
        }
        return entityType;
    }

    public String getProductId(Element element) {
        String prodId = "";
        ItemExtractor ix = new ItemExtractor();
        List<Element> atrList = ix.getElementListFromPath(element, "ItemData/Attribute");
        for (Element el : atrList) {
            if (el.getAttributeValue("name").equals("PRODUCTID")) {
                prodId = ix.getElementFromPath(el, "Value").getValue();
                break;
            }
        }
        return prodId;
    }

    public String getParentRefKey(Element element, ActionCodeValue val) {
        String refId = "";
        ItemExtractor ix = new ItemExtractor();
        List<Element> relationship = new ArrayList<>();
        switch (val) {
            case ADD_PROD:
                relationship = ix.getAllChildrenFromPath(element, "RelationshipData");
                for (Element e : relationship) {
                    if (ix.getElementFromPath(e, "RelationType").getValue().equals("BELONGS_TO_BSS_PARENT")) {
                        refId = ix.getElementFromPath(e, "RelatedItems").getChildren("RelatedItem").get(0).getAttributeValue("referenceKey");
                        break;
                    }
                }
                break;

//          CHANGE_PROD and DEL_PROD are treated as same. ToDo : Recheck the validity of DEL_PROD
            case CHANGE_PROD:
                relationship = ix.getAllChildrenFromPath(element, "ActionLog/ChangedRelationshipData");
                for (Element e : relationship) {
                    if (ix.getElementFromPath(e, "RelationType").getValue().equals("BELONGS_TO_BSS_PARENT")) {
                        List<Element> relatedItem = e.getChildren("RelatedItem");
                        for (Element rel : relatedItem) {
                            if (rel.getAttributeValue("action").equals("add"))
                                refId = rel.getAttributeValue("referenceKey");
                        }
                        break;
                    }
                }
                break;
        }

        return refId;
    }

    public String getMdmRefKey(Element element) {
        String refId = "";
        ItemExtractor ix = new ItemExtractor();
        List<Element> relationship = ix.getAllChildrenFromPath(element, "RelationshipData");
        for (Element e : relationship) {
            if (ix.getElementFromPath(e, "RelationType").getValue().equals("BSS_BELONGS_TO_MDM")) {
                refId = ix.getElementFromPath(e, "RelatedItems").getChildren().get(0).getAttributeValue("referenceKey");
                break;
            }
        }
        return refId;
    }

    public List<Element> getItemDataFromRemoteFile(String entityId) {
        List<Element> itemData = new ArrayList<>();
        File folder = new File("src\\Entities");
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                String fileName = listOfFiles[i].getName();
                String[] split0 = fileName.split("\\.");
                String[] split = split0[0].split("_");
                String tempEntity = split[3] + "_" + split[4];
                if (tempEntity.equals(entityId)) {
                    ItemExtractor ix = new ItemExtractor();
                    SAXBuilder saxBuilder = new SAXBuilder();

                    Document document = null;
                    try {
                        document = saxBuilder.build(listOfFiles[i]);
                    } catch (JDOMException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Root element of entity file:"
                            + document.getRootElement().getName());

                    itemData = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(1).getChildren();
                    break;
                }
            }
        }
        return itemData;
    }

    public void readFile() {
        File folder = new File("src\\Views");
        File[] listOfFiles = folder.listFiles();
        for (File f : listOfFiles) {
            createCsv(f);
        }
    }

    /*public boolean createDeltafile(Element e, String productId, String baseName) {
        Items itemList = new Items(new ItemExtractor().getAllChildrenFromPath(e, "ActionLog/Action"));
        Path pathToFile = Paths.get("src\\" + baseName + "_delta.csv");
        try {
            File outFile = new File("src\\" + baseName + "_delta.csv");
            FileWriter fw;
            if (outFile.exists()) {
                fw = new FileWriter(outFile, true);

            } else {
                fw = new FileWriter("src\\" + baseName + "_delta.csv");
            }
            String row = "Edit ," + productId + " ,";
            for (Element item : itemList.getItemList()) {
                if (item.getAttribute("changed") != null && item.getAttribute("changed").getValue().equals("true")) {
                    if (item.getName().equals("Attribute")) {
                        String entry = "";
                        entry += item.getAttribute("name").getValue() + " = ";
                        entry += item.getChild("OriginalValue").getValue().toString().trim().replace("\n", "") + "|";
                        entry += item.getChild("Value").getValue().toString().trim().replace("\n", "") + " ,";
                        row += entry;
                    } else if (item.getName().equals("MultiValueAttribute")) {
                        List<Element> valueList = item.getChild("ValueList").getChildren();
                        List<Element> originalValueList = item.getChild("OriginalValueList").getChildren();
                        String entry = item.getAttribute("name").getValue() + " = ";
                        if (valueList != null) {
                            for (int j = 0; j < valueList.size(); j++) {
                                entry += "(";
                                entry += originalValueList.get(j).getValue().toString().trim().replace("\n", "") + "|";
                                entry += valueList.get(j).getValue().toString().trim().replace("\n", "");
                                entry += ")";
                            }
                            entry += " ,";
                            row += entry;
                        }
                    }
                }
            }
            if (!row.equals("Edit ," + productId + " ,")) {
                fw.append(row + "\n");
            }
            System.out.println();
            fw.close();
        } catch (Exception ex) {
            System.out.println(e);
        }
        return true;
    }*/
}

enum ActionCodeValue {
    ADD_PROD, CHANGE_PROD, DEL_PROD
}

enum ProductSpecValue {
    PRODUCT_NAME, PRODUCT_DESC, VALID_FROM, VALID_TO, IS_PARAMETRIC;

    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }
}

enum CharacteristicValue {
    CHARACTERISTIC_NAME, IS_MANDATORY, CHARACTERISTIC_VALUE, LOOK_UP_REQUIRED, DISPLAY_POSITION, CHARACTERISTIC_UOM;

    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }
}