/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.languagetool.rules.patterns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.danielnaber.languagetool.JLanguageTool;
import de.danielnaber.languagetool.Language;

/**
 * Loads {@link PatternRule}s from an XML file.
 * 
 * @author Daniel Naber
 */
public class PatternRuleLoader extends DefaultHandler {

  private List<PatternRule> rules;

  public PatternRuleLoader() {
  }

  public List<PatternRule> getRules(String filename) throws ParserConfigurationException,
      SAXException, IOException {
    PatternRuleHandler handler = new PatternRuleHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(JLanguageTool.getAbsoluteFile(filename), handler);
    rules = handler.getRules();
    return rules;
  }

  /** Testing only. */
  public static void main(String[] args) throws ParserConfigurationException, SAXException,
      IOException {
    PatternRuleLoader prg = new PatternRuleLoader();
    List<PatternRule> l = prg.getRules("rules/de/grammar.xml");
    System.out.println(l);
  }

}

class PatternRuleHandler extends XMLRuleHandler {

  private String id;
  private boolean caseSensitive = false;
  private boolean stringRegExp = false;
  private boolean stringNegation = false;
  private boolean stringInflected = false;
  private String posToken;
  private boolean posNegation = false;
  private boolean posRegExp = false;
  
  private Language language;
  private String description;
  private String ruleGroupId;
  private String ruleGroupDescription;
  
  private String exceptionPosToken;
  private boolean exceptionStringRegExp = false;
  private boolean exceptionStringNegation = false;
  private boolean exceptionStringInflected = false;
  private boolean exceptionPosNegation = false;
  private boolean exceptionPosRegExp = false;
  private boolean exceptionSet = false;
  
  private List<Element> elementList = null;
  
  private int startPositionCorrection = 0;
  private int endPositionCorrection = 0;
  private int skipPos = 0;

  // ===========================================================
  // SAX DocumentHandler methods
  // ===========================================================

  @SuppressWarnings("unused")
  public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
      throws SAXException {
    if (qName.equals("rules")) {
      String languageStr = attrs.getValue("lang");
      language = Language.getLanguageforShortName(languageStr);
      if (language == null) {
        throw new SAXException("Unknown language '" + languageStr + "'");
      }
    } else if (qName.equals("rule")) {
      id = attrs.getValue("id");
      if (inRuleGroup && id == null)
        id = ruleGroupId;
      description = attrs.getValue("name");
      if (inRuleGroup && description == null)
        description = ruleGroupDescription;
      correctExamples = new ArrayList<String>();
      incorrectExamples = new ArrayList<String>();
    } else if (qName.equals("pattern")) {
      pattern = new StringBuffer();
      inPattern = true;
      if (attrs.getValue("mark_from") != null)
        startPositionCorrection = Integer.parseInt(attrs.getValue("mark_from"));
      if (attrs.getValue("mark_to") != null)
        endPositionCorrection = Integer.parseInt(attrs.getValue("mark_to"));
      if (attrs.getValue("case_sensitive") != null
          && attrs.getValue("case_sensitive").equals("yes"))
        caseSensitive = true;
    } else if (qName.equals("token")) {
      inToken = true;
      if (attrs.getValue("negate") != null) {
        stringNegation = attrs.getValue("negate").equals("yes");
      }
      if (attrs.getValue("inflected") != null) {
        stringInflected = attrs.getValue("inflected").equals("yes");
      }
      if (attrs.getValue("skip") != null) {
        skipPos = Integer.parseInt(attrs.getValue("skip"));
      }
      elements = new StringBuffer();
      if (elementList == null) // lazy init
      {
        elementList = new ArrayList<Element>();
      }
      // POSElement creation
      if (attrs.getValue("postag") != null) {
        posToken = attrs.getValue("postag");
        if (attrs.getValue("postag_regexp") != null) {
          posRegExp = attrs.getValue("postag_regexp").equals("yes");
        }
        if (attrs.getValue("negate_pos") != null) {
          posNegation = attrs.getValue("negate_pos").equals("yes");
        }
        if (elementList == null) { // lazy init
          elementList = new ArrayList<Element>();
        }
      }
      if (attrs.getValue("regexp") != null) {
        stringRegExp = attrs.getValue("regexp").equals("yes");
      }

    } else if (qName.equals("exception")) {
      inException = true;
      exceptionSet = true;
      exceptions = new StringBuffer();

      if (attrs.getValue("negate") != null) {
        exceptionStringNegation = attrs.getValue("negate").equals("yes");
      }
      if (attrs.getValue("inflected") != null) {
        exceptionStringInflected = attrs.getValue("inflected").equals("yes");
      }
      if (attrs.getValue("postag") != null) {
        exceptionPosToken = attrs.getValue("postag");
        if (attrs.getValue("postag_regexp") != null) {
          exceptionPosRegExp = attrs.getValue("postag_regexp").equals("yes");
        }
        if (attrs.getValue("negate_pos") != null) {
          exceptionPosNegation = attrs.getValue("negate_pos").equals("yes");
        }
      }
      if (attrs.getValue("regexp") != null) {
        exceptionStringRegExp = attrs.getValue("regexp").equals("yes");
      }

    } else if (qName.equals("example") && attrs.getValue("type").equals("correct")) {
      inCorrectExample = true;
      correctExample = new StringBuffer();
    } else if (qName.equals("example") && attrs.getValue("type").equals("incorrect")) {
      inIncorrectExample = true;
      incorrectExample = new StringBuffer();
    } else if (qName.equals("message")) {
      inMessage = true;
      message = new StringBuffer();
    } else if (qName.equals("rulegroup")) {
      ruleGroupId = attrs.getValue("id");
      ruleGroupDescription = attrs.getValue("name");
      inRuleGroup = true;
    } else if (qName.equals("suggestion") && inMessage) {
      message.append("<suggestion>");
    } else if (qName.equals("marker") && inCorrectExample) {
      correctExample.append("<marker>");
    } else if (qName.equals("marker") && inIncorrectExample) {
      incorrectExample.append("<marker>");
    }
  }

