/*
* The Apache Software License, Version 1.1
*
*
* Copyright (c) 2003 The Apache Software Foundation.  All rights 
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer. 
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:  
*       "This product includes software developed by the
*        Apache Software Foundation (http://www.apache.org/)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Apache" and "Apache Software Foundation" must 
*    not be used to endorse or promote products derived from this
*    software without prior written permission. For written 
*    permission, please contact apache@apache.org.
*
* 5. Products derived from this software may not be called "Apache 
*    XMLBeans", nor may "Apache" appear in their name, without prior 
*    written permission of the Apache Software Foundation.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*
* This software consists of voluntary contributions made by many
* individuals on behalf of the Apache Software Foundation and was
* originally based on software copyright (c) 2000-2003 BEA Systems 
* Inc., <http://www.bea.com/>. For more information on the Apache Software
* Foundation, please see <http://www.apache.org/>.
*/

package org.apache.xmlbeans.impl.common;

import javax.xml.namespace.QName;
import org.apache.xmlbeans.xml.stream.XMLName;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.UnsupportedEncodingException;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaField;

public class QNameHelper
{
    private static final Map WELL_KNOWN_PREFIXES = buildWKP();

    public static XMLName getXMLName(QName qname)
    {
        if (qname == null)
            return null;
        
        return XMLNameHelper.forLNS( qname.getLocalPart(), qname.getNamespaceURI() );
    }
    
    public static QName forLNS(String localname, String uri)
    {
        if (uri == null)
            uri = "";
        return new QName(uri, localname);
    }

    public static QName forLN(String localname)
    {
        return new QName("", localname);
    }

    public static QName forPretty(String pretty, int offset)
    {
        int at = pretty.indexOf('@', offset);
        if (at < 0)
            return new QName("", pretty.substring(offset));
        return new QName(pretty.substring(at + 1), pretty.substring(offset, at));
    }

    public static String pretty(QName name)
    {
        if (name == null)
            return "null";

        if (name.getNamespaceURI() == null || name.getNamespaceURI().length() == 0)
            return name.getLocalPart();
        
        return name.getLocalPart() + "@" + name.getNamespaceURI();
    }

