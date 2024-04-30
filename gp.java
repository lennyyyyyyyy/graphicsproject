import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

class MAIN
{	
	public static void main(String[] args)
	{
		Frame frame = new Frame();
	}
}

class Frame extends JFrame
{
	public static int WIDTH = 1000, HEIGHT = 1000;
	public Frame()
	{
		setSize(WIDTH,HEIGHT);
			
		//MENU
		MenuBar menuBar = new MenuBar();
		Menu menuFile = new Menu("File");
		
		MenuItem ExitItem = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_E));		
		ExitItem.addActionListener(new ExitListener());
		
		menuFile.add(ExitItem);
		
		menuBar.add(menuFile);
		this.setMenuBar(menuBar);
		
        // PANELS
        Container c = getContentPane();
        Background b = new Background("arrow.png");
        c.add(b);
        
		repaint();
		
	}

	private class ExitListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			//System.out.println("Exit");
			System.exit(0);
		}
	}
}

class Background extends JPanel {
    ImageIcon img;
    public Background(String src) {
        img = new ImageIcon(src);
    }
    protected void paintComponent(Graphics g) {
        g.drawImage(img.getImage(), 0, 0, WIDTH, HEIGHT, this);
    }
}