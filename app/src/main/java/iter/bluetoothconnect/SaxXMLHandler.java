package iter.bluetoothconnect;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jfernandez on 26/10/2016.
 */

public class SaxXMLHandler extends DefaultHandler {

    public ArrayList<String> items;
    private String name="";
    //private Float value = 0.0f;
    private String buffer ="";

    public SaxXMLHandler(){
        items = new ArrayList<String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
       // super.startElement(uri, localName, qName, attributes);
        name = qName;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        buffer = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (!buffer.isEmpty()){
            String str = qName +":"+ buffer;
            items.add(str);
            buffer = "";
        }
    }
}