    private static final char[] hexdigits = new char[]
        {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    private static boolean isSafe(int c)
    {
        if (c >= 'a' && c <= 'z')
            return true;
        if (c >= 'A' && c <= 'Z')
            return true;
        if (c >= '0' && c <= '9')
            return true;
        return false;
    }

    // This produces a string which is a safe filename from the given string s.
    // To make it a safe filename, the following two transformations are applied:
    //
    // 1. First all non-ascii-alphanumeric characters are escaped using
    //    their UTF8 byte sequence, in the form _xx_xx_xx, for example,
    //    "Hello_20There" for "Hello There".  (Obviously, a single unicode
    //    character may expand into as many as three escape patterns.)
    //    If the resulting string is 64 characters or fewer, that's the result.
    //
    // 2. If the resulting string is longer than 64 characters, then it is
    //    discarded.  Instead, the SHA1 algorithm is run on the original
    //    string's UTF8 representation, and then the resulting 20-byte message
    //    digest is turned into a 40-character hex string; then "URI_SHA_1_" is
    //    prepended.
    //
    // The reason for the "shortening" is to avoid filenames longer than about
    // 256 characters, which are prohibited on Windows NT.
   
    public static final int MAX_NAME_LENGTH = 64;
    
    public static String hexsafe(String s)
    {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (isSafe(ch))
            {
                result.append(ch);
            }
            else
            {
                byte[] utf8 = null;
                try
                {
                    utf8 = s.substring(i, i + 1).getBytes("UTF-8");
                for (int j = 0; j < utf8.length; j++)
                {
                    result.append('_');
                    result.append(hexdigits[(utf8[j] >> 4) & 0xF]);
                    result.append(hexdigits[utf8[j] & 0xF]);
                }
            }
                catch(UnsupportedEncodingException uee)
                {
                    // should never happen - UTF-8 i always supported
                    result.append("_BAD_UTF8_CHAR");
                }
            }
        }
        
        // short enough? Done!
        if (result.length() <= MAX_NAME_LENGTH)
            return result.toString();
        
        // too long? use SHA1
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] inputBytes = null;
            try
            {
                inputBytes = s.getBytes("UTF-8");
            }
            catch(UnsupportedEncodingException uee)
            {
                // should never happen - UTF-8 is always supported
                inputBytes = new byte[0];
            }
            byte[] digest = md.digest(inputBytes);
            assert(digest.length == 20); // SHA1 160 bits == 20 bytes
            result = new StringBuffer("URI_SHA_1_");
            for (int j = 0; j < digest.length; j++)
            {
                result.append(hexdigits[(digest[j] >> 4) & 0xF]);
                result.append(hexdigits[digest[j] & 0xF]);
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("Using in a JDK without an SHA implementation");
        }
    }

    public static String hexsafedir(QName name)
    {
        if (name.getNamespaceURI() == null || name.getNamespaceURI().length() == 0)
            return "_nons/" + hexsafe(name.getLocalPart());
        return hexsafe(name.getNamespaceURI()) + "/" + hexsafe(name.getLocalPart());
    }

    private static Map buildWKP()
    {
        Map result = new HashMap();
        result.put("http://www.w3.org/XML/1998/namespace", "xml");
        result.put("http://www.w3.org/2001/XMLSchema", "xs");
        result.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        result.put("http://schemas.xmlsoap.org/wsdl/", "wsdl");
        result.put("http://schemas.xmlsoap.org/soap/encoding/", "soapenc");
        result.put("http://schemas.xmlsoap.org/soap/envelope/", "soapenv");
        return Collections.unmodifiableMap(result);
    }

    public static String readable(SchemaType sType)
    {
        return readable(sType, WELL_KNOWN_PREFIXES);
    }

    public static String readable(SchemaType sType, Map nsPrefix)
    {
        if (sType.getName() != null)
        {
            return readable(sType.getName(), nsPrefix);
        }
        
        if (sType.isAttributeType())
        {
            return "attribute type " + readable(sType.getAttributeTypeAttributeName(), nsPrefix);
        }
        
        if (sType.isDocumentType())
        {
            return "document type " + readable(sType.getDocumentElementName(), nsPrefix);
        }
        
        if (sType.isNoType() || sType.getOuterType() == null)
        {
            return "invalid type";
        }
        
        SchemaType outerType = sType.getOuterType();
        SchemaField container = sType.getContainerField();
        
        if (outerType.isAttributeType())
        {
            return "type of attribute " + readable(container.getName(), nsPrefix);
        }
        else if (outerType.isDocumentType())
        {
            return "type of element " + readable(container.getName(), nsPrefix);
        }
            
        if (container != null)
        {
            if (container.isAttribute())
            {
                return "type of " + container.getName().getLocalPart() + " attribute in " + readable(outerType, nsPrefix);
            }
            else
            {
                return "type of " + container.getName().getLocalPart() + " element in " + readable(outerType, nsPrefix);
            }
        }
        
        if (outerType.getBaseType() == sType)
            return "base type of " + readable(outerType, nsPrefix);
        else if (outerType.getSimpleVariety() == SchemaType.LIST)
            return "item type of " + readable(outerType, nsPrefix);
        else if (outerType.getSimpleVariety() == SchemaType.UNION)
            return "member type " + sType.getAnonymousUnionMemberOrdinal() + " of " + readable(outerType, nsPrefix);
        else
            return "inner type in " + readable(outerType, nsPrefix); 
    }
    
    public static String readable(QName name)
    {
        return readable(name, WELL_KNOWN_PREFIXES);
    }

    public static String readable(QName name, Map prefixes)
    {
        if (name.getNamespaceURI().length() == 0)
            return name.getLocalPart();
        String prefix = (String)prefixes.get(name.getNamespaceURI());
        if (prefix != null)
            return prefix + ":" + name.getLocalPart();
        return name.getLocalPart() + " in namespace " + name.getNamespaceURI();
    }
    
    public static String suggestPrefix(String namespace)
    {
        String result = (String)WELL_KNOWN_PREFIXES.get(namespace);
        if (result != null)
            return result;
        
        int len = namespace.length();
        int i = namespace.lastIndexOf('/');
        if (i > 0 && i == namespace.length() - 1)
        {
            len = i;
            i = namespace.lastIndexOf('/', i - 1);
        }
        
        i += 1; // skip '/', also covers -1 case.
        
        if (namespace.startsWith("www.", i))
        {
            i += 4; // "www.".length()
        }
        
        while (i < len)
        {
            if (XMLChar.isNCNameStart(namespace.charAt(i)))
                break;
            i += 1;
        }
        
        for (int end = i + 1; end < len; end += 1)
        {
            if (!XMLChar.isNCName(namespace.charAt(end)) || !Character.isLetterOrDigit(namespace.charAt(end)))
            {
                len = end;
                break;
            }
        }
        
        // prefixes starting with "xml" are forbidden, so change "xmls" -> "xs"
        if (namespace.length() >= i + 3 && startsWithXml(namespace, i))
        {
            if (namespace.length() >= i + 4)
                return "x" + Character.toLowerCase(namespace.charAt(i + 3));
            return "ns";
        }
        
        if (len - i > 4) // four or less? leave it.
        {
            if (isVowel(namespace.charAt(i + 2)) && !isVowel(namespace.charAt(i + 3)))
                len = i + 4;
            else
                len = i + 3; // more than four? truncate to 3.
        }
        
        int n;
        
        if (len - i == 0)
            return "ns";
        
        return namespace.substring(i, len).toLowerCase();
    }
    
    private static boolean startsWithXml(String s, int i)
    {
        if (s.length() < i + 3)
            return false;
        
        if (s.charAt(i) != 'X' && s.charAt(i) != 'x')
            return false;
        if (s.charAt(i + 1) != 'M' && s.charAt(i + 1) != 'm')
            return false;
        if (s.charAt(i + 2) != 'L' && s.charAt(i + 2) != 'l')
            return false;
        
        return true;
    }
    
    private static boolean isVowel(char ch)
    {
        switch (ch)
        {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
                return true;
            default:
                return false;
        }
    }
    
    public static String namespace(SchemaType sType)
    {
        while (sType != null)
        {
            if (sType.getName() != null)
                return sType.getName().getNamespaceURI();
            if (sType.getContainerField() != null && sType.getContainerField().getName().getNamespaceURI().length() > 0)
                return sType.getContainerField().getName().getNamespaceURI();
            sType = sType.getOuterType();
        }
        return "";
    }
}
