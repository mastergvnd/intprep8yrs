package com.testing;

/*import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
//import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.hyperion.calcmgr.beans.RunTimeVariable;
import com.hyperion.calcmgr.beans.VariableBean;
//import com.hyperion.calcmgr.beans.VariableParserHelper;
//import com.hyperion.calcmgr.beans.VariableReplaceHelper;
import com.hyperion.calcmgr.common.ArrayListCache;
import com.hyperion.calcmgr.common.CHashList;
import com.hyperion.calcmgr.common.CHashMapCache;
import com.hyperion.calcmgr.common.ConfigurationManager;
import com.hyperion.calcmgr.common.DebugFileUtils;
import com.hyperion.calcmgr.common.StringBufferCache;
import com.hyperion.calcmgr.common.Util;
import com.hyperion.calcmgr.common.enums.FileExtension;
import com.hyperion.calcmgr.excp.ParsingException;
import com.hyperion.calcmgr.expressions.Argument;
import com.hyperion.calcmgr.expressions.Function;
//import com.hyperion.calcmgr.groovy.GroovyInstance;
import com.hyperion.calcmgr.interfaces.ExpressionTranslator;
import com.hyperion.calcmgr.interfaces.ICellFixer;
import com.hyperion.calcmgr.interfaces.IDryRunExporter;
import com.hyperion.calcmgr.interfaces.IFixResolver;
import com.hyperion.calcmgr.interfaces.IScriptParser;
import com.hyperion.calcmgr.parser.Import;
import com.hyperion.calcmgr.parser.cellcheck.CellCheck;
import com.hyperion.calcmgr.parser.cellcheck.CellCheck.BadCalcModeDef;
import com.hyperion.calcmgr.parser.cellcheck.CellCheck.CellDef;
import com.hyperion.calcmgr.parser.cellcheck.CellCheck.FixDef;
import com.hyperion.calcmgr.parser.cellcheck.IdempotentChecker;
import com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParse;
import com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseConstants;
import com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants;
import com.hyperion.calcmgr.plugin.essbase.calcscript.ParseException;
import com.hyperion.calcmgr.plugin.essbase.calcscript.SimpleNode;
import com.hyperion.calcmgr.plugin.essbase.calcscript.Token;
import com.hyperion.calcmgr.plugin.essbase.calcscript.TokenMgrError;
import com.hyperion.calcmgr.plugin.essbase.java.JavaParser;
import com.hyperion.calcmgr.plugin.essbase.java.JavaParserConstants;

import java.math.BigInteger;

*//**
 * Created by IntelliJ IDEA. User: rhenness Date: Jun 17, 2004 Time: 2:50:54 PM
 * To change this template use Options | File Templates.
 *//*
public class CalcScriptParseHelper implements IScriptParser {
    public static final String PARM_PREFIX = "parm";
    public static final char VAR_START = '{';
    public static final char DTP_START = '[';
    public static final char ESSBASE_START = '&';

    public static final VariableTypes SUBSTITUTION_VARS = VariableTypes.SUBSTITUTION_VARS;
    public static final VariableTypes DTP_VARS = VariableTypes.DTP_VARS;
    public static final VariableTypes ESSBASE_VARS = VariableTypes.ESSBASE_VARS;

    private String[] variablesUsed = null;
    private String[] parmsUsed = null;
    private String[] macrosUsed = null;
    private SimpleNode rootNode = null; // Root node for the CalcScript parsed
//    private boolean hasGroovy = false;
                                        // tree
    public static final HashMap<String, Boolean> functionOptions = new HashMap<String, Boolean>();
    public static int CLBPARSE_TOKEN_COMMA = 0;
    static {
        functionOptions.put("ADD", Boolean.TRUE);
        functionOptions.put("AFTER", Boolean.TRUE);
        functionOptions.put("ALL", Boolean.TRUE);
        functionOptions.put("AVERAGE", Boolean.TRUE);
        functionOptions.put("BLOCK", Boolean.TRUE);
        functionOptions.put("BOTTOMUP", Boolean.TRUE);
        functionOptions.put("CELL", Boolean.TRUE);
        functionOptions.put("COPYFORWARD", Boolean.TRUE);
        functionOptions.put("DD-MM-YYYY", Boolean.TRUE);
        functionOptions.put("MM-DD-YYYY", Boolean.TRUE);
        functionOptions.put("DEFAULT", Boolean.TRUE);
        functionOptions.put("DES", Boolean.TRUE);
        functionOptions.put("DETAIL", Boolean.TRUE);
        functionOptions.put("DIVIDE", Boolean.TRUE);
        functionOptions.put("DYNAMIC", Boolean.TRUE);
        functionOptions.put("EMPTY", Boolean.TRUE);
        functionOptions.put("ERROR", Boolean.TRUE);
        functionOptions.put("ERRORSTOHIGH", Boolean.TRUE);
        functionOptions.put("ERRORSTOLOW", Boolean.TRUE);
        functionOptions.put("ERRORSTOMBR", Boolean.TRUE);
        functionOptions.put("EXPENSE", Boolean.TRUE);
        functionOptions.put("FIRST", Boolean.TRUE);
        functionOptions.put("GEN", Boolean.TRUE);
        functionOptions.put("HIGH", Boolean.TRUE);
        functionOptions.put("IN", Boolean.TRUE);
        functionOptions.put("INFO", Boolean.TRUE);
        functionOptions.put("LAST", Boolean.TRUE);
        functionOptions.put("LEV", Boolean.TRUE);
        functionOptions.put("LOW", Boolean.TRUE);
        functionOptions.put("LR", Boolean.TRUE);
        functionOptions.put("MM-DD-YYYY", Boolean.TRUE);
        functionOptions.put("MULTIPLY", Boolean.TRUE);
        functionOptions.put("NONE", Boolean.TRUE);
        functionOptions.put("NONINPUT", Boolean.TRUE);
        functionOptions.put("NOROUND", Boolean.TRUE);
        functionOptions.put("OFF", Boolean.TRUE);
        functionOptions.put("ON", Boolean.TRUE);
        functionOptions.put("ONLY", Boolean.TRUE);
        functionOptions.put("PERCENT", Boolean.TRUE);
        functionOptions.put("ROUNDAMT", Boolean.TRUE);
        functionOptions.put("SES", Boolean.TRUE);
        functionOptions.put("SHARE", Boolean.TRUE);
        functionOptions.put("SKIPBOTH", Boolean.TRUE);
        functionOptions.put("SKIPMISSING", Boolean.TRUE);
        functionOptions.put("SKIPNONE", Boolean.TRUE);
        functionOptions.put("SKIPZERO", Boolean.TRUE);
        functionOptions.put("SPREAD", Boolean.TRUE);
        functionOptions.put("SUBTRACT", Boolean.TRUE);
        functionOptions.put("SUMMARY", Boolean.TRUE);
        functionOptions.put("TES", Boolean.TRUE);
        functionOptions.put("TOPDOWN", Boolean.TRUE);
        functionOptions.put("TRAILMISSING", Boolean.TRUE);
        functionOptions.put("TRAILSUM", Boolean.TRUE);
        functionOptions.put("TWOPASS", Boolean.TRUE);
        functionOptions.put("UPPER", Boolean.TRUE);
        functionOptions.put("WARNS", Boolean.TRUE);
        functionOptions.put("XEXP", Boolean.TRUE);
        functionOptions.put("XLOG", Boolean.TRUE);
        functionOptions.put("XPOW", Boolean.TRUE);
        functionOptions.put("YEXP", Boolean.TRUE);
        functionOptions.put("YLOG", Boolean.TRUE);
        functionOptions.put("YPO", Boolean.TRUE);

        for (int i = 0; i < CLBParseConstants.tokenImage.length; i++)
            if (CLBParseConstants.tokenImage[i].equals("\",\"")) {
                CLBPARSE_TOKEN_COMMA = i;
                break;
            }
    }

    private static String fixA0(String text) {
        return (text.replace((char) 0xA0, ' '));
    }

    @Override
    public void parse(String text) throws ParsingException {
        if (ConfigurationManager.getProperty(ConfigurationManager.LOG_CALCPARSED, false)) {
            if (!Util.isEmpty(ConfigurationManager.getProperty(ConfigurationManager.DEBUG_DIR_KEY))) {
                DebugFileUtils.writeFile("calc", text, FileExtension.CALC);
            }
        }
        parse(new StringReader(fixA0(text)), false);
    }

    public void parse(String text, boolean fullValidate) throws ParsingException {
        if (ConfigurationManager.getProperty(ConfigurationManager.LOG_CALCPARSED, false)) {
            if (!Util.isEmpty(ConfigurationManager.getProperty(ConfigurationManager.DEBUG_DIR_KEY))) {
                DebugFileUtils.writeFile("calc", text, FileExtension.CALC);
            }
        }
        parse(new StringReader(fixA0(text)), fullValidate);
    }

    public void parse(Reader reader, boolean fullValidate) throws ParsingException {
        CLBParse parser = null;
        Map<String, Boolean> vars = CHashMapCache.getMap();
        Map<String, String> parms = CHashMapCache.getMap();

        try {
            parser = new CLBParse(reader);
            parser.setValidateAll(fullValidate);
            this.rootNode = parser.Input();

            Map<String, Boolean> jVars = null;
            if (parser.token_source.planningExpr.size() > 0) {
                JavaParser jp = new JavaParser(System.in);
                for (int i = 0; i < parser.token_source.planningExpr.size(); i++) {
                    jp.ReInit(new StringReader(parser.token_source.planningExpr.get(i)));
                    try {
                        jp.planningexpression();
                    }
                    catch (com.hyperion.calcmgr.plugin.essbase.java.ParseException e) {
                    }
                }
                jVars = jp.token_source.variables;
                parser.token_source.variables.putAll(jVars);
            }

            Set<String> variables = parser.token_source.variables.keySet();
            Iterator<String> iter = variables.iterator();

            while (iter.hasNext()) {
                String var = iter.next();
                if (var.startsWith(PARM_PREFIX)) {
                    parms.put(var, var);
                }
                else {
                    vars.put(var, Boolean.TRUE);
                }
            }
            // Merge the variables from Planning Expressions and the Calc script
//            if (jVars != null) {
//                vars.putAll(jVars);
//            }

//            if ((this.hasGroovy = parser.token_source.hasGroovyExpr) == true) {
//                scanVariablesInGroovy(this.rootNode, vars);
//            }
            
            RunJavaHelper.scanVariablesInRunJava(this.rootNode, vars);
            
            this.variablesUsed = vars.keySet().toArray(Util.EMPTY_STRINGS);
            this.parmsUsed = parms.keySet().toArray(Util.EMPTY_STRINGS);
            this.macrosUsed = parser.token_source.macros.keySet().toArray(Util.EMPTY_STRINGS);
        }
        catch (ParseException e) {
            // e.printStackTrace();
            ThrowParsingException(e);
        }
        catch (TokenMgrError e) {
            ThrowParsingException(parser, e);
        }
        catch (Throwable t) {
            throw new ParsingException(t.getMessage());
        }
        finally {
            CHashMapCache.releaseMap(vars);
            CHashMapCache.releaseMap(parms);
        }
    }

    public void parse(InputStream fileIn, boolean fullValidate) throws ParsingException {
        parse(new InputStreamReader(fileIn), fullValidate);
    }

    private void ThrowParsingException(ParseException e) throws ParsingException {
        throw new ParsingException(e);
    }

    private void ThrowParsingException(CLBParse parser, TokenMgrError e) {
        int offset = 0;
        if (parser != null) {
            offset = parser.token_source.getimagelength();
        }
        String msg = e.getMessage();
        String linNum = msg.substring(22, msg.indexOf(StringConstants.COMMA));
        int line = Integer.parseInt(linNum);
        String colNum = msg.substring(msg.indexOf("column") + 7, msg.indexOf(StringConstants.DOT));
        int col = Integer.parseInt(colNum);

        if (offset != 0) {
            throw new ParsingException(e.getMessage(), offset);
        }

        throw new ParsingException(e.getMessage(), line, col, line, col);
    }

    public String CS2MDXRange(String text) throws ParseException {
        return (CS2MDXRange(text, false, null));
    }

    *//**
     * The converter for changing a member range into mdx
     * 
     * @param text
     *            a dimension's memberselection
     * @param fixer
     *            the find member in dimension finder
     * @return returns a mdx string for the given member selection
     * @throws ParseException
     *//*

    public String CS2MDXRange(String text, boolean useDistinct, List<String> mbrs) throws ParseException {
        CLBParse parser;
        if (Util.isEmpty(text)) {
            return Util.EMPTY_STRING;
        }

        parser = new CLBParse(new StringReader(text));
        SimpleNode root = parser.fixlist();
        // lets convert the escape chars
        convertStrings(root);
        // start set
        StringBuilder retval = StringBufferCache.getBuffer();

        try {
            if (useDistinct) {
                retval.append("distinct(");
            }

            if (mbrs != null) {
                findMbrs(root, mbrs, null);
            }

            retval.append(StringConstants.LEFT_CURLY);

            for (int j = 0; j < root.jjtGetNumChildren(); j++) {
                if (j != 0) {
                    retval.append(StringConstants.COMMA);
                }
                SimpleNode fixexp = (SimpleNode) root.jjtGetChild(j);
                retval.append(getfixExpMDX(fixexp));
            }

            retval.append(StringConstants.RIGHT_CURLY);
            if (useDistinct) {
                retval.append(StringConstants.RIGHT_PARAN);
            }
            return retval.toString();
        }
        finally {
            StringBufferCache.releaseBuffer(retval);
        }
    }

    public String[] CS2MDXRanges(String text, IFixResolver fixer) throws ParseException {
        return (CS2MDXRanges(text, fixer, false, null));
    }

    *//**
     * The converter for changing a member range into mdx
     * 
     * @param text
     *            a dimension's memberselection
     * @param fixer
     *            the find member in dimension finder
     * @return returns a mdx string for the given member selection
     * @throws ParseException
     *//*

    public String[] CS2MDXRanges(String text, IFixResolver fixer, boolean useDistinct, List<String> mbrs) throws ParseException {
        CLBParse parser;
        if (Util.isEmpty(text)) {
            return Util.EMPTY_STRINGS;
        }

        parser = new CLBParse(new StringReader(text));
        SimpleNode root = parser.fixlist();
        // lets convert the escape chars
        convertStrings(root);
        // start set
        StringBuilder retval = StringBufferCache.getBuffer();
        List<String> list = ArrayListCache.getArrayList();

        try {
            if (mbrs != null) {
                findMbrs(root, mbrs, null);
            }

            for (int j = 0; j < root.jjtGetNumChildren(); j++) {
                retval.setLength(0);
                if (useDistinct) {
                    retval.append("distinct(");
                }
                retval.append(StringConstants.LEFT_CURLY);

                SimpleNode fixexp = (SimpleNode) root.jjtGetChild(j);
                retval.append(getfixExpMDX(fixexp));
                retval.append(StringConstants.RIGHT_CURLY);
                if (useDistinct) {
                    retval.append(StringConstants.RIGHT_PARAN);
                }
                list.add(retval.toString());
            }
            return list.toArray(new String[list.size()]);
        }
        finally {
            StringBufferCache.releaseBuffer(retval);
            ArrayListCache.releaseList(list);
        }
    }

    public String getfixExpMDX(SimpleNode fixExp) throws ParseException {
        SimpleNode fixdisjunction = (SimpleNode) fixExp.jjtGetChild(0);
        if (fixdisjunction.jjtGetNumChildren() == 1) {
            return getfixExpfixconjunction((SimpleNode) fixdisjunction.jjtGetChild(0));
        }

        StringBuilder retval = StringBufferCache.getBuffer();

        try {
            retval.append(VariableBean.VAR_START);
            for (int j = 0; j < fixdisjunction.jjtGetNumChildren(); j++) {
                if (j != 0) {
                    retval.append(StringConstants.COMMA);
                }
                SimpleNode fixconjunction = (SimpleNode) fixdisjunction.jjtGetChild(j);
                retval.append(getfixExpfixconjunction(fixconjunction));
            }
            retval.append(VariableBean.VAR_END);

            return retval.toString();
        }
        finally {
            StringBufferCache.releaseBuffer(retval);
        }
    }

    public String getfixitem(SimpleNode fixItem) throws ParseException {
        // fixitem() (<AND> fixitem())*
        fixItem = (SimpleNode) fixItem.jjtGetChild(0);
        StringBuilder retval = StringBufferCache.getBuffer();
        try {
            switch (fixItem.getId()) {
                case CLBParseTreeConstants.JJTRANGELIST: // new 3 child node
                    retval.append("MemberRange(");
                    retval.append(fixItem.jjtGetChild(0));
                    retval.append(StringConstants.COMMA);
                    if (fixItem.jjtGetNumChildren()>2) {
                        retval.append(fixItem.jjtGetChild(2));
                    }
                    else {
                        retval.append(fixItem.jjtGetChild(1));
                    }

                    retval.append(StringConstants.RIGHT_PARAN);
                    break;
                case CLBParseTreeConstants.JJTFUNCTION:
                    return MDXFunction(fixItem);
                case CLBParseTreeConstants.JJTMBRONLY:
                    return fixItem.toString();
                case CLBParseTreeConstants.JJTFIXEXP:
                    return getfixExpMDX(fixItem);
                default:
            }

            return retval.toString();
        }
        finally {
            StringBufferCache.releaseBuffer(retval);
        }
    }

    public boolean updateFixParallelThread(SimpleNode currNode, int numThread) {
        if (currNode == null) {
            currNode = this.rootNode;
        }
        
        if (currNode.getId() == CLBParseTreeConstants.JJTFIXPARATHREADS) {
            try {
                int n = Integer.parseInt(currNode.first_token.image);
                if ((n > 0) && (n < numThread)) {
                    numThread = n;
                }
            }
            catch (Throwable t) {
            }
            
            ReplaceNodeToken(String.valueOf(numThread), currNode);
            return true;
        }

        boolean ret = false;
        for (int i = 0; i < currNode.jjtGetNumChildren(); i++) {
            if (updateFixParallelThread((SimpleNode)currNode.jjtGetChild(i), numThread)) {
                ret = true;
            }
        }
        
        return (ret);
    }
    
    private void convertFuncParms(SimpleNode parm) {
        if (parm.getId() == CLBParseTreeConstants.JJTFUNCTION) {
            String value = MDXFunction(parm);
            ReplaceNodeToken(value, parm);
        }
        else {
            for (int i = 0; i < parm.jjtGetNumChildren(); i++) {
                convertFuncParms((SimpleNode) parm.jjtGetChild(i));
            }
        }
    }

    private void ReplaceNodeToken(String value, SimpleNode parm) {
        parm.first_token.image = value;
        parm.first_token.next = parm.last_token.next;

        for (SimpleNode curr = (SimpleNode) parm.jjtGetParent(); curr != null; curr = (SimpleNode) curr.jjtGetParent()) {
            if (curr.last_token == parm.last_token)
                curr.last_token = parm.first_token;
        }

        parm.last_token = parm.first_token;
        parm.zeroChildren();
    }

    public static MDXFunctionConverter getMDXFuncConverter(String name) {
        return (MDXFunctionConverter.definedFuncs.get(name.toLowerCase().trim()));
    }

    private String MDXFunction(SimpleNode functionNode) {
        String function = functionNode.jjtGetChild(0).toString();

        String key = function.toLowerCase().trim();
        MDXFunctionConverter converter = MDXFunctionConverter.definedFuncs.get(key);
        SimpleNode parms = (SimpleNode) functionNode.jjtGetChild(1);
        for (int i = 0; i < parms.jjtGetNumChildren(); i++) {
            convertFuncParms((SimpleNode) parms.jjtGetChild(i));
        }

        if (converter == null) {
            String parm1 = parms.jjtGetChild(0).toString();

            StringBuilder retval = StringBufferCache.getBuffer();
            try {
                retval.append(parm1);
                retval.append(StringConstants.DOT);
                retval.append(function.substring(1));
                retval.append(StringConstants.LEFT_PARAN);
                for (int i = 1; i < parms.jjtGetNumChildren(); i++) {
                    if (i != 1)
                        retval.append(StringConstants.COMMA);
                    retval.append(parms.jjtGetChild(i).toString());
                }
                retval.append(StringConstants.RIGHT_PARAN);
                return retval.toString();
            }
            finally {
                StringBufferCache.releaseBuffer(retval);
            }
        }
        return converter.ConvertFunction(parms);
    }

    public String getfixExpfixconjunction(SimpleNode fixconjunction) throws ParseException {
        // fixitem() (<AND> fixitem())*
        StringBuilder retval = StringBufferCache.getBuffer();

        try {
            if (fixconjunction.jjtGetNumChildren() == 1)
                return getfixitem((SimpleNode) fixconjunction.jjtGetChild(0));
            if (fixconjunction.jjtGetNumChildren() > 1) {
                int i = 0;

                // 0 and 1 and 2 and 3
                // i(0,i(1, i(2, 3))); interce
                int parensNeeded = 0;
                while (i < fixconjunction.jjtGetNumChildren()) {
                    if (i + 2 < fixconjunction.jjtGetNumChildren()) {
                        retval.append("intersect(");
                        SimpleNode fixItem1 = (SimpleNode) fixconjunction.jjtGetChild(i);
                        retval.append(getfixitem(fixItem1));
                        parensNeeded++;
                        i++;
                    }
                    else {
                        retval.append("intersect(");
                        SimpleNode fixItem1 = (SimpleNode) fixconjunction.jjtGetChild(i);
                        SimpleNode fixItem2 = (SimpleNode) fixconjunction.jjtGetChild(i + 1);
                        retval.append(getfixitem(fixItem1));
                        retval.append(StringConstants.COMMA);
                        retval.append(getfixitem(fixItem2));
                        retval.append(StringConstants.RIGHT_PARAN);
                        i += 2;
                    }
                }
                for (i = 0; i < parensNeeded; i++)
                    retval.append(StringConstants.RIGHT_PARAN);
            }
            return retval.toString();
        }
        finally {
            StringBufferCache.releaseBuffer(retval);
        }
    }

    public String CS2MDXFormula(String text, IFixResolver fixer) throws ParseException {
        CLBParse parser;
        if (text == null || text.trim().length() == 0)
            return Util.EMPTY_STRING;

        parser = new CLBParse(new StringReader(text));
        SimpleNode root = parser.assign(); // variable() <ASSIGN> expression()
        SimpleNode lhs = (SimpleNode) root.jjtGetChild(0); // variable()
        SimpleNode rhs = (SimpleNode) root.jjtGetChild(1); // variable()
        // System.out.println("lhs:");
        // lhs.dump("");
        // System.out.println("rhs:");
        // rhs.dump("");

        // lets convert the escape chars
        convertStrings(root);

        lhs = (SimpleNode) lhs.jjtGetChild(0); // xmb
        String[] dimsA = null;
        Map<String, Integer> dims = null;

        try {
            if (fixer != null) { // to normalize tuple
                dimsA = fixer.getDimensions();
                dims = CHashMapCache.getMap();
                for (int i = 0; i < dimsA.length; i++) {
                    dims.put(dimsA[i], new Integer(i));
                }
            }
            String LHS = ConvertCrossDim(lhs, fixer, dims, dimsA);
            // We need to walk the RHS coverting crossDims;
            ConvertExpr(rhs, fixer, dims, dimsA);
            return LHS + " = " + rhs.toString() + StringConstants.SEMICOLON;
        }
        finally {
            CHashMapCache.releaseMap(dims);
        }
    }

    public String CS2MDXCustomCalc(String text, IFixResolver fixer, List<String> lhsMbrs, List<String> rhsMbrs) {
        return CS2MDXCustomCalc(text, fixer, lhsMbrs, rhsMbrs, null, null);
    }

    public String CS2MDXCustomCalc(String text, IFixResolver fixer, List<String> lhsMbrs, List<String> rhsMbrs, List<String> lhsXmbrs, List<String> rhsXmbrs) {
        if (text == null || text.trim().length() == 0)
            return Util.EMPTY_STRING;
        parse(text, false);
        SimpleNode root = getRootNode();
        return CS2MDXCustomCalc(root, fixer, lhsMbrs, rhsMbrs, lhsXmbrs, rhsXmbrs);
    }

    public String CS2MDXCustomCalc(SimpleNode root, IFixResolver fixer, List<String> lhsMbrs, List<String> rhsMbrs, List<String> lhsXmbrs, List<String> rhsXmbrs) {
        String[] dimsA = null;
        Map<String, Integer> dims = null;

        try {
            if (fixer != null) { // to normalize tuple
                dimsA = fixer.getDimensions();
                dims = CHashMapCache.getMap();
                for (int i = 0; i < dimsA.length; i++) {
                    dims.put(dimsA[i], new Integer(i));
                }
            }
            ConvertAssignents(root, fixer, dims, dimsA, lhsMbrs, rhsMbrs, lhsXmbrs, rhsXmbrs);

            return root.toString();
        }
        finally {
            CHashMapCache.releaseMap(dims);
        }
    }

    private void ConvertAssignents(SimpleNode root, IFixResolver fixer, Map<String, Integer> dims, String[] dimsA, List<String> lhsMbrs, List<String> rhsMbrs, List<String> lhsXmbrs, List<String> rhsXmbrs) {
        if (root.getId() == CLBParseTreeConstants.JJTASSIGN) {
            SimpleNode lhs = (SimpleNode) root.jjtGetChild(0); // variable()
            SimpleNode rhs = (SimpleNode) root.jjtGetChild(1); // expression()

            // need to change the assignment operator
            for (Token curr = root.first_token; curr != root.last_token; curr = curr.next) {
                if (curr.kind == CLBParseConstants.ASSIGN)
                    curr.image = ":=";
            }
            if (lhsMbrs != null || lhsXmbrs != null)
                findMbrs(lhs, lhsMbrs, lhsXmbrs);
            if (rhsMbrs != null || rhsXmbrs != null)
                findMbrs(rhs, rhsMbrs, rhsXmbrs);
            lhs = (SimpleNode) lhs.jjtGetChild(0); // xmb
            // lets convert the escape chars
            convertStrings(root);
            String LHS = ConvertCrossDim(lhs, fixer, dims, dimsA);
            ReplaceNodeToken(LHS, lhs);
            ConvertExpr(rhs, fixer, dims, dimsA);

            // return LHS + " = " + rhs.toString() + StringConstants.SEMICOLON;
        }
        else {
            for (int i = 0; i < root.jjtGetNumChildren(); i++)
                ConvertAssignents((SimpleNode) root.jjtGetChild(i), fixer, dims, dimsA, lhsMbrs, rhsMbrs, lhsXmbrs, rhsXmbrs);

        }
    }

    private void ConvertExpr(SimpleNode expr, IFixResolver fixer, Map<String, Integer> dims, String[] dimsA) {
        for (int i = 0; i < expr.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) expr.jjtGetChild(i);
            if (child.getId() == CLBParseTreeConstants.JJTXMBR) {
                String xmbr = ConvertCrossDim(child, fixer, dims, dimsA);
                ReplaceNodeToken(xmbr, child);
            }
            if (child.jjtGetNumChildren() > 0)
                ConvertExpr(child, fixer, dims, dimsA);
        }
    }

    private String Convert2NoramlizedCrossDim(SimpleNode lhs, IFixResolver fixer, Map<String, Integer> dims, String[] dimsA) {
        String[] mbrs = new String[dimsA.length];
        // each child is either a function or mbrName
        for (int i = 0; i < lhs.jjtGetNumChildren(); i++) { // parensMbrorfunc()
                                                            // (<CROSSDIM>
                                                            // parensMbrorfunc())*
            SimpleNode mbrOrFunc = (SimpleNode) lhs.jjtGetChild(i);
            SimpleNode mbr = drillToMbrOrFunc(mbrOrFunc);
            if (mbr == null)
                continue;
            String dim;
            if (mbr.getId() == CLBParseTreeConstants.JJTFUNCTION)
                continue; // todo ignoring functions for now;
            if (mbr.jjtGetNumChildren() > 0) {// must be a reference
                dim = fixer.getDimensionForVariable(mbr.toString());
            }
            else
                dim = fixer.getDimensionForMember(mbr.toString());
            if (dim == null)
                dim = Util.EMPTY_STRING;
            Integer pos = dims.get(dim);
            if (pos != null) {
                mbrs[pos.intValue()] = mbr.toString();
            }
        }
        StringBuilder buff = StringBufferCache.getBuffer();

        try {
            buff.append(StringConstants.LEFT_PARAN);
            for (int i = 0; i < dimsA.length; i++) {
                if (i != 0)
                    buff.append(StringConstants.COMMA);
                if (mbrs[i] == null)
                    buff.append(dimsA[i]);
                else
                    buff.append(mbrs[i]);
            }
            buff.append(StringConstants.RIGHT_PARAN);

            return buff.toString();
        }
        finally {
            StringBufferCache.releaseBuffer(buff);
        }
    }

    private String ConvertCrossDim(SimpleNode lhs, IFixResolver fixer, Map<String, Integer> dims, String[] dimsA) {
        if (fixer != null) {
            return Convert2NoramlizedCrossDim(lhs, fixer, dims, dimsA);
        }

        return ConvertCrossDim(lhs, false);
    }

    private String ConvertCrossDim(SimpleNode xmbr, boolean inner) {
        // each child is a xmbrTerm
        
         * void xmbr():{} { xmbrTerm() ( <CROSSDIM> xmbrTerm() )* }
         
        StringBuilder buff = StringBufferCache.getBuffer();

        try {
            for (int i = 0; i < xmbr.jjtGetNumChildren(); i++) {
                SimpleNode xmbrTerm = (SimpleNode) xmbr.jjtGetChild(i);
                SimpleNode child = (SimpleNode) xmbrTerm.jjtGetChild(0);
                switch (child.getId()) {
                    case CLBParseTreeConstants.JJTXMBR:
                        if (buff.length() != 0) {
                            buff.append(StringConstants.COMMA);
                        }
                        buff.append(ConvertCrossDim(child, true));
                        break;
                    case CLBParseTreeConstants.JJTMBRORFUNC:
                    default:
                        SimpleNode mbr = drillToMbrOrFunc(child);
                        if (mbr == null)
                            continue;
                        if (mbr.getId() == CLBParseTreeConstants.JJTFUNCTION)
                            continue; // todo ignoring functions for now;
                        if (buff.length() != 0) {
                            buff.append(StringConstants.COMMA);
                        }
                        buff.append(mbr.toString());

                }
            }

            if (buff.length() == 0) {
                return (Util.EMPTY_STRING);
            }

            if (inner) {
                return buff.toString().trim();
            }

            return (StringConstants.LEFT_PARAN + buff.toString().trim() + StringConstants.RIGHT_PARAN);
        }
        finally {
            StringBufferCache.releaseBuffer(buff);
        }
    }

    private SimpleNode drillToMbrOrFunc(SimpleNode mbrOrFunc) {
        if (mbrOrFunc.getId() == CLBParseTreeConstants.JJTFUNCTION || mbrOrFunc.getId() == CLBParseTreeConstants.JJTMBRNAME) {
            return mbrOrFunc;
        }

        for (int i = 0; i < mbrOrFunc.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) mbrOrFunc.jjtGetChild(i);
            SimpleNode inner = drillToMbrOrFunc(child);
            if (inner != null)
                return inner;
        }

        return null;
    }

    private void convertStrings(SimpleNode root) {
        Token curr = root.first_token;
        while (curr != null) {
            if ((curr.kind == CLBParseConstants.STRING) || (curr.kind == CLBParseConstants.ID)) {
                if (curr.image.startsWith("\"")) {
                    curr.image = MDXHelper.convertMemeberToMDX(curr.image.substring(1, curr.image.length() - 1));
                }
                else {
                    curr.image = MDXHelper.convertMemeberToMDX(curr.image);
                }
            }
            else if (curr.kind == CLBParseConstants.SUBST_VARIABLE) {
                curr.image = MDXHelper.convertMemeberToMDX(curr.image);
            }
            if (curr == root.last_token)
                break;
            curr = curr.next;
        }
    }
    static public ArrayList<SimpleNode> findAllNodeType(int nodeType, SimpleNode root,
                                                        ArrayList<SimpleNode> list){
        return findAllNodeType( nodeType,  root, list, true);                                          
    }
    static public ArrayList<SimpleNode> findAllNodeType(int nodeType, SimpleNode root,
                                                  ArrayList<SimpleNode> list, boolean recurse) {
         if (root == null)
             return list;
         if (root.getId() == nodeType) {
             if (list == null)
                 list = new ArrayList<SimpleNode>();
             list.add(root);
             if (!recurse) {

             
                 return list;
             }
         }

         ArrayList<SimpleNode> stack = new ArrayList<SimpleNode>();
         stack.add(root);

         while (stack.size() > 0) {
             SimpleNode curr = stack.remove(0);
             for (int i = 0; i < curr.jjtGetNumChildren(); i++) {
                 SimpleNode child = (SimpleNode)curr.jjtGetChild(i);
                 if (child.getId() == nodeType) {
                     if (list == null)
                         list = new ArrayList<SimpleNode>();
                     list.add(child);
                     if (recurse && child.jjtGetNumChildren()>0 ) {
                         for (int childI = 0;childI < child.jjtGetNumChildren();childI++ )
                             stack.add((SimpleNode)child.jjtGetChild(childI));
                     }
                 } else
                     stack.add(child);
             }
         }
         return list;
     }
    public MultiMbrSelection multiMbrParse(String text) throws ParseException {
        CLBParse parser;
        ArrayList<String> selectedMbrs = new ArrayList<String>();
        ArrayList<String> excludedMbrs = new ArrayList<String>();
        if (text == null || text.trim().length() == 0)
            return new MultiMbrSelection(selectedMbrs, excludedMbrs);
        parser = new CLBParse(new StringReader(text));
        SimpleNode root = parser.mdparse(); // mdParse
        root = (SimpleNode) root.jjtGetChild(0); // MEMBERSELECTION
        // let's get selected
        SimpleNode selected = (SimpleNode) root.jjtGetChild(0); // MBRORFUNCLIST
        parseMbrOrFuncList(selected, selectedMbrs);
        if (root.jjtGetNumChildren() > 1) {
            // let's get excluded
            SimpleNode excluded = (SimpleNode) root.jjtGetChild(1); // MBRORFUNCLIST
            parseMbrOrFuncList(excluded, excludedMbrs);
        }
        return new MultiMbrSelection(selectedMbrs, excludedMbrs);
    }

    private void parseMbrOrFuncList(SimpleNode mbrorfunclist, ArrayList<String> list) {
        for (int i = 0; i < mbrorfunclist.jjtGetNumChildren(); i++) {
            SimpleNode mbrorfunc = (SimpleNode) mbrorfunclist.jjtGetChild(i);
            list.add(mbrorfunc.toString().trim());
        }
    }

    @Override
    public Object getRoot() {
        return (getRootNode());
    }

    public SimpleNode getRootNode() {
        return this.rootNode;
    }

    @Override
    public String[] getVariablesUsed() {
        return (getVariablesUsed(VariableTypes.SUBSTITUTION_VARS));
    }

    @Override
    public String[] getDTPUsed() {
        return (getVariablesUsed(VariableTypes.DTP_VARS));
    }

    @Override
    public String[] getVariablesUsed(VariableTypes varType) {
        if (this.variablesUsed == null) {
            return (Util.EMPTY_STRINGS);
        }

        if (this.variablesUsed.length == 0) {
            return (this.variablesUsed);
        }

        int type = varType.getVariableType();
        CHashList<String, String> list = new CHashList<String, String>();

        try {
            for (String variable : this.variablesUsed) {
                char ch = variable.charAt(0);
                String varName = null;
                if (((type & SUBSTITUTION_VARS.getVariableType()) == SUBSTITUTION_VARS.getVariableType()) && (ch == VAR_START)) {
                    varName = variable.substring(1);
                }
                else if (((type & DTP_VARS.getVariableType()) == DTP_VARS.getVariableType()) && (ch == DTP_START)) {
                    varName = variable.substring(1);
                }
                else if (((type & ESSBASE_VARS.getVariableType()) == ESSBASE_VARS.getVariableType()) && (ch == ESSBASE_START)) {
                    varName = variable.substring(1);
                }
                else {
                    if (ch != '$') {
                        // Assume substitution vars if there is no prefix. These
                        // came from the expressions.
                        varName = variable;
                    }
                }
                
                if (varName != null) {
                    list.add(varName);
                }
            }
            return (list.toArray(new String[list.size()]));
        }
        finally {
            list.clear();
        }
    }

    @Override
    public String[] getParmsUsed() {
        return this.parmsUsed;
    }

    public String[] getMacrosUsed() {
        return this.macrosUsed;
    }

    *//***** Members function *******//*
    @Override
    public String[] getMembers(String text) throws ParsingException {
        return getMembers(text, false);
    }

    public String[] getMembers(String text, boolean ignoreFuncs) throws ParsingException {
        ParserToken[] tokens = getMemberTokens(text, ignoreFuncs);
        return (ParserToken.toStringArray(tokens));
    }

    public ParserToken[] getMemberTokens(String text, boolean ignoreFuncs) throws ParsingException {
        parse(text, false);
        return (getMemberTokens(ignoreFuncs));
    }

    public ParserToken[] getMemberTokens() throws ParsingException {
        return getMemberTokens(false);
    }

    public ParserToken[] getMemberTokens(boolean ingnoreFuncs) throws ParsingException {
        Map<String, String> scriptVars = CHashMapCache.getMap();

        try {
            findScriptVariables(scriptVars);
            return (getMemberTokens(scriptVars, ingnoreFuncs));
        }
        finally {
            CHashMapCache.releaseMap(scriptVars);
        }
    }

    public ParserToken[] getAllMemberTokens(Map<String, String> scriptVars, boolean ingnoreFuncs) throws ParsingException {
        List<ParserToken> tokens = ArrayListCache.getArrayList();

        try {
            findScriptVariables(scriptVars);
            findMbrs(getRootNode(), null, tokens, scriptVars, null, ingnoreFuncs);
            return (tokens.toArray(new ParserToken[tokens.size()]));
        }
        finally {
            ArrayListCache.releaseList(tokens);
        }
    }

    public ParserToken[] getMemberTokens(Map<String, String> scriptVars, boolean ingnoreFuncs) throws ParsingException {
        Map<String, ParserToken> mbrNames = CHashMapCache.getMap();

        try {
            findMbrs(getRootNode(), mbrNames, null, scriptVars, null, ingnoreFuncs);
            return ((ParserToken[]) Util.mapToArray(mbrNames, new ParserToken[mbrNames.keySet().size()]));
        }
        finally {
            CHashMapCache.releaseMap(mbrNames);
        }
    }

    
     * public void findMembers(Object currNode, Hashtable mbrNames, Hashtable
     * scriptVars) { if (currNode instanceof SimpleNode) { findMbrs((SimpleNode)
     * currNode, mbrNames, scriptVars, null); } }
     
    private boolean isMember(Map<String, ParserToken> mbrNames, Map<String, String> scriptVars, String mbrName) {
        mbrName = Util.removeDQuoteString(mbrName);

        if (mbrName.equalsIgnoreCase("#Missing") || (mbrName.indexOf('?') >= 0) || (mbrName.indexOf('*') >= 0)) {
            return (false);
        }

        if (mbrName.equals(">") || mbrName.equals(">=") || mbrName.equals("<") || mbrName.equals("<=") || mbrName.equals("==") || mbrName.equals("<>") || mbrName.equals("!=") || mbrName.equalsIgnoreCase("IN")) {
            return (false);
        }

        if (scriptVars == null) {
            return (true);
        }

        mbrName = Util.removeDQuoteString(mbrName);
        return (!scriptVars.containsKey(mbrName));
    }

    public void findMbrs(SimpleNode currNode, Map<String, ParserToken> mbrNames, List<ParserToken> memberList, Map<String, String> scriptVars, String functionName, boolean ignoreFuncs) {
        SimpleNode child;

        if (currNode.getId() == CLBParseTreeConstants.JJTRUNTIMEPARAMETERS) {
            return;
        }
        
        int count = currNode.jjtGetNumChildren();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                child = (SimpleNode) currNode.jjtGetChild(i);
                if ((child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTMBRNAME) || (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTDIMNAME)) {
                    if (child.jjtGetNumChildren() == 0) { // <ID> | <STRING> |
                        String mbrName = child.toString().trim();
                        if (!Util.isEmpty(mbrName)) {
                            if (functionName == null) {
                                if (isMember(mbrNames, scriptVars, mbrName)) {
                                    if ((mbrNames != null) && !mbrNames.containsKey(mbrName)) {
                                        mbrNames.put(mbrName, getToken(child, true));
                                    }
                                    if (memberList != null) {
                                        memberList.add(getToken(child, true));
                                    }
                                }
                            }
                            else {
                                // if (functionName.equalsIgnoreCase("@XREF") ||
                                // functionName.equalsIgnoreCase("@CONCATENATE")
                                // ||
                                // functionName.equalsIgnoreCase("@SUBSTRING"))
                                // {
                                // // For @XREF, first argument is a location
                                // alias and the other arguments are the members
                                // // in the database pointed by the location
                                // alias. So we can't validate that
                                // return;
                                // }

                                if (functionOptions.get(mbrName.toUpperCase()) == null) {
                                    if (isMember(mbrNames, scriptVars, mbrName)) {
                                        if ((mbrNames != null) && !mbrNames.containsKey(mbrName)) {
                                            mbrNames.put(mbrName, getToken(child, true));
                                        }
                                        if (memberList != null) {
                                            memberList.add(getToken(child, true));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else { // qualifiedMbrName() | reference()
                        SimpleNode childChild = (SimpleNode) child.jjtGetChild(0);
                        if (childChild.getId() == CLBParseTreeConstants.JJTQUALIFIEDMBRNAME) {
                            String mbrName = child.toString().trim();
                            if ((mbrNames != null) && !mbrNames.containsKey(mbrName)) {
                                mbrNames.put(mbrName, getToken(child, true));
                            }
                            if (memberList != null) {
                                memberList.add(getToken(child, true));
                            }
                        }
                        
                         * the following code will split the path into members
                         * for (Token curr = childChild.first_token; curr!=
                         * null; curr=curr.next) { if (curr.kind ==
                         * CLBParseConstants.MDXMbr) { String mbrName =
                         * curr.image.substring(1, curr.image.length() - 1);
                         * ParserToken toke = new ParserToken(mbrName,
                         * curr.beginLine, curr.beginColumn, curr.endLine,
                         * curr.endColumn); mbrNames.put(mbrName, toke); } }
                         
                    }
                }
                else if (child.jjtGetNumChildren() > 0) {
                    if (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTFUNCTION) {
                        if (!ignoreFuncs) {
                            String funcName = child.first_token.toString().trim();
                            findMbrs(child, mbrNames, memberList, scriptVars, funcName, ignoreFuncs);
                        }
                    }
                    else {
                        findMbrs(child, mbrNames, memberList, scriptVars, functionName, ignoreFuncs);
                    }
                }
            }
        }
    }

    public static ParserToken getToken(SimpleNode node, boolean trim) {
        String name = (trim) ? node.toString().trim() : node.toString();
        return (new ParserToken(name, node.first_token.beginLine, node.first_token.beginColumn, node.last_token.endLine, node.last_token.endColumn));
    }

    public static ParserToken getToken(SimpleNode node, Object object) {
        return (new ParserToken(object, node.first_token.beginLine, node.first_token.beginColumn, node.last_token.endLine, node.last_token.endColumn));
    }

    @Override
    public String[] getAllMembers(String text) throws ParsingException {
        text = text.trim();

        if (!text.endsWith(StringConstants.SEMICOLON)) {
            text = text + StringConstants.SEMICOLON;
        }
        parse(text, false);
        List<String> list = ArrayListCache.getArrayList();

        try {
            SimpleNode currNode = getRootNode();
            findMbrs(currNode, list, null);
            return (list.toArray(new String[list.size()]));
        }
        finally {
            ArrayListCache.releaseList(list);
        }
    }

    public boolean hasCalcCommands(SimpleNode root) {
        
         * void calccommand():{} { <CALC> calccmd1() ";" } void calccmd1():{} {
         * calcall() | dimfunc() | idname() }
         
        if (root.getId() == CLBParseTreeConstants.JJTCALCCMD1) {
            SimpleNode child = (SimpleNode) root.jjtGetChild(0);
            if (child.getId() == CLBParseTreeConstants.JJTCALCALL || child.getId() == CLBParseTreeConstants.JJTDIMFUNC) {
                return true;
            }
        }
        else {
            for (int i = 0; i < root.jjtGetNumChildren(); i++) {
                boolean inner = hasCalcCommands((SimpleNode) root.jjtGetChild(i));
                if (inner)
                    return true;
            }
        }

        return false;
    }

    public void findMembers(Object currNode, List<String> list) {
        if (currNode instanceof SimpleNode) {
            findMbrs((SimpleNode) currNode, list, null);
        }
    }

    public void findMbrs(SimpleNode currNode, List<String> list, List<String> xmbrs) {
        SimpleNode child;

        int count = currNode.jjtGetNumChildren();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                child = (SimpleNode) currNode.jjtGetChild(i);
                if ((child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTMBRNAME) || (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTDIMNAME)) {
                    if (child.jjtGetNumChildren() == 0) {
                        list.add(child.toString().trim());
                    }
                    else { // qualifiedMbrName() | reference()
                        SimpleNode childChild = (SimpleNode) child.jjtGetChild(0);
                        if (childChild.getId() == CLBParseTreeConstants.JJTQUALIFIEDMBRNAME) {
                            String mbrName = child.toString().trim();
                            if (list != null)
                                list.add(mbrName);
                        }
                    }
                }
                else if (child.getId() == CLBParseTreeConstants.JJTXMBR && xmbrs != null) {
                    xmbrs.add(child.toString().trim());
                    findMbrs(child, list, xmbrs);
                }
                else if (child.jjtGetNumChildren() > 0) {
                    findMbrs(child, list, xmbrs);
                }
            }
        }
    }

    @Override
    public void getClauses(String text, ArrayList<String> clauseList) throws ParsingException {
        parse(text, false);
        SimpleNode curr = getRootNode();
        findClauses(curr, clauseList);
    }

    @Override
    public String[] getClauses(Object curr, ArrayList<String> clauseList) {
        if (curr instanceof SimpleNode) {
            return (findClauses((SimpleNode) curr, clauseList));
        }

        return (Util.EMPTY_STRINGS);
    }

    public String[] findClauses(SimpleNode curr, ArrayList<String> clauseList) {
        SimpleNode child;
        if (curr.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTCONDSTUFF) {
            for (int i = 0; i <= curr.jjtGetNumChildren() - 1; i++) {
                child = (SimpleNode) curr.jjtGetChild(i);
                if (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTPARENEXP) {
                    for (int j = 0; j < child.jjtGetNumChildren(); j++) {
                        SimpleNode child2 = (SimpleNode) child.jjtGetChild(j);
                        if (child2.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTEXPRESSION)
                            for (int k = 0; k <= child2.jjtGetNumChildren() - 1; k++) {
                                SimpleNode child3 = (SimpleNode) child2.jjtGetChild(k);
                                if (child3.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTDISJUNCTION) {
                                    if (child3.jjtGetNumChildren() > 1)
                                        clauseList.add("or");
                                    else
                                        for (int m = 0; m <= child3.jjtGetNumChildren() - 1; m++) {
                                            SimpleNode child4 = (SimpleNode) child3.jjtGetChild(m);
                                            if (child4.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTCONJUNCTION)
                                                for (int n = 0; n <= child4.jjtGetNumChildren() - 1; n++) {
                                                    clauseList.add("and");
                                                    SimpleNode child5 = (SimpleNode) child4.jjtGetChild(n);
                                                    if (child5.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTCOMPARISON)
                                                        clauseList.add(child5.toString());
                                                }
                                        }
                                }
                            }
                    }
                }
            }
        }
        else {
            if (curr.jjtGetNumChildren() > 0)
                for (int z = 0; z < curr.jjtGetNumChildren(); z++) {
                    findClauses((SimpleNode) curr.jjtGetChild(z), clauseList);
                }
        }
        return clauseList.toArray(Util.EMPTY_STRINGS);
    }
    public String setOuterFix(String script, String fixMembers) {
        parse(script, false);
        SimpleNode curr = getRootNode();
        
        setOuterFix(curr, fixMembers);
        return (curr.toStringWithComments());
    }
    
    public void setOuterFix(SimpleNode curr, String fixMembers) {
        if (curr.getId() == CLBParseTreeConstants.JJTFIXBLOCK) {
            StringBuilder b = new StringBuilder();
            b.append("FIX(");
            b.append(fixMembers);
            b.append(")\n");
            b.append(curr.toStringWithComments());
            b.append("\nENDFIX");
            updateNodeWithValue(curr, b.toString());
            return;
        }
        
        int children = curr.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) curr.jjtGetChild(i);
            setOuterFix(child, fixMembers);
        }
    }

    public static void updateNodeWithValue(SimpleNode node, String value) {
        Token first = node.first_token;
        Token old_last = node.last_token;
        first.image = value;
        first.next = old_last.next;
        node.last_token = first;
    }

    public void replaceEssbaseVariables(Object node, Map<String, ?> values) throws Exception {
        if (!(node instanceof SimpleNode))
            throw new IllegalArgumentException("Argument node must of of type SimpleNode");

        replaceEssbaseVariables((SimpleNode) node, values);
    }

    public void replaceEssbaseVariables(SimpleNode node, Map<String, ?> values) throws ParseException {
        if (node.getId() == CLBParseTreeConstants.JJTSUBSTVARIABLE) {
            Token token = node.first_token;
            
            if ((token.image != null) && (token.image.length() > 1)) {
                String varname = token.image.substring(1, token.image.length());

                Object var = values.get(varname.toUpperCase());
                if (var != null) {
                    token.image = var.toString();
                }
            }
            
            return;
        }

        int children = node.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            replaceEssbaseVariables(child, values);
        }
    }

    @Override
    public void replaceVariables(Object node, Map<String, ?> values) throws Exception {
        if (!(node instanceof SimpleNode))
            throw new IllegalArgumentException("Argument node must of of type SimpleNode");

        replaceVariables((SimpleNode) node, values, null);
    }

    public void replaceVariables(SimpleNode node, Map<String, ?> values, ExpressionTranslator translator) throws Exception {
//        if (node == this.rootNode) {
//            if (this.hasGroovy) {
//                replaceGroovyVariables(this.rootNode, values);
//            }
//        }
        
        if (node.getId() == CLBParseTreeConstants.JJTREFERENCE ||node.getId() == CLBParseTreeConstants.JJTPLANNINGEXPRESSIONSTMT) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if ((translator != null) && (child.getId() == CLBParseTreeConstants.JJTPLANNINGEXPRESSION)) {
                String expression = replaceVariablesInPlanningExpression(node, values);
                expression = translator.translateExpression(expression);
                if (expression != null) {
                    updateNodeWithValue(node, expression);
                }
            }
            else {
                replaceVariable(node, values);
            }
        }

        int children = node.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            replaceVariables(child, values, translator);
        }
        
        if (node.getId() == CLBParseTreeConstants.JJTINPUT) {
            RunJavaHelper.updateVariablesInRunJava(node, values);
        }
    }
    
    private String replaceVariablesInPlanningExpression(SimpleNode node, Map<String, ?> values) throws ParseException {
        StringBuilder retval = StringBufferCache.getBuffer();
        StringBuilder tokenImage = StringBufferCache.getBuffer();

        try {
            Token token = node.first_token;
            String expression = token.name;
            // save off the orginal string. we want to rebuild the everytime
            // the rule is run.
            if (expression == null) {
                expression = token.image.substring(2, token.image.length() - 2);
                token.name = expression;
            }
            if (expression != null && expression.trim().length() > 0) {
                JavaParser jp = new JavaParser(new StringReader(expression));
                try {
                    jp.planningexpression();
                }
                catch (com.hyperion.calcmgr.plugin.essbase.java.ParseException e) {
                    throw new ParseException(e.getMessage());
                }
    
                com.hyperion.calcmgr.plugin.essbase.java.Token toke = jp.first_token;
                while (toke != null) {
                    // we will replace it with the value of the variable
                    if (toke.kind == JavaParserConstants.REFERENCE_TO_PARAMETER) {
                        String varname = toke.image.substring(1, toke.image.length() - 1);
                        String value = Util.EMPTY_STRING;
                        Object var = values.get(varname.toUpperCase());
                        if (var != null) {
                            value = getObjectValue(var);
                        }
    
                        if (value != null) {
                            toke.image = value;
                        }
                    }
                    toke = toke.next;
                }
    
                toke = jp.first_token;
                while (toke != null) {
                    tokenImage.setLength(0);
                    for (com.hyperion.calcmgr.plugin.essbase.java.Token currSpecial = toke.specialToken; currSpecial != null; currSpecial = currSpecial.specialToken) {
                        if (currSpecial.image != null && currSpecial.image.length() > 0)
                            tokenImage.insert(0, currSpecial.image);
                    }
                    if (toke.image != null && toke.image.length() > 0) {
                        tokenImage.append(toke.image);
                    }
                    retval.append(tokenImage.toString());
                    toke = toke.next;
                }
            }
            
            return (retval.toString());
        }
        finally {
            StringBufferCache.releaseBuffer(tokenImage);
            StringBufferCache.releaseBuffer(retval);
        }
    }
    
    // The Map keys are expected to be in all uppercase
    private void replaceVariable(SimpleNode node, Map<String, ?> values) {
        if (node.first_token.kind == CLBParseConstants.ID || node.first_token.kind == CLBParseConstants.hfm_VARIABLE) {
            Token token = node.first_token;
            String varname = token.name;
            String value = null;
            if (varname == null) {
                varname = token.image.substring(0, token.image.length() - 1);
                token.name = varname;
            }

            char ch = varname.charAt(0);
            if ((ch == VAR_START) || (ch == DTP_START)) {
                varname = varname.substring(1);

                Object var = values.get(varname.toUpperCase());
                if (var != null) {
                    value = getObjectValue(var);
                }

                if (value != null) {
                    token.image = value;
                }
            }
        }
    }

    public void replacePlanningExpressions(SimpleNode node, Map<String, ?> planningExpressionValues) throws ParseException{
        if (node.getId() == CLBParseTreeConstants.JJTREFERENCE) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if (child.getId() == CLBParseTreeConstants.JJTPLANNINGEXPRESSION) {
                String expression = replaceVariablesInPlanningExpression(node, planningExpressionValues);
                String expressionValue = (String)planningExpressionValues.get(expression);
                if (expressionValue != null) {
                    updateNodeWithValue(node, expressionValue);
                }
            }
        }   
        int children = node.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            replacePlanningExpressions(child, planningExpressionValues);
        }
    }
    
    public void loadPlanningExpressions(SimpleNode node, Map<String, ?> values, Set<String> planningExpressions) throws ParseException{
        if (node.getId() == CLBParseTreeConstants.JJTREFERENCE) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if ((child.getId() == CLBParseTreeConstants.JJTPLANNINGEXPRESSION)) {
                String expression = replaceVariablesInPlanningExpression(node, values);
                planningExpressions.add(expression);
            }
            else {
                if (values != null) {
                    replaceVariable(node, values);
                }
            }
        }

        int children = node.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            if (node.getId() != CLBParseTreeConstants.JJTMACROBLOCK) {
                loadPlanningExpressions(child, values, planningExpressions);
            }
        }
    }

    *//**
     * Overirde this if you have a different implementation
     * 
     * @param object
     * @return
     *//*
    static String getObjectValue(Object object) {
        return (Util.invokeMethodString(object, StringConstants.FUNC_GETVALUE, Util.toString(object)));
    }

    @Override
    public Function[] getAllFunctions(String text) throws ParsingException {
        text = text.trim();

        if (!text.endsWith(StringConstants.SEMICOLON)) {
            text = text + StringConstants.SEMICOLON;
        }
        parse(text, false);
        return getFunctions();
    }

    @Override
    public Function[] getFunctions() {
        return (getFunctions(false));
    }

    public Function[] getFunctions(boolean includeNestedFunctions) {
        ParserToken[] tokens = getFunctionTokens(includeNestedFunctions);
        if (Util.isEmpty(tokens)) {
            return (Function.EMPTY_ARRAY);
        }

        Function[] functions = new Function[tokens.length];
        for (int i = 0; i < functions.length; i++) {
            functions[i] = (Function) tokens[i].getObject();
        }

        return (functions);
    }

    public ParserToken[] getFunctionTokens() {
        return (getFunctionTokens(false));
    }

    public ParserToken[] getFunctionTokens(boolean includeNestedFunctions) {
        List<ParserToken> functionList = ArrayListCache.getArrayList();

        try {
            findFunctions(null, functionList, includeNestedFunctions);
            return (functionList.toArray(new ParserToken[functionList.size()]));
        }
        finally {
            ArrayListCache.releaseList(functionList);
        }
    }

    public void findFunctions(SimpleNode curr, List<ParserToken> functionList, boolean includeNestedFunctions) {
        if (curr == null)
            curr = this.rootNode;
        
         * void function():{} functionname() parmlist() void functionname():{}
         * <FUNCTION> void parmlist():{} "(" [ parm()] ( "," [parm()] )* ")"
         

        // SimpleNode child;
        if (curr.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTFUNCTION) {
            Function function = new Function();
            ParserToken token = getToken(curr, function);

            SimpleNode functionName = (SimpleNode) curr.jjtGetChild(0);
            function.setName(functionName.first_token.image);

            SimpleNode parmList = (SimpleNode) curr.jjtGetChild(1);
            // commas and parens are owned by the parmlist node, not its
            // children

            Token currTok = parmList.first_token;
            int currChild = 0;
            // int parmCount = 0;
            SimpleNode parm = null;
            boolean expectingParm = true;
            boolean commaFound = false;
            while (currTok != null) {
                if (currChild < parmList.jjtGetNumChildren()) {
                    parm = (SimpleNode) parmList.jjtGetChild(currChild);
                }
                if (currTok.kind == CLBPARSE_TOKEN_COMMA) {
                    commaFound = true;
                    if (expectingParm)
                        function.addArgument(new Argument(Util.EMPTY_STRING));
                    expectingParm = true;
                }
                if (parm != null && currTok == parm.first_token) {
                    function.addArgument(new Argument(parm.toString()));
                    if (includeNestedFunctions) {
                        findFunctions(parm, functionList, includeNestedFunctions);
                    }
                    expectingParm = false;
                    currTok = parm.last_token;
                    currChild++;
                    parm = null;
                }
                if (currTok == parmList.last_token)
                    currTok = null;
                else
                    currTok = currTok.next;
            }
            if (expectingParm && commaFound)
                function.addArgument(new Argument(Util.EMPTY_STRING));

            functionList.add(token);
        }
        else
            for (int i = 0; i < curr.jjtGetNumChildren(); i++) {
                findFunctions((SimpleNode) curr.jjtGetChild(i), functionList, includeNestedFunctions);
            }
    }

    // private int countChars(String asString, char c) {
    // int retval=0;
    // for (int i=0; i < asString.length();i++)
    // if (asString.charAt(i)==c) retval++;
    // return retval;
    // }

    @Override
    public String[] getMembersByFix(String text) throws ParsingException {
        List<String> list = ArrayListCache.getArrayList();

        try {
            text = createFixStatement(text);
            parse(text, true);
            findFixMembers(this.rootNode, list);
            return (list.toArray(new String[list.size()]));
        }
        finally {
            ArrayListCache.releaseList(list);
        }
    }

    public void findFixMembers(List<String> list) {
        findFixMembers(this.rootNode, list);
    }

    public void findFixMembers(SimpleNode currNode, List<String> list) {
        SimpleNode child;
        // String text = (currNode != null) ? currNode.toString() :
        // Util.EMPTY_STRING;
        int parentID = currNode.getId();

        int count = currNode.jjtGetNumChildren();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                child = (SimpleNode) currNode.jjtGetChild(i);
                if (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTFIXEXP) {
                    if (parentID == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTFIXLIST) {
                        list.add(child.toString().trim());
                    }
                }
                else if (child.jjtGetNumChildren() > 0) {
                    findFixMembers(child, list);
                }
            }
        }
    }

    public String[] findScriptVariables(String script) {
        parse(script, false);
        return (findScriptVariables());
    }

    public void findScriptVariables(String script, Map<String, String> map) {
        parse(script, false);
        findScriptVariables(this.rootNode, map);
    }

    public String[] findScriptVariables() {
        Map<String, String> map = CHashMapCache.getMap();

        try {
            findScriptVariables(map);
            return (Util.mapToStringArray(map));
        }
        finally {
            CHashMapCache.releaseMap(map);
        }
    }

    *//**
     * @param varNames
     *//*
    @Override
    public void findScriptVariables(Map<String, String> varNames) {
        findScriptVariables(this.rootNode, varNames);
    }

    private void findScriptVariables(SimpleNode currNode, Map<String, String> varNames) {
        SimpleNode child;
        // String text = (currNode != null) ? currNode.toString() :
        // Util.EMPTY_STRING;
        int parentID = currNode.getId();

        int count = currNode.jjtGetNumChildren();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                child = (SimpleNode) currNode.jjtGetChild(i);
                if (child.getId() == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTIDNAME) {
                    if (((parentID == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTVARNAME) || (parentID == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTARRAYNAME)) && (child.jjtGetNumChildren() == 0)) {
                        String varName = child.toString().trim();
                        // System.out.println("Parent ID: " + currNode.getId() +
                        // " Name: " +
                        // com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.jjtNodeName[currNode.getId()]);
                        varNames.put(varName, varName);
                    }
                }
                else if (child.jjtGetNumChildren() > 0) {
                    findScriptVariables(child, varNames);
                }
            }
        }
    }

    public String Script2Rule(String rulename, String app, String plantype, String text, int RuleID, Map<String, String[]> macroInfos, IFixResolver fixer) {
        Import importer = new Import(app, plantype, rulename, RuleID, fixer);
        parse(text, false);
        StringWriter stringwriter = new StringWriter(100);
        importer.importCalcScript((SimpleNode) getRoot(), new PrintWriter(stringwriter), macroInfos);
        return stringwriter.toString();
    }

    public String Script2Rule(String rulename, String app, String plantype, Reader reader, int RuleID, Map<String, String[]> macroInfos, IFixResolver fixer) {
        StringWriter stringwriter = new StringWriter(100);
        Script2Rule(rulename, app, plantype, reader, RuleID, macroInfos, fixer, stringwriter);
        return stringwriter.toString();
    }

    public void Script2Rule(String rulename, String app, String plantype, Reader reader, int RuleID, Map<String, String[]> macroInfos, IFixResolver fixer, Writer writer) {
        Import importer = new Import(app, plantype, rulename, RuleID, fixer);
        parse(reader, false);

        importer.importCalcScript((SimpleNode) getRoot(), new PrintWriter(writer), macroInfos);
    }

    public String Script2Rule(String rulename, String app, String plantype, String text) throws ParsingException {
        return Script2Rule(rulename, app, plantype, text, 1, null, null);
    }

    public String Script2Rule(String rulename, String app, String plantype, String text, IFixResolver fixer) throws ParsingException {
        return Script2Rule(rulename, app, plantype, text, 1, null, fixer);
    }

    public void Script2Rule(String rulename, String app, String plantype, Reader reader, IFixResolver fixer, Writer writer) throws ParsingException {
        Script2Rule(rulename, app, plantype, reader, 1, null, fixer, writer);
    }

    public String Script2Rule(String rulename, String app, String plantype, Reader reader, IFixResolver fixer) throws ParsingException {
        return Script2Rule(rulename, app, plantype, reader, 1, null, fixer);
    }

    // public String Script2Rule1(String rulename, String app, String
    // plantype,InputStream textFile, Map<String, String[]> macInfos) throws
    // ParsingException {
    // Import importer = new Import(app, plantype, rulename);
    // parse(textFile, false);
    // StringWriter stringwriter = new StringWriter(100);
    // importer.importCalcScript((SimpleNode)getRoot(), new
    // PrintWriter(stringwriter), macInfos);
    // return stringwriter.toString();
    // }

    public String ScriptComponent2Rule(String rulename, String app, String plantype, String script, Map<String, String[]> macInfos, IFixResolver fixer) throws ParsingException {
        Import importer = new Import(app, plantype, rulename, fixer);
        CLBParse parser = new CLBParse(new StringReader(script));
        parser.setValidateAll(false);
        SimpleNode compRoot = null;
        try {
            compRoot = parser.scriptComponentForEdit();
        }
        catch (ParseException e) {
            ThrowParsingException(e);
        }
        StringWriter stringwriter = new StringWriter(100);
        importer.importCalcScript(compRoot, new PrintWriter(stringwriter), macInfos);
        return stringwriter.toString();
    }

    *//**
     * @param text
     * @return
     * @throws ParsingException
     *//*
    public boolean hasOperator(String text) throws ParsingException {
        CLBParse parser = null;

        if (Util.isEmpty(text)) {
            return (false);
        }

        try {
            parser = new CLBParse(new StringReader(text));
            SimpleNode root = parser.assign();
            return (hasOperator(root));
        }
        catch (ParseException e) {
            ThrowParsingException(e);
        }
        catch (TokenMgrError e) {
            ThrowParsingException(parser, e);
        }
        catch (Throwable t) {
            throw new ParsingException(t.getMessage());
        }

        return (false);
    }

    *//**
     * @param curr
     * @return
     *//*
    public boolean hasOperator(SimpleNode curr) {
        if (curr == null) {
            curr = this.rootNode;
        }

        int id = curr.getId();

        if ((id == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTMULOP) || (id == com.hyperion.calcmgr.plugin.essbase.calcscript.CLBParseTreeConstants.JJTADDOP)) {
            return (true);
        }

        if (curr.jjtGetNumChildren() > 0) {
            for (int z = 0; z < curr.jjtGetNumChildren(); z++) {
                if (hasOperator((SimpleNode) curr.jjtGetChild(z))) {
                    return (true);
                }
            }
        }

        return (false);
    }

    public void dispose() {
        this.variablesUsed = null;
        this.parmsUsed = null;
        this.rootNode = null;
    }

    *//**
     * @param members
     * @return
     *//*
    public static String createFixStatement(String members) {
        StringBuilder buf = StringBufferCache.getBuffer();

        try {
            buf.append(StringConstants.FIX);
            buf.append(StringConstants.SPACE);
            buf.append(StringConstants.LEFT_PARAN);
            buf.append(members);
            buf.append(StringConstants.RIGHT_PARAN);
            buf.append(StringConstants.CR);
            buf.append(StringConstants.ENDFIX);

            return (buf.toString());
        }
        finally {
            StringBufferCache.releaseBuffer(buf);
        }
    }

    public static String replaceRTPs(String script, Properties rtps) throws ParseException {
        CLBParse parser = new CLBParse(new StringReader(script));

        SimpleNode root = parser.Input();
        for (Token curr = root.first_token; curr != null && curr != root.last_token; curr = curr.next) {
            if (curr.kind == CLBParseConstants.hfm_VARIABLE) {
                String name = curr.image.substring(1, curr.image.length() - 1).trim();
                if (rtps.getProperty(name) != null) {
                    curr.image = rtps.getProperty(name);
                }
            }
        }
        return root.toString();
    }

    private static void findAllNodeType(ArrayList<SimpleNode> list, SimpleNode curr, int id) {
        if (curr == null)
            return;

        if (curr.getId() == id) {
            list.add(curr);
            return;
        }

        for (int i = 0; i < curr.jjtGetNumChildren(); i++) {
            findAllNodeType(list, (SimpleNode) curr.jjtGetChild(i), id);
        }
    }

    public static boolean removeRuntimeSubvars(SimpleNode node) {
        if (node.getId() == CLBParseTreeConstants.JJTSETCOMMAND) {
            if (node.jjtGetNumChildren() > 0) {
                if (((SimpleNode) node.jjtGetChild(0)).getId() == CLBParseTreeConstants.JJTRUNTIMEPARAMETERS) {
                    
                    for (Token curr = node.first_token; (curr != null); curr = curr.next) {
                        curr.image = Util.EMPTY_STRING;
                        
                        for (Token currSpecial = curr.specialToken; currSpecial != null; currSpecial = currSpecial.specialToken) {
                            currSpecial.image = Util.EMPTY_STRING;
                        }
                        
                        if (curr == node.last_token) {
                            break;
                        }
                    }
                 
                    return (true);
                }
            }
            
            return (false);
        }
        
        int count = node.jjtGetNumChildren();
        for (int i = 0; i < count; i++) {
            if (removeRuntimeSubvars((SimpleNode) node.jjtGetChild(i))) {
                return (true);
            }
        }
        
        return (false);
    }
    
    public static ArrayList<RunTimeVariable> getRunTimeVariables(FileInputStream fis) throws ParseException {
        CLBParse parser = new CLBParse(fis);
        SimpleNode root = parser.Input();
        return getRunTimeVariables(root);
    }

    public static ArrayList<RunTimeVariable> getRunTimeVariables(String script) throws ParseException {
        CLBParse parser = new CLBParse(new StringReader(script));
        SimpleNode root = parser.Input();
        return getRunTimeVariables(root);
    }

    public static ArrayList<RunTimeVariable> getRunTimeVariables(SimpleNode root) {
        ArrayList<RunTimeVariable> retval = new ArrayList<RunTimeVariable>();

        ArrayList<SimpleNode> rtpDefDesc = new ArrayList<SimpleNode>();
        findAllNodeType(rtpDefDesc, root, CLBParseTreeConstants.JJTRUNTIME_PARAMETER_DEF_DESC);

        for (SimpleNode RunTimeVariableNode : rtpDefDesc) {
            SimpleNode runtime_parameter_definition = (SimpleNode) RunTimeVariableNode.jjtGetChild(0);

            RunTimeVariable RunTimeVariable = new RunTimeVariable();
            retval.add(RunTimeVariable);
            RunTimeVariable.setName(runtime_parameter_definition.first_token.image.trim());
            if (runtime_parameter_definition.jjtGetNumChildren() > 0)
                RunTimeVariable.setValue(runtime_parameter_definition.jjtGetChild(0).toString().trim());

            if (RunTimeVariableNode.jjtGetNumChildren() > 1) {
                SimpleNode runtime_parameter_description = (SimpleNode) RunTimeVariableNode.jjtGetChild(1);
                String hint = runtime_parameter_description.toString().trim();
                hint = hint.substring(hint.indexOf('>') + 1);
                hint = hint.substring(0, hint.lastIndexOf('<'));
                RunTimeVariable.setHint(hint);

            }

            
             * 
             * 
             * void runtime_parameter_def_desc() : {} {
             * runtime_parameter_definition() [runtime_parameter_description()]
             * }
             * 
             * void runtime_parameter_definition():{} {
             * 
             * 
             * // runtime_parameter_name [= value]
             * 
             * <ID> [<ASSIGN> expression()] }
             * 
             * void runtime_parameter_description():{} { // <RTP_HINT>
             * descriptions </RTP_HINT> (see 3.5 for explanation) <RTPHINT> }
             
        }

        return retval;
    }

    private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':', '\'' };

    public OptimizerWarnings getOptimizerWarnings(ICellFixer fixer, String script, Locale loc) {
        return getOptimizerWarnings(fixer, script, loc, false, null);

    }

    public OptimizerWarnings getOptimizerWarnings(ICellFixer fixer, String script, Locale loc, boolean forDesign) {
        return getOptimizerWarnings(fixer, script, loc, forDesign, null);

    }

    public OptimizerWarnings getOptimizerWarnings(ICellFixer fixer, String script, Locale loc, boolean forDesign, String ruleName) {
        return getOptimizerWarnings(fixer, script, loc, forDesign, -1, ruleName);
    }

    public PrintStream getDebugPrintStream(String ruleName, String appName, String bdName) {
        String debugDirStr = ConfigurationManager.getProperty("DEBUG_DIR");
        PrintStream debugOut = null;
        if (debugDirStr == null)
            debugDirStr = System.getProperty("CALCMGR_DEBUG_DIR");

        if (debugDirStr != null) {
            File debugDir = new File(debugDirStr);
            File tempOut = null;
            if (debugDir.exists() && debugDir.isDirectory()) {
                int i = 0;
                String filebase = "warnings";
                if (ruleName != null && ruleName.length() > 0) {
                    if (appName != null && appName.length() > 0)
                        filebase = appName;
                    else
                        filebase = "app";
                    filebase += "_";
                    if (bdName != null && bdName.length() > 0)
                        filebase += bdName;
                    else
                        filebase += "db";
                    filebase += "_";

                    filebase += ruleName;
                    for (char c : ILLEGAL_CHARACTERS) {
                        filebase = filebase.replace(c, '_');
                    }

                }
                tempOut = new File(debugDir, filebase + i);
                while (tempOut.exists() && i < 1000) {
                    i++;
                    tempOut = new File(debugDir, filebase + i);
                }
            }
            else
                try {
                    tempOut = File.createTempFile("CMwarnings", ".txt");
                }
                catch (IOException e1) {
                }
            if (tempOut != null)
                try {
                    debugOut = new PrintStream(new FileOutputStream(tempOut));
                }
                catch (FileNotFoundException e1) {
                }
        }
            return debugOut;
    }

    public OptimizerWarnings getOptimizerWarnings(ICellFixer fixer, String script, Locale loc, boolean forDesign, long expTimeOut, String ruleName) {
        long startTime=System.currentTimeMillis();

        CellCheck test = new CellCheck(fixer);
        String linesep = "\n";
        OptimizerWarnings retval = new OptimizerWarnings(loc);
        PrintStream debugOut = getDebugPrintStream(ruleName,fixer.getApp(),fixer.getDB());
       
            if (debugOut != null) {

                try {
                    if (ruleName != null)
                        debugOut.println(ruleName);
                    BufferedReader br = new BufferedReader(new StringReader(script));
                    int i = 1;
                    linesep = System.getProperty("line.separator", "\n");
                    String line;
                    while (br.ready()) {
                        line = br.readLine();
                        if (line == null)
                            break;
                        debugOut.println((i++) + ": " + line);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        
        IDryRunExporter drExporter = null;

        if (fixer instanceof IDryRunExporter)
            drExporter = (IDryRunExporter) fixer;
        try {
            if (debugOut != null) {
                debugOut.println("\n setuptime: " +(System.currentTimeMillis()-startTime));
                debugOut.println();
                startTime=System.currentTimeMillis();

            }

            test.FindCells(script);
            if (debugOut != null) {
                debugOut.println("\n find cells: " +(System.currentTimeMillis()-startTime));
                debugOut.println();
                startTime=System.currentTimeMillis();
            }
            int numpasses = test.globalPassCount;
            if (numpasses == 0)
                numpasses = 1;

            //int calcCmds = 0;
            BigInteger totalAffected = null;
            long totPotBlocks = 0;
            long totCntBlocks = 0;
            HashSet<Integer> fixesSeen = new HashSet<Integer>();

            ArrayList<OptimizerWarnings.CellWarning> errorCells = new ArrayList<OptimizerWarnings.CellWarning>();
            for (CellDef cell : test.getAllCells()) {
                Integer fixID = Integer.valueOf(cell.getFixID());
                // if (cell.getPassCount() > numpasses)
                // numpasses = cell.getPassCount();

                if (cell instanceof BadCalcModeDef){
                    BadCalcModeDef badCell = (BadCalcModeDef)cell;
                if( badCell.getSetTokenKind() == CLBParseConstants.CREATEBLOCKONEQ||
                badCell.getSetTokenKind() == CLBParseConstants.EMPTYMEMBERSETS) {
                    cell.setCountedBlocks(0);

                    errorCells.add(retval.newCellWarning(cell, false));

                    continue; //doesn't affect counts
                }
                }
                if (!(cell instanceof FixDef)) {
                if (totalAffected == null)
                    totalAffected = cell.countAffectedCells();
                else
                totalAffected = totalAffected.add(cell.countAffectedCells());
                }
                if (cell.isDataCmd() || (!(cell instanceof FixDef) || forDesign) && !fixesSeen.contains(fixID)) {
                    // if (debugOut!=null)
                    // debugOut.println("MDX: "+cell.getMDXquery().replace("\n",linesep));
                    // MDX is too slow when a lot of blocks exist

                    // datacommand act like their own fix
                    long potBlocks = 0;
                    if (cell instanceof FixDef) {
                    if (cell.dims.size() == 0 && ((FixDef)cell).emptyMemberSets_on) {
                        errorCells.add(retval.newCellWarning(cell, false));

                             continue; // don't add to counts
                    }
                    }
                            
                    HashMap<String, HashSet<String>> effectiveScope = cell.getEffectiveScope(!cell.isDataCmd(), true);

                     potBlocks = cell.countAffectedBlocks(effectiveScope);

                    long countedBlocks = 0;
                    if (drExporter != null) {
                        long start = System.currentTimeMillis();
                        countedBlocks = drExporter.countBlocks(cell.getEffectiveFixList(), fixer.getDenseDimensions(), expTimeOut);
                        // old way countedBlocks
                        // =drExporter.countBlocks(effectiveScope,fixer.getDenseDimensions());
                        long elapse = System.currentTimeMillis() - start;
                        System.out.println("elapsed time in ms: " + elapse + " cell mage: " + cell.getImage() + " line:" + cell.getBeginLine());

                    }

                    cell.setCountedBlocks(countedBlocks);
                    if (!(cell instanceof FixDef)) {
                        // only add to block count when on a statement,
                        // don't want nested fixes with no intermediate
                        // statements to
                        // to add to the count

                        totCntBlocks += countedBlocks;
                        if (potBlocks > 0) {
                            if (!cell.isDataCmd())
                                fixesSeen.add(fixID);
                            totPotBlocks += potBlocks;
                        }
                    }
                }
                else {

                    if (debugOut != null) {
                        debugOut.println("Following cell not counted, due to: " + linesep);
                        HashMap<String, HashSet<String>> effectiveScope = cell.getEffectiveScope(!cell.isDataCmd(), true);
                        cell.countAffectedBlocks(effectiveScope);
                        if (fixesSeen.contains(fixID)) {
                            debugOut.println("FixBlock " + cell.getFixID() + " already calculated " + linesep);

                        }

                        if (cell instanceof FixDef) {
                            debugOut.println("In Governor mode skipping fix statements" + linesep);
                        }
                    }
                }

                if (debugOut != null)
                    debugOut.println(cell.toString().replace("\n", linesep));

                if (cell instanceof BadCalcModeDef)
                    errorCells.add(retval.newCellWarning(cell, false));
                else if (cell instanceof FixDef)
                    errorCells.add(retval.newCellWarning(cell, false));
                else {
                    if (cell.isCalcCmd()) {
                        errorCells.add(retval.newCellWarning(cell, false));
                        //calcCmds++;
                    }
                    else if (cell.isAssMis())
                        errorCells.add(retval.newCellWarning(cell, false));
                    else if ((cell.getUnqualifiedSparseDims().size() > 0))
                        errorCells.add(retval.newCellWarning(cell, false));
                }

            }

            for (CellDef cell : test.getMismatched()) {
                errorCells.add(retval.newCellWarning(cell, true));
            }
            // acounted for in global passcount
            // numpasses += calcCmds; // calccommands cause a pass over DB

            retval.setTotalAffectedCells(totalAffected);
            retval.setTotalNumPasses(numpasses);
            retval.setNumAssignMisMatch(test.getAssignMisMatch());
            retval.setNumImproperDimUsage(test.getImproperDimUsage());
            retval.setCells(errorCells);
            retval.setTotalPotentialBlocks(totPotBlocks);
            retval.setTotalCountedBlocks(totCntBlocks);
            if (debugOut != null) {
                debugOut.println("\n count blocks: "+ (System.currentTimeMillis()-startTime));
                debugOut.println();
                startTime=System.currentTimeMillis();
            }
            if (debugOut != null) {
                StringBuilder buff = new StringBuilder("\n*************************************************");

                buff.append("\nTotalAffectedCells:" + retval.getTotalAffectedCells());
                buff.append("\nTotalNumPasses:" + retval.getTotalNumPasses());
                buff.append("\nNumAssignMisMatch:" + retval.getNumAssignMisMatch());
                buff.append("\nNumImproperDimUsage:" + retval.getNumImproperDimUsage());
                
                buff.append("\nTotalPotentialBlocks:" + retval.getTotalPotentialBlocks());
                buff.append("\nTotalCountedBlocks:" + retval.getTotalCountedBlocks());
                buff.append("\n********************");

                for(OptimizerWarnings.CellWarning errCel:errorCells)
                    buff.append(errCel.toString());
                buff.append("\n*************************************************");

                debugOut.println(buff.toString().replace("\n", linesep));
               
                // debugOut.println(retval);
            }
            
            if (System.getProperty("IDEM_CHECK")!=null) {
            
             * Idempotent testing
            
                if (debugOut != null ) {
                    debugOut.println("Idempotent checking:");
                }
            IdempotentChecker idCheck = new IdempotentChecker(fixer) ;
            IdempotentChecker.IdemCellDef[]  idErrCells = idCheck.checkIdempotent( script,debugOut );
            if (idErrCells != null) {
                retval.setIdempotent(false);
                    if (debugOut != null ) {
                        debugOut.println("Idempotent check: false");
                        debugOut.println("target:"+ idErrCells[0] + "\nsource:"+idErrCells[1]);
                    }
                    errorCells.add(retval.newCellWarning(idErrCells[0], idErrCells[1]));
                }
            }
        }
        catch (ParseException e) {
            return null;
        }
        finally {
            if (debugOut != null) {
                debugOut.close();
            }

        }
        return retval;
    }

    
    public void replaceGroovyVariables(SimpleNode node, Map<String, ?> values) throws Exception {
        if (node.getId() == CLBParseTreeConstants.JJTREFERENCE) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if (child.getId() == CLBParseTreeConstants.JJTGROOVYEXPRESSION) {
                String expression = replaceVariablesInGroovyExpression(node, values);
                if (expression != null) {
                    updateNodeWithValue(node, expression);
                }
            }
        }
        else if (node.getId() == CLBParseTreeConstants.JJTGROOVYEXPRESSION) {
            String expression = replaceVariablesInGroovyExpression(node, values);
            if (expression != null) {
                updateNodeWithValue(node, expression);
            }
        }

        int children = node.jjtGetNumChildren();
        for (int i = 0; i < children; i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            replaceGroovyVariables(child, values);
        }
    }

    private String replaceVariablesInGroovyExpression(SimpleNode node, Map<String, ?> values) throws Exception {
        Token token = node.first_token;
        String expression = token.name;
        // save off the orginal string. we want to rebuild the everytime
        // the rule is run.
        if (expression == null) {
            expression = token.image.substring(3, token.image.length() - 3);
            token.name = expression;
        }
        
        expression = VariableReplaceHelper.replaceVariables(null, expression, values, false);
        
        if ((expression != null) && !expression.isEmpty()) {
            Object object = GroovyInstance.getInstance().evaluate(expression);
            if (object != null) {
                return (object.toString());
            }
        }
        
        return (expression);
    }

    private void scanVariablesInGroovy(SimpleNode node, Map<String, Boolean> vars) {
        if (node.getId() == CLBParseTreeConstants.JJTGROOVYEXPRESSION) {
            Object[] variables = VariableParserHelper.parseText(node.toString(), null, false);
            
            for(Object object : variables) {
                vars.put(object.toString(), Boolean.TRUE);
            }
            
            return;
        }
        
        int count = node.jjtGetNumChildren();
        for (int i = 0; i < count; i++) {
            scanVariablesInGroovy((SimpleNode) node.jjtGetChild(i), vars);
        }
    }
    
    private void getGroovyNodes(SimpleNode node, List<SimpleNode> nodeList) {
        if (node.getId() == CLBParseTreeConstants.JJTGROOVYEXPRESSION) {
            nodeList.add(node);
            return;
        }
        
        int count = node.jjtGetNumChildren();
        for (int i = 0; i < count; i++) {
            getGroovyNodes((SimpleNode) node.jjtGetChild(i), nodeList);
        }
    }

    public SimpleNode[] getGroovyNodes() {
        if (!this.hasGroovy) {
            return (new SimpleNode[0]);
        }
        
        List<SimpleNode> nodeList = new LinkedList<SimpleNode>();
        getGroovyNodes(this.rootNode, nodeList);
        return nodeList.toArray(new SimpleNode[nodeList.size()]);
    }

    public boolean hasEmbeddedGroovy() {
        return this.hasGroovy;
    }
    
    public static class MultiMbrSelection {
        private String[] selected = Util.EMPTY_STRINGS;
        private String[] excluded = Util.EMPTY_STRINGS;

        public MultiMbrSelection(List<String> sel, List<String> exc) {
            this.selected = sel.toArray(this.selected);
            this.excluded = exc.toArray(this.excluded);
        }

        public MultiMbrSelection() {
        }

        public String[] getSelected() {
            return this.selected;
        }

        public String[] getExcluded() {
            return this.excluded;
        }
    }
}
*/