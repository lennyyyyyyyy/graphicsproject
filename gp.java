import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

class MAIN
{	
	public static void main(String[] args) {
		Frame frame = new Frame();
        frame.show();
        frame.startGame();
	}
}

class Frame extends JFrame
{
    public static Frame self;
	public static int WIDTH = 950, HEIGHT = 950;
    Game g;
	public Frame() {
        self = this;
		setSize(WIDTH,HEIGHT);
        setResizable(false);
			
		//MENU
		MenuBar menuBar = new MenuBar();
		Menu menuFile = new Menu("File");
		
		MenuItem ExitItem = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_E));		
		ExitItem.addActionListener(new ExitListener());
		
		menuFile.add(ExitItem);
		
		menuBar.add(menuFile);
		this.setMenuBar(menuBar);
		
        // PANELS
        g = new Game();
        add(g);
	}
    public void startGame() {
        g.startGame();
    }
	private class ExitListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			//System.out.println("Exit");
			System.exit(0);
		}
	}
}

class Game extends JPanel {
    // static vars
    static int STARTBUTTONWIDTH = 100, STARTBUTTONHEIGHT = 100;
    static int SPACEWIDTH = 80, SPACEHEIGHT = 120, SPACEPADDING = 7, SPACEMARGIN = 6;
    static int CARDWIDTH = 200, CARDHEIGHT = 340, CARDSPACING = 20;
    static int PASSTURNWIDTH = 250;

    Space[][] board = new Space[4][4];
    ArrayList<Card> playerHand = new ArrayList<Card>(), enemyHand = new ArrayList<Card>();
    Card playerHolding, enemyHolding;
    // possible states
    int STARTSCREEN = 0, PLAYERWINSCREEN = 6, ENEMYWINSCREEN = 7;
    int PLAYERROLL = 8, PLAYERPICK = 1, PLAYERPLACE = 2, PLAYERVIEW = 10, ENEMYROLL = 9, ENEMYPICK = 3, ENEMYPLACE = 4, ENEMYVIEW = 11, UPDATEAIR = 5;

    int mouseX, mouseY;

    int gameState = STARTSCREEN;

