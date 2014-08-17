package com.xuchun.mybatis.generator.plugin;

import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mybatis-generator提供的插件接口,配置在genrator.xml中
 * <p>Author: desheng.tu</p>
 * <p>Date: 2014年8月5日</p>
 */
public class MyPlugin extends PluginAdapter implements Plugin {

    static final String BASE_MAPPER = "com.ysdai.core.dao.BaseMapper";
    static final String BASE_MODEL = "com.ysdai.model.po.BaseModel";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * 该方法在生成model文件前调用
     * <p/>
     * 因为我的项目所有model都继承自BaseModel,所以这里添加继承,然后去掉积累中以有的属性(id, create_time, modify_tiem)
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }

    /**
     * 该方法在生成java接口文件前调用
     * <p/>
     * 因为我的项目所有的mapper都继承自BaseMapper,所以这里添加继承,然后去掉所有原来的方法
     * <p/>
     * 因为我的项目命名方式:
     * model:  xxxModel
     * mapper: xxxMapper
     * 而自动生成的mapper名字为:xxxModelMapper,所以这里反射修改类名,去掉里面的Model字符串
     */
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        Iterator it = interfaze.getMethods().iterator();
        String javaTypeName = "";

        while (it.hasNext()) {
            Method method  = (org.mybatis.generator.api.dom.java.Method) it.next();
            if (method.getName().equals("updateByPrimaryKey")) {
                method.setName("update");
            } else if (method.getName().equals("deleteByPrimaryKey")) {
                method.setName("delete");
            } else if (method.getName().equals("selectByPrimaryKey")) {
                javaTypeName = method.getReturnType().getFullyQualifiedName();
                method.setName("selectByUkid");
            } else if (method.getName().equals("insert")) {

            } else {
               it.remove();
            }
        }

        FullyQualifiedJavaType returnType = new FullyQualifiedJavaType("List<"+javaTypeName+">");
        FullyQualifiedJavaType parameType = new FullyQualifiedJavaType("PageParameter");
        Method method = new Method("selectList");
        method.setReturnType(returnType);
        method.setVisibility(JavaVisibility.PUBLIC);
        method.addParameter(new Parameter(parameType,"record"));
        interfaze.addMethod(method);


        return super.clientGenerated(interfaze, topLevelClass, introspectedTable);
    }

    /**
     * 该方法在生成xml文件前调用
     */
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        //反射修改mapper.xml的文件名,去掉里面的Model字符串
        replaceModelString(sqlMap, "fileName");

        return super.sqlMapGenerated(sqlMap, introspectedTable);
    }

    /**
     * 该方法在sqlMapGenerated方法前调用
     * 对xml的修改可以在这里处理
     * <p/>
     * 我这里的处理是:
     * 1.去掉namespace中的Model字符串(你不一定需要)
     * 2.改方法名(如:selectByPrimaryKey 改为 findById),或者删除不用的方法(如:insertSelective方法)
     * 3.添加新方法(如:批量插入,批量删除,mysql分页查询等等)
     */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement mapper = document.getRootElement();

        //反射修改mapper.xml中namespace属性值,去掉里面的Model字符串
        Attribute namespace = mapper.getAttributes().get(0);
        replaceModelString(namespace, "value");

        //需要修改方法名或删除的方法
        Iterator<Element> eleIterator = mapper.getElements().iterator();
        while (eleIterator.hasNext()) {
            XmlElement xmlEle = (XmlElement) eleIterator.next();
            Attribute idAttr = getEleAttr(xmlEle, "id");

            if (idAttr.getValue().equals("selectByPrimaryKey")) {
                setFieldValue(idAttr, "value", "selectByUkid");
            } else if (idAttr.getValue().equals("deleteByPrimaryKey")) {
                setFieldValue(idAttr, "value", "delete");
            } else if (idAttr.getValue().equals("insertSelective")) {
                eleIterator.remove();
            } else if (idAttr.getValue().equals("updateByPrimaryKeySelective")) {
                eleIterator.remove();
            } else if (idAttr.getValue().equals("updateByPrimaryKey")) {
                setFieldValue(idAttr, "value", "update");
            }
            this.delJdbcType(xmlEle);
        }

        //添加新方法
        addAllMethod(mapper, introspectedTable, "selectList");

        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    private void delJdbcType(XmlElement ele) {
        Iterator<Element> eleIterator = ele.getElements().iterator();
        while (eleIterator.hasNext()) {
            Object xmlEle = eleIterator.next();
            if (xmlEle instanceof XmlElement) {
                this.delJdbcType((XmlElement) xmlEle);
            }
            //去除语句中的jdbc
            if (xmlEle instanceof TextElement && ((TextElement) xmlEle).getContent().indexOf("jdbcType") > -1) {
                TextElement textEle = ((TextElement) xmlEle);
                Pattern pattern = Pattern.compile(".jdbcType.+?\\}");
                Matcher matcher = pattern.matcher(textEle.getContent());
                while (matcher.find()) {
                    setFieldValue(textEle, "content", textEle.getContent().replace(matcher.group(), "}"));
                }
            }
        }
        //去除resultMap中的jdbc
        if (ele.getAttributes() != null) {
            for (int i = 0; i < ele.getAttributes().size(); i++) {
                if (ele.getAttributes().get(i).getName().equals("jdbcType"))
                    ele.getAttributes().remove(i--);
            }
        }
    }


    private void addFindByPageCountMethod(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("select");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("resultType", "java.lang.Integer"));
        ele.addAttribute(new Attribute("parameterType", "PageInfo"));

        ele.addElement(new TextElement("select count(1) from ("));

        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "findByPageSql"));
        ele.addElement(include);

        ele.addElement(new TextElement(") temp "));

        mapper.addElement(ele);
    }

    private void addFindByPageMethod(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("select");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("resultMap", "BaseResultMap"));
        ele.addAttribute(new Attribute("parameterType", "PageInfo"));

        ele.addElement(new TextElement("select temp.* from ("));

        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "findByPageSql"));
        ele.addElement(include);

        ele.addElement(new TextElement(") temp limit #{begin}, #{end}"));

        mapper.addElement(ele);
    }

    private void addFindByPageSql(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("sql");
        ele.addAttribute(new Attribute("id", id));

        ele.addElement(new TextElement("select a.* from " + tablename(table) + " a where 1=1 "));

        Iterator<IntrospectedColumn> iter = table.getAllColumns().iterator();
        while (iter.hasNext()) {
            IntrospectedColumn column = iter.next();
            if (column.getActualColumnName().equals("create_time")
                    || column.getActualColumnName().equals("modify_time")) {
                continue;
            }

            XmlElement iftag = new XmlElement("if");
            iftag.addAttribute(new Attribute("test", column.getJavaProperty("paramMap.") + " != null"));

            String columnStr = "a." + MyBatis3FormattingUtilities.getEscapedColumnName(column);
            String paramStr = MyBatis3FormattingUtilities.getParameterClause(column, "paramMap.");
            iftag.addElement(new TextElement("and " + columnStr + " = " + paramStr));

            ele.addElement(iftag);
        }

        ele.addElement(new TextElement("order by a." + pkname(table) + " desc"));

        mapper.addElement(ele);
    }

    private void addLoadMethod(XmlElement mapper, IntrospectedTable table, String id) {
        if (table.getPrimaryKeyColumns().size() != 1) {
            System.err.println("load : ignore complex primary key.");
            return;
        }

        XmlElement ele = new XmlElement("select");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("resultMap", "BaseResultMap"));

        ele.addElement(new TextElement("select "));

        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Base_Column_List"));
        ele.addElement(include);

        ele.addElement(new TextElement("from " + tablename(table) + " where " + pkname(table) + " in "));

        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "ids"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("open", "("));
        foreach.addAttribute(new Attribute("separator", ","));
        foreach.addAttribute(new Attribute("close", ")"));
        foreach.addElement(new TextElement("#{item}"));

        ele.addElement(foreach);

        mapper.addElement(ele);
    }

    private void addDeletesMethod(XmlElement mapper, IntrospectedTable table, String id) {
        if (table.getPrimaryKeyColumns().size() != 1) {
            System.err.println("deletes : ignore complex primary key.");
            return;
        }

        XmlElement ele = new XmlElement("delete");
        ele.addAttribute(new Attribute("id", id));

        ele.addElement(new TextElement("delete from " + tablename(table) + " where " + pkname(table) + " in "));

        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "ids"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("open", "("));
        foreach.addAttribute(new Attribute("separator", ","));
        foreach.addAttribute(new Attribute("close", ")"));
        foreach.addElement(new TextElement("#{item}"));

        ele.addElement(foreach);

        mapper.addElement(ele);
    }

    private void addInsertsMethod(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("insert");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("parameterType", "java.util.List"));

        StringBuilder insertClause = new StringBuilder("insert into ");
        insertClause.append(tablename(table) + " (");

        StringBuilder valuesClause = new StringBuilder("(");

        Iterator<IntrospectedColumn> iter = table.getAllColumns().iterator();
        while (iter.hasNext()) {
            IntrospectedColumn introspectedColumn = iter.next();
            if (introspectedColumn.isIdentity()) {
                continue;
            }

            insertClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            valuesClause.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn, "item."));
            if (iter.hasNext()) {
                insertClause.append(", ");
                valuesClause.append(", ");
            }
        }

        ele.addElement(new TextElement(insertClause.append(")").toString()));
        ele.addElement(new TextElement("values"));

        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "list"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("separator", ","));
        foreach.addElement(new TextElement(valuesClause.append(")").toString()));

        ele.addElement(foreach);

        mapper.addElement(ele);
    }

    private void addCountMethod(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("select");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("resultType", "java.lang.Integer"));

        ele.addElement(new TextElement("select count(1) from " + tablename(table)));

        mapper.addElement(ele);
    }

    private void addAllMethod(XmlElement mapper, IntrospectedTable table, String id) {
        XmlElement ele = new XmlElement("select");
        ele.addAttribute(new Attribute("id", id));
        ele.addAttribute(new Attribute("resultMap", "PageParameter "));

        ele.addElement(new TextElement("select "));

        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Base_Column_List"));
        ele.addElement(include);

        ele.addElement(new TextElement("from " + tablename(table)));
        ele.addElement(new TextElement("where ${where} ${sort}"));
        mapper.addElement(ele);
    }

    private Attribute getEleAttr(XmlElement ele, String attrName) {
        for (Attribute attr : ele.getAttributes()) {
            if (attr.getName().equals(attrName)) {
                return attr;
            }
        }
        return null;
    }

    private void replaceModelString(Object target, String fieldName) {
        try {
            Field field = getField(target, fieldName);
            String old = field.get(target).toString();
            field.set(target, old.replace("Model", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target, fieldName);
            field.set(target, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Field getField(Object target, String fieldName) throws NoSuchFieldException {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String tablename(IntrospectedTable table) {
        return table.getFullyQualifiedTableNameAtRuntime();
    }

    private String pkname(IntrospectedTable table) {
        return MyBatis3FormattingUtilities.getEscapedColumnName(table.getPrimaryKeyColumns().get(0));
    }
}
