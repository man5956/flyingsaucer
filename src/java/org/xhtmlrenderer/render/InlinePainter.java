/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import org.xhtmlrenderer.layout.Context;
import org.xhtmlrenderer.layout.InlineLayout;
import org.xhtmlrenderer.layout.Layout;
import org.xhtmlrenderer.layout.LayoutFactory;
import org.xhtmlrenderer.util.GraphicsUtil;


/**
 * Description of the Class
 *
 * @author    jmarinacci
 * @created   August 30, 2004
 */

public class InlinePainter {

    /**
     * Description of the Method
     *
     * @param c    Description of the Parameter
     * @param box  Description of the Parameter
     */

    public static void paintInlineContext( Context c, Box box ) {
        BlockBox block = (BlockBox)box;
        // translate into local coords
        // account for the origin of the containing box
        c.translate( box.x, box.y );
        // for each line box
        c.getGraphics().setColor( Color.black );
        //u.p("line count = " + block.boxes.size());
        for ( int i = 0; i < block.getChildCount(); i++ ) {
            // get the line box
            //u.p("line = " + block.boxes.get(i));
            paintLine( c, (LineBox)block.getChild( i ) );
        }
        // translate back to parent coords
        c.translate( -box.x, -box.y );
    }


    /**
     * Description of the Method
     *
     * @param c       Description of the Parameter
     * @param inline  Description of the Parameter
     * @param lx      Description of the Parameter
     * @param ly      Description of the Parameter
     */

    public static void paintSelection( Context c, InlineBox inline, int lx, int ly ) {
        if ( c.inSelection( inline ) ) {
            int dw = inline.width - 2;
            int xoff = 0;
            if ( c.getSelectionEnd() == inline ) {
                dw = c.getSelectionEndX();
            }
            if ( c.getSelectionStart() == inline ) {
                xoff = c.getSelectionStartX();
            }
            c.getGraphics().setColor( new Color( 200, 200, 255 ) );
            ( (Graphics2D)c.getGraphics() ).setPaint( new GradientPaint(
                    0, 0, new Color( 235, 235, 255 ),
                    0, inline.height / 2, new Color( 190, 190, 235 ),
                    true ) );
            /*
             * u.p("height = " + inline.height);
             * u.p("baseline = " + inline.baseline);
             * u.p("font = " + inline.getFont());
             * FontUtil.dumpFontMetrics(inline.getFont(),c.getGraphics());
             */
            FontMetrics fm = c.getGraphics().getFontMetrics( inline.getFont() );
            int top = ly + inline.y - fm.getAscent();
            int height = fm.getAscent() + fm.getDescent();
            c.getGraphics().fillRect(
                    lx + inline.x + xoff,
                    top, //ly + inline.y - inline.height,
                    dw - xoff,
                    height );//inline.height);
        }
    }


    /**
     * Description of the Method
     *
     * @param c     Description of the Parameter
     * @param line  Description of the Parameter
     */

    private static void paintLine( Context c, LineBox line ) {
        //u.p("painting line = " + line);
        // get x and y
        int lx = line.x;
        int ly = line.y + line.baseline;
        // for each inline box
        for ( int j = 0; j < line.getChildCount(); j++ ) {
            paintInline( c, (InlineBox)line.getChild( j ), lx, ly, line );
        }
        if ( c.debugDrawLineBoxes() ) {
            GraphicsUtil.drawBox( c.getGraphics(), line, Color.blue );
        }
    }

    // inlines are drawn vertically relative to the baseline of the line,
    // not relative to the origin of the line.
    // they *are* drawn horizontally (x) relative to the origin of the line
    // though

    /**
     * Description of the Method
     *
     * @param c       Description of the Parameter
     * @param inline  Description of the Parameter
     * @param lx      Description of the Parameter
     * @param ly      Description of the Parameter
     * @param line    Description of the Parameter
     */

