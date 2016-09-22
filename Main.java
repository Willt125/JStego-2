/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jstego;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 * @author signallock
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, Exception {
//        File infile = new File("t.jpg");
//        File cover = new File("cover.jpg");
//        File outfile = new File("stego.jpg");
//        File secret = new File("secret.txt");
//        Jstego js = new Jstego(outfile);
//        js.generateCover(infile, cover);
//        js.huffmanDecode();
//        System.out.println(js.jstegMaxFileSize());
//        js.jstegHide(secret);
//        js.jstegSeek();
//        js.f5Hide(secret);
//        js.f5Seek();
//        js.huffmanEncode(outfile);

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}
