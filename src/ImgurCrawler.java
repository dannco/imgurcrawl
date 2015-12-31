package dako;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImgurCrawler extends JFrame implements KeyListener{

    JTextField inp;
    JTextArea disp;

    boolean locked;

    public ImgurCrawler() {
        try {
        UIManager.setLookAndFeel(
            "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
        );
        } catch (Exception e) {e.printStackTrace();}
        locked = false;
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
        JPanel input = new JPanel();
        input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
        input.add(new JLabel("Enter URL for imgur album, gallery or request page: "));

        
        input.setBorder(BorderFactory.createCompoundBorder(
            null,BorderFactory.createEmptyBorder(10,10,10,10))
        );
        inp = new JTextField();
        inp.addKeyListener(this);
        input.add(inp);
        JButton go = new JButton("Crawl");
        go.addActionListener(new GetListener());
        input.add(go);
        pan.add(input);
        disp = new JTextArea();
        disp.setEditable(false);
        JScrollPane jsp = new JScrollPane(disp);
        jsp.setPreferredSize(new Dimension(1000,550));
        pan.add(jsp);

        setTitle("DaKo imgur crawler");
        add(pan);
        setSize(1000,600);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new ImgurCrawler();
    }

    class GetListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (!locked) {
                locked = true;
                new ScrapeThread();
            }
        }
    }
    public void keyPressed(KeyEvent e) {
        if (!locked && inp.hasFocus() && e.getKeyCode() == KeyEvent.VK_ENTER) {
            locked = true;
            new ScrapeThread();
        }
    }

    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    private class ScrapeThread extends Thread {
        File dir;
        public ScrapeThread() {
            start();
        }

        public List<String> parseForImages(final String text) {
            ImgurCrawler.this.disp.append("Source length: " + text.length() + "\n");
            List<String> hashMatch = new ArrayList<String>();

            Matcher m1 = Pattern.compile("hash\":\"(\\w+)\"").matcher(text);
            Matcher m2 = Pattern.compile("ext\":\"(\\.\\w+)\"").matcher(text);

            List<String> found = new ArrayList<String>();

            while (m1.find() && m2.find()) {
                String s = m1.group(1)+m2.group(1);
                found.add(s);
            }
            return found;
        }

        public void tryURL(String urlString) {
            ImgurCrawler.this.disp.append("gallery or album\n");
            try {
                StringBuffer buffer = new StringBuffer();
                String line;
                URL url = new URL(urlString);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                while((line = br.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                String text = buffer.toString();

                List<String> images = parseForImages(text);
                
                ImgurCrawler.this.disp.append("Download initiated. Saving to "+dir.getAbsolutePath()+":\n");

                for ( String s : images) {
                    ImgurCrawler.this.disp.append("http://i.imgur.com/" + s+ " ...");
                    saveImgur(s);
                }
                ImgurCrawler.this.disp.append("Files saved to folder "+dir.getAbsolutePath());
            } catch (Exception e) {
                ImgurCrawler.this.disp.append(e.toString());
                e.printStackTrace();
            }
        }
        
        public void getHashes(String urlString) {
            ImgurCrawler.this.disp.append("request\n");
            System.out.println("request");
            try {
                
                String line;
                int i = 0;
                List<String> images = new ArrayList<String>();
                while (true) {
                    StringBuffer buffer = new StringBuffer();
                    URL url = new URL(urlString+"/page/"+i);
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                    while((line = br.readLine()) != null) {
                        buffer.append(line+"\n");
                    }
                    
                    Matcher m = Pattern.compile("class=\"posts\"").matcher(buffer.toString());
                    
                    if (!m.find() || i>20) {
                        ImgurCrawler.this.disp.append("no more images or max page count reached (20 pages)");
                        break;
                    }
                    ImgurCrawler.this.disp.append("requesting "+urlString+"\\page\\"+i+"\n");
                    String text = buffer.toString();
                    
                    Matcher m1 = Pattern.compile("id=\"(\\w+)\" class=\"post\"").matcher(text);
                    Matcher m2 = Pattern.compile("img alt=\"\" src=\".+(\\.\\w+)\"").matcher(text);
                    while(m1.find() && m2.find()) {
                        images.add((m1.group(1)+m2.group(1)));
                    }
                    i++;
                }

                ImgurCrawler.this.disp.append("possible images found: "+images.size()+"\n");
                ImgurCrawler.this.disp.append("Download starting. Saving to "+dir.getAbsolutePath()+":\n");
                for (String s : images) {
                    ImgurCrawler.this.disp.append("http://i.imgur.com/" + s+ " ...");
                    saveImgur(s);
                }   
            } catch (Exception e) {
                ImgurCrawler.this.disp.append(e.toString());
                e.printStackTrace();
            }
        }


        public void saveImgur(String s) {
            URL url;
            ByteArrayOutputStream out;
            try {
                url = new URL("http://i.imgur.com/"+s);
                InputStream in = new BufferedInputStream(url.openStream());
                out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n = 0;
                while (-1 != (n = in.read(buf))) { out.write(buf,0,n);}
                out.close();
                in.close();
                if (out != null) {
                    byte[] resp = out.toByteArray();
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream("scrapedImages/"+s);
                        ImgurCrawler.this.disp.append("  saved.\n");
                    } catch (FileNotFoundException e) {
                        ImgurCrawler.this.disp.append("file. "+e.toString()+"\n");
                    }
                    fos.write(resp);
                    fos.close();
                }
            } catch (MalformedURLException e) {
                ImgurCrawler.this.disp.append("url: " + e.toString()+"\n");
            } catch (IOException e) {
                ImgurCrawler.this.disp.append("io: " + e.toString()+"\n");
            }
        }

        public void run() {
            dir = new File("scrapedImages");
            if (!dir.exists()) dir.mkdir();
            String urlString = ImgurCrawler.this.inp.getText();

            Matcher m = Pattern.compile("imgur.com/(\\w+)/.*").matcher(urlString);
            if (m.find()) {
                String str = m.group(1);

                if ("a".equalsIgnoreCase(str) || "gallery".equalsIgnoreCase(str)) {
                    tryURL(urlString);
                } else if ("r".equalsIgnoreCase(str)) {
                    getHashes(urlString);
                }
            } else {
                ImgurCrawler.this.disp.append("possibly invalid imgur address, verify.");
            }    
            ImgurCrawler.this.locked=false;
        }
    }
}