    private static void paintInline( Context c, InlineBox inline, int lx, int ly, LineBox line ) {
        if ( InlineLayout.isReplaced( inline.node ) ) {
            //u.p("painting a replaced block: " + inline);
            c.translate( line.x, line.y + ( line.baseline - inline.height ) );
            Renderer rend = LayoutFactory.getRenderer( inline.node );
            //u.p("inline node = " + inline.node);
            //u.p("got the layout: " + layout);
            rend.paint( c, inline );
            c.translate( -line.x, -( line.y + ( line.baseline - inline.height ) ) );
            if ( c.debugDrawInlineBoxes() ) {
                GraphicsUtil.draw( c.getGraphics(), new Rectangle( lx + inline.x + 1, ly + inline.y + 1 - inline.height,
                        inline.width - 2, inline.height - 2 ), Color.green );
            }
            return;
        }

        if ( InlineLayout.isFloatedBlock( inline.node, c ) ) {
            Rectangle oe = c.getExtents();
            c.setExtents( new Rectangle( oe.x, 0, oe.width, oe.height ) );
            int xoff = line.x + inline.x;
            int yoff = line.y + ( line.baseline - inline.height ) + inline.y;
            c.translate( xoff, yoff );
            Renderer rend = LayoutFactory.getRenderer( inline.node );
            rend.paint( c, inline.sub_block );
            c.translate( -xoff, -yoff );
            c.setExtents( oe );
            return;
        }

        if ( inline.is_break ) {
            return;
        }

        Graphics g = c.getGraphics();
        // handle relative
        if ( inline.relative ) {
            c.translate( inline.left, inline.top );
        }

        c.updateSelection( inline );
        // calculate the x and y relative to the baseline of the line (ly) and the
        // left edge of the line (lx)
        String text = inline.getSubstring();
        int iy = ly + inline.y;
        int ix = lx + inline.x;
        // draw a selection rectangle
        paintSelection( c, inline, lx, ly );
        //adjust font for current settings
        Font oldfont = c.getGraphics().getFont();
        c.getGraphics().setFont( inline.getFont() );
        Color oldcolor = c.getGraphics().getColor();
        c.getGraphics().setColor( inline.color );

        //draw the line
        if ( text != null && text.length() > 0 ) {
            c.getGraphics().drawString( text, ix, iy );
        }
        c.getGraphics().setColor( oldcolor );
        //draw any text decoration
        Font cur_font = c.getGraphics().getFont();
        LineMetrics lm = cur_font.getLineMetrics( text, ( (Graphics2D)c.getGraphics() ).getFontRenderContext() );
        if ( inline.underline ) {
            float down = lm.getUnderlineOffset();
            float thick = lm.getUnderlineThickness();
            g.fillRect( ix, iy + (int)down, g.getFontMetrics().stringWidth( text ), (int)thick );
        }

        if ( inline.strikethrough ) {
            float down = lm.getStrikethroughOffset();
            float thick = lm.getStrikethroughThickness();
            g.fillRect( ix, iy + (int)down, g.getFontMetrics().stringWidth( text ), (int)thick );
        }

        if ( inline.overline ) {
            float down = lm.getAscent();
            float thick = lm.getUnderlineThickness();
            g.fillRect( ix, iy - (int)down, g.getFontMetrics().stringWidth( text ), (int)thick );
        }

        if ( c.debugDrawInlineBoxes() ) {
            GraphicsUtil.draw( c.getGraphics(), new Rectangle( lx + inline.x + 1, ly + inline.y + 1 - inline.height,
                    inline.width - 2, inline.height - 2 ), Color.green );
        }

        // restore the old font
        c.getGraphics().setFont( oldfont );

        // handle relative
        if ( inline.relative ) {
            c.translate( -inline.left, -inline.top );
        }
    }

}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.5  2004/11/01 14:24:19  joshy
 * added a boolean for turning off threading
 * fixed the diff tests
 * removed some dead code
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/10/27 13:17:01  joshy
 * beginning to split out rendering code
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:50:26  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 *
 */