    public void paintComponent(Graphics g) {
        for (MouseListener ml : getMouseListeners()) {
            removeMouseListener(ml);
        }
        super.paintComponent(g);
        if (gameState == STARTSCREEN) {
            Image background = new Image("fillerred.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            Image startButton = new StartButton();
            startButton.draw(g);
        } else if (gameState == PLAYERROLL) {
            Image background = new Image("playerpick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            fillHand(playerHand, "player");
            gameState = PLAYERPICK;
        } else if (gameState == PLAYERPICK || gameState == PLAYERVIEW) {
            Image background = new Image("playerpick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            if (gameState == PLAYERPICK) {
                displayHand(g, playerHand, true);
            } else {
                displayHand(g, playerHand, false);
            }
            if (gameState == PLAYERVIEW) {
                PassTurn pt = new PassTurn();
            }
        } else if (gameState == PLAYERPLACE) {
            Image background = new Image("playerboard.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            displayBoard(g, "player");
            if (playerHolding != null) {
                playerHolding.lower(0);
                playerHolding.draw(g);
            }
            ExitPlace e = new ExitPlace();
        } else if (gameState == ENEMYROLL) {
            Image background = new Image("enemypick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            fillHand(enemyHand, "enemy");
            gameState = ENEMYPICK;
            repaint();
        } else if (gameState == ENEMYPICK || gameState == ENEMYVIEW) {
            Image background = new Image("enemypick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            if (gameState == ENEMYPICK) {
                displayHand(g, enemyHand, true);
            } else {
                displayHand(g, enemyHand, false);
            }
            if (gameState == ENEMYVIEW) {
                PassTurn pt = new PassTurn();
            }
        } else if (gameState == ENEMYPLACE) {
            Image background = new Image("enemyboard.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            displayBoard(g, "enemy");
            if (enemyHolding != null) {
                enemyHolding.lower(0);
                enemyHolding.draw(g);
            }
            ExitPlace e = new ExitPlace();
        } else if (gameState == UPDATEAIR) {
            Image background = new Image("fillerred.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
        }
    }
    public void startGame() {
        for (int i=0; i<4; i++) { // fill board
            for (int j=0; j<4; j++) {
                board[i][j] = new Space(i, j);
            }
        }

        while (true) {
            // update mouse locations
            mouseX = (int) (MouseInfo.getPointerInfo().getLocation().getX() - Frame.self.getLocation().getX());
            mouseY = (int) (MouseInfo.getPointerInfo().getLocation().getY() - Frame.self.getLocation().getY() + Frame.self.getContentPane().getSize().getHeight() - Frame.HEIGHT);
            repaint();
            // System.out.println("x: " + mouseX + ", y: " + mouseY);
            // System.out.println(gameState);
            // wait for 17 ms so ~ 60fps
            try{Thread.sleep(17);}
			    catch(Exception e){}
        }
    }
    public void fillHand(ArrayList<Card> hand, String owner) { // chooses 3 random cards
        hand.clear();
        String[] options = new String[]{"nue", "garmr", "cancer", "ogama", "suffocation", "baneberry"};
        for (int i=0; i<3; i++) {
            hand.add(new Card(options[(int) (Math.random()*options.length)], owner));
        }
    }
    public void displayHand(Graphics g, ArrayList<Card> hand, boolean pick) {
        for (int i=0; i<hand.size(); i++) {
            if (hand.get(i).inside() && pick) { // hovering
                hand.get(i).raise(i);
            } else { // not hovering
                hand.get(i).lower(i);
            }
            hand.get(i).draw(g);
            if (pick) {
                hand.get(i).setListener();
            }
        }
    }
    public void displayBoard(Graphics g, String turn) {
        for (Space[] r : board) {
            for (Space s : r) {
                s.draw(g, turn);
                s.setListener();
            }
        }
    }

    private class Box {
        int x, y, width, height;
        public Box(int X, int Y, int WIDTH, int HEIGHT) {
            x = X;
            y = Y;
            width = WIDTH;
            height = HEIGHT;
        }
        public boolean inside() {
            return mouseX >= x && mouseX <= x+width && mouseY >= y && mouseY <= y+height;
        }
        public void update(int endx, int endy, double power) {
            x += (endx-x) * power;
            y += (endy-y) * power;
        }
        
    }

    private class Image extends Box{
        ImageIcon img;
        public Image(String src, int WIDTH, int HEIGHT) {
            super(0, 0, WIDTH, HEIGHT);
            img = new ImageIcon(src);
        }
        public void draw(Graphics g) {
            g.drawImage(img.getImage(), x, y, width, height, Frame.self);
        }
    }

    private class StartButton extends Image {
        public StartButton() {
            super("fillerblue.png", STARTBUTTONWIDTH, STARTBUTTONHEIGHT);
            x=Frame.WIDTH/2 - STARTBUTTONWIDTH/2;
            y=Frame.HEIGHT/2 - STARTBUTTONHEIGHT/2;
            addMouseListener(new MouseAdapter() { // listens to press the button
                public void mousePressed(MouseEvent e) {
                    System.out.println(inside());
                    if (inside()) {
                        gameState = PLAYERROLL; 
                    }
                } 
            });
        }
    }

    private class ExitPlace extends Box {
        public ExitPlace() {
            super(0, 0, Frame.WIDTH, Frame.HEIGHT/4);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (inside()) {
                        if (gameState == PLAYERPLACE) {
                            if (playerHolding == null) {
                                gameState = PLAYERVIEW;
                            } else {
                                gameState = PLAYERPICK;
                            }
                        } else if (gameState == ENEMYPLACE) {
                            if (enemyHolding == null) {
                                gameState = ENEMYVIEW;
                            } else {
                                gameState = ENEMYPICK;
                            }
                        }
                    }
                }
            });
        }
    }

    private class PassTurn extends Box {
        public PassTurn() {
            super(Frame.WIDTH/2 - PASSTURNWIDTH/2, Frame.HEIGHT/2 - PASSTURNWIDTH/2, PASSTURNWIDTH, PASSTURNWIDTH);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    System.out.println(inside());
                    if (inside()) {
                        if (gameState == PLAYERVIEW) {
                            if (enemyHand.size() == 0) {
                                gameState = ENEMYROLL;
                            } else {
                                gameState = ENEMYPICK;
                            }
                        } else if (gameState == ENEMYVIEW) {
                            if (playerHand.size() == 0) {
                                gameState = PLAYERROLL;
                            } else {
                                gameState = PLAYERPICK;
                            }
                        }
                    }
                }
            });
        }
    }

