package com.xuchun.mybatis.generator.plugin;

import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.TopLevelClass;
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
public class EcmPlugin extends PluginAdapter implements Plugin {

    static final String BASE_MAPPER = "com.ysdai.core.dao.BaseMapper";
    static final String BASE_MODEL = "com.ysdai.model.po.BaseModel";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }
    
    /**
     * 该方法在生成model文件前调用
     * 
     * 因为我的项目所有model都继承自BaseModel,所以这里添加继承,然后去掉积累中以有的属性(id, create_time, modify_tiem)
     * 
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }
    
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
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
     * 
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
                setFieldValue(idAttr, "value", "findById");
            } else if (idAttr.getValue().equals("deleteByPrimaryKey")) {
                setFieldValue(idAttr, "value", "delete");
            } else if (idAttr.getValue().equals("insertSelective")) {
                eleIterator.remove();
            } else if (idAttr.getValue().equals("updateByPrimaryKeySelective")) {
                setFieldValue(idAttr, "value", "update");
            } else if (idAttr.getValue().equals("updateByPrimaryKey")) {
                eleIterator.remove();
            }


            //删除jdbcType
            delJdbcType(xmlEle);
        }

        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    private void delJdbcType(XmlElement ele) {
        Iterator<Element> eleIterator = ele.getElements().iterator();
        while (eleIterator.hasNext()) {
            Object xmlEle = eleIterator.next();
            if (xmlEle instanceof XmlElement) {
                this.delJdbcType((XmlElement)xmlEle);
            }
            //去除语句中的jdbc
            if (xmlEle instanceof TextElement && ((TextElement) xmlEle).getContent().indexOf("jdbcType") > -1) {
                TextElement textEle =  ((TextElement) xmlEle);
                Pattern pattern = Pattern.compile(".jdbcType.+?\\}");
                Matcher matcher = pattern.matcher(textEle.getContent());
                while(matcher.find()){
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