  @SuppressWarnings("unused")
  public void endElement(String namespaceURI, String sName, String qName) {
    if (qName.equals("rule")) {
      PatternRule rule = new PatternRule(id, language, elementList, description, message.toString());
      rule.setStartPositionCorrection(startPositionCorrection);
      rule.setEndPositionCorrection(endPositionCorrection);
      startPositionCorrection = 0;
      endPositionCorrection = 0;
      rule.setCorrectExamples(correctExamples);
      rule.setIncorrectExamples(incorrectExamples);
      rule.setCaseSensitive(caseSensitive);
      caseSensitive = false;
      rules.add(rule);
      if (elementList != null) {
        elementList.clear();
      }
    } else if (qName.equals("exception")) {
      inException = false;
    } else if (qName.equals("token")) {
      Element stringElement = new Element(elements.toString(), caseSensitive, stringRegExp,
          stringInflected);
      stringElement.setNegation(stringNegation);
      if (skipPos != 0) {
        stringElement.setSkipNext(skipPos);
        skipPos = 0;
      }
      if (posToken != null) {
        stringElement.setPosElement(posToken, posRegExp, posNegation);
        posToken = null;
      }
      if (exceptionSet) {
        stringElement.setStringException(exceptions.toString(), caseSensitive,
            exceptionStringRegExp, exceptionStringInflected, exceptionStringNegation);
        exceptionSet = false;
      }
      if (exceptionPosToken != null) {
        stringElement.setPosException(exceptionPosToken, exceptionPosRegExp, exceptionPosNegation);
        exceptionPosToken = null;
      }
      elementList.add(stringElement);
      stringNegation = false;
      stringInflected = false;
      posNegation = false;
      posRegExp = false;
      inToken = false;
      stringRegExp = false;

      exceptionStringNegation = false;
      exceptionStringInflected = false;
      exceptionPosNegation = false;
      exceptionPosRegExp = false;
      exceptionStringRegExp = false;

    } else if (qName.equals("pattern")) {
      inPattern = false;
    } else if (qName.equals("example")) {
      if (inCorrectExample) {
        correctExamples.add(correctExample.toString());
      } else if (inIncorrectExample) {
        incorrectExamples.add(incorrectExample.toString());
      }
      inCorrectExample = false;
      inIncorrectExample = false;
      correctExample = new StringBuffer();
      incorrectExample = new StringBuffer();
    } else if (qName.equals("message")) {
      inMessage = false;
    } else if (qName.equals("rulegroup")) {
      inRuleGroup = false;
    } else if (qName.equals("suggestion") && inMessage) {
      message.append("</suggestion>");
    } else if (qName.equals("marker") && inCorrectExample) {
      correctExample.append("</marker>");
    } else if (qName.equals("marker") && inIncorrectExample) {
      incorrectExample.append("</marker>");
    }
  }

  public void characters(char buf[], int offset, int len) {
    String s = new String(buf, offset, len);
    if (inException) {
      exceptions.append(s);
    } else if (inToken) {
      elements.append(s);
    } else if (inCorrectExample) {
      correctExample.append(s);
    } else if (inIncorrectExample) {
      incorrectExample.append(s);
    } else if (inMessage) {
      message.append(s);
    }

  }

}