    private class Card extends Image {
        String type, owner;
        boolean hovered = false;
        public Card(String TYPE, String OWNER) {
            // super(TYPE + "_" + OWNER+ ".png", CARDWIDTH, CARDHEIGHT);
            super("fillerred.png", CARDWIDTH, CARDHEIGHT);
            type = TYPE;
            owner = OWNER;
        }
        public boolean inside() {
            return mouseX >= x && mouseX <= x+width && mouseY >= y && mouseY <= y+2*height; // card collider is two times taller
        }
        public void raise(int slot) {
            update((slot+1)*CARDSPACING + slot*CARDWIDTH, Frame.HEIGHT - 3*CARDHEIGHT/2, 0.5);
        }
        public void lower(int slot) {
            update((slot+1)*CARDSPACING + slot*CARDWIDTH, Frame.HEIGHT - CARDHEIGHT, 0.5);
        }
        public void setListener() {
            addMouseListener(new ChooseCardEvent(this));
        }
        private class ChooseCardEvent extends MouseAdapter {
            Card c;
            public ChooseCardEvent(Card card) {
                super();
                c = card;
            }
            public void mousePressed(MouseEvent e) {
                if (inside()) {
                    if (gameState == PLAYERPICK) {
                        gameState = PLAYERPLACE;
                        playerHolding = c;
                    } else if (gameState == ENEMYPICK) {
                        gameState = ENEMYPLACE;
                        enemyHolding = c;
                    }
                }
            }
        }
    }

    private class Air extends Image {
        public Air(Frame frame) {
            super("air.png", 15, 15);
        }
    }

    private class Space extends Image {
        ArrayList<Air> airs = new ArrayList<Air>();
        int row, col;
        Card c;
        public Space(int ROW, int COL) {
            super("fillerblue.png", SPACEWIDTH, SPACEHEIGHT);
            row = ROW;
            col = COL;
        }
        public void assignCard(Card card) {
            card.width = SPACEWIDTH - 2*SPACEPADDING;
            card.height = SPACEHEIGHT - 2*SPACEPADDING;
            c = card;
        }
        public void draw(Graphics g, String turn) {
            x = (int) (0.5*Frame.WIDTH - 1.5*SPACEMARGIN - 2*SPACEWIDTH + col * (SPACEMARGIN + SPACEWIDTH));
            y = (int) (0.5*Frame.HEIGHT + 1.5*SPACEMARGIN + SPACEHEIGHT - row * (SPACEMARGIN + SPACEHEIGHT));
            super.draw(g); // draws the outline of the space
            if (c != null) { // if there is a card in the space
                c.x = x + SPACEPADDING;
                c.y = y + SPACEPADDING;
                if (turn.equals("enemy")) { // rotate card 180 around center
                    ((Graphics2D) g).rotate(Math.PI, x + SPACEWIDTH/2, y + SPACEHEIGHT/2);
                } 
                c.draw(g); // draws card
                if (turn.equals("enemy")) { // rotate back
                    ((Graphics2D) g).rotate(Math.PI, x + SPACEWIDTH/2, y + SPACEHEIGHT/2);
                } 
            }
        }
        public void setListener() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (inside()) {
                        if (gameState == PLAYERPLACE && playerHolding != null) {
                            playerHand.remove(playerHand.indexOf(playerHolding));
                            assignCard(playerHolding);
                            playerHolding = null;
                        } else if (gameState == ENEMYPLACE && enemyHolding != null) {
                            enemyHand.remove(enemyHand.indexOf(enemyHolding));
                            assignCard(enemyHolding);
                            enemyHolding = null;
                        }
                    }
                }
            });
        }
    }
}