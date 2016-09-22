/**
 * File: JPEGException.java
 *
 *
 * Date         Author                   Changes
 * Aug 21 01    Andreas Putz             Created
 */
package jstego;

import java.io.IOException;

/*-----------------------------------------------------------------------------
 * jfor - Open-Source XSL-FO to RTF converter - see www.jfor.org
 *
 * ====================================================================
 * jfor Apache-Style Software License.
 * Copyright (c) 2002 by the jfor project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 * if any, must include the following acknowledgment:
 * "This product includes software developed
 * by the jfor project (http://www.jfor.org)."
 * Alternately, this acknowledgment may appear in the software itself,
 * if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "jfor" must not be used to endorse
 * or promote products derived from this software without prior written
 * permission.  For written permission, please contact info@jfor.org.
 *
 * 5. Products derived from this software may not be called "jfor",
 * nor may "jfor" appear in their name, without prior written
 * permission of info@jfor.org.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE JFOR PROJECT OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * Contributor(s):
-----------------------------------------------------------------------------*/
/**
 * Exception if error occured in JPEG encoder.
 *  @author Andreas Putz a.putz@skynamics.com
 */
//------------------------------------------------------------------------------
// $Id: JPEGException.java,v 1.5 2003/07/17 14:43:55 bdelacretaz Exp $
// $Log: JPEGException.java,v $
// Revision 1.5  2003/07/17 14:43:55  bdelacretaz
// V0.7.2dev-i - jpeg encoder refactored, encoder license files added
//
// Revision 1.4  2003/07/17 14:07:31  bdelacretaz
// V0.7.2.dev-h - jpeg encoder package refactoring
//
// Revision 1.3  2002/07/12 08:08:31  bdelacretaz
// License changed to jfor Apache-style license
//
// Revision 1.2  2001/08/31 07:51:01  bdelacretaz
// MPL license text added + javadoc class comments corrected
//
// Revision 1.1  2001/08/29 13:27:51  bdelacretaz
// V0.4.1 - base package name changed to org.jfor.jfor
//
// Revision 1.4  2001/08/27 20:35:05  putzi
// Bigfix in external graphic, insert alignment in paragraph, Converter option.
//
// Revision 1.2  2001/08/21 15:58:02  bdelacretaz
// V0.3.5, jpeg and gif support added
//
// Revision 1.1  2001/08/21 15:18:01  bdelacretaz
// V0.3.4 - bookmarks and links added
//
// Revision 1.1.1.1  2001/08/02 12:53:45  bdelacretaz
// initial SourceForge checkin of V0.1 code
//
//------------------------------------------------------------------------------
public class JPEGException extends IOException {

    /** Exception */
    private Exception e = null;
    /** Message */
    private String message = null;

    /**
     * Constructor.
     *
     * @param message Error message
     */
    public JPEGException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * Constructor.
     *
     * @param message Error message
     */
    public JPEGException(Exception e) {
        this(e.getMessage());
        this.e = e;
    }

    /**
     * Gets the exception.
     *
     * @return Exception
     */
    public Exception getException() {
        return e;
    }

    /**
     * Modifies the message.
     *
     * @param message Message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}

