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
	public static int WIDTH = 900, HEIGHT = 900;
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
    static int STARTBUTTONWIDTH = 152, STARTBUTTONHEIGHT = 60;
    static int SPACEWIDTH = 110, SPACEHEIGHT = 165, SPACEPADDING = 3;
    static int CARDWIDTH = 200, CARDHEIGHT = 340, CARDSPACING = 20;
    static int AIRWIDTH = 20, AIRSRADIUS = 26, PLAYERAIRSRADIUS = 50;
    static double RADIUSFLUX = 0.25;
    static int PASSTURNWIDTH = 250;

    String[] cards = new String[]{"cat", "dog", "crab", "toad", "suffocation", "plant", "clam", "worm", "rat", "mouse"};
    HashMap<String, int[][]> moves = new HashMap<String, int[][]>();

    Space[][] board = new Space[4][4];
    ArrayList<Card> playerHand = new ArrayList<Card>(), enemyHand = new ArrayList<Card>();
    Card playerHolding, enemyHolding;
    ArrayList<Air> playerAirs = new ArrayList<Air>(), enemyAirs = new ArrayList<Air>();
    // possible states
    int STARTSCREEN = 0, PLAYERWINSCREEN = 6, ENEMYWINSCREEN = 7;
    int PLAYERROLL = 8, PLAYERPICK = 1, PLAYERPLACE = 2, PLAYERVIEW = 10, ENEMYROLL = 9, ENEMYPICK = 3, ENEMYPLACE = 4, ENEMYVIEW = 11;
    int SWITCH = 12, UPDATEAIR = 5;

    double angle = 0;
    int mouseX, mouseY;

    int gameState = STARTSCREEN;

    public void paintComponent(Graphics g) {
        for (MouseListener ml : getMouseListeners()) {
            removeMouseListener(ml);
        }
        super.paintComponent(g);
        if (gameState == STARTSCREEN) {
            Image background = new Image("start.png", Frame.WIDTH, Frame.HEIGHT);
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
            displayBoard(g, 0.6);
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
            ((Graphics2D)g).rotate(Math.PI, Frame.WIDTH/2, Frame.HEIGHT/2); // board has to be flipped
            displayBoard(g, 0.6);
            ((Graphics2D)g).rotate(Math.PI, Frame.WIDTH/2, Frame.HEIGHT/2);
            if (enemyHolding != null) {
                enemyHolding.lower(0);
                enemyHolding.draw(g);
            }
            ExitPlace e = new ExitPlace();
        } else if (gameState == UPDATEAIR) {
            ((Graphics2D)g).rotate(-1*Math.PI/2, Frame.WIDTH/2, Frame.HEIGHT/2); // tilt board sideways
            Image background1 = new Image("enemyboard.png", Frame.WIDTH, Frame.HEIGHT);
            background1.draw(g);
            Image background2 = new Image("playerboard.png",0, Frame.HEIGHT/2, Frame.WIDTH, Frame.HEIGHT);
            background2.draw(g);
            displayBoard(g, 0.005);
            ((Graphics2D)g).rotate(Math.PI/2, Frame.WIDTH/2, Frame.HEIGHT/2);
        } else if (gameState == SWITCH) {
            Image background = new Image("black.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
        }
    }
    public void startGame() {
        moves.put("cat", new int[][] {{0,0,-1,0}, {1,0,0,0}});
        moves.put("dog", new int[][] {{0,0,1,0}, {-1,0,0,0}});
        moves.put("worm", new int[][] {{0,2,0,0}});
        moves.put("rat", new int[][] {{1,1,0,0}});
        moves.put("mouse", new int[][] {{-1,1,0,0}});
        for (int i=0; i<4; i++) { // fill board
            for (int j=0; j<4; j++) {
                board[i][j] = new Space(i, j);
            }
        }
        for (int i=0; i<10; i++) { // each player starts with ten airs
            playerAirs.add(new Air());
            enemyAirs.add(new Air());
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
        for (int i=0; i<3; i++) {
            hand.add(new Card(cards[(int) (Math.random()*cards.length)], owner));
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
    public void displayBoard(Graphics g, double power) {
        Image boardArea = new Image("board.png", Frame.WIDTH/2 - 2*SPACEWIDTH, Frame.HEIGHT/2 - 2*SPACEHEIGHT, 4*SPACEWIDTH, 4*SPACEHEIGHT);
        boardArea.draw(g);
        for (Space[] r : board) {
            for (Space s : r) {
                s.draw(g, power);
                s.setListener();
            }
        }
        angle += 3.14/60;
        int dist = (int)((1+RADIUSFLUX)*PLAYERAIRSRADIUS + AIRWIDTH);
        displayAirs(g, playerAirs, PLAYERAIRSRADIUS, angle, Frame.WIDTH - dist, Frame.HEIGHT - dist, power);
        displayAirs(g, enemyAirs, PLAYERAIRSRADIUS, angle, dist, dist, power);
    }
    public void displayAirs(Graphics g, ArrayList<Air> airs, double radius, double theta, int centerx, int centery, double power) {
        for (int i=0; i<airs.size(); i++) {
            double airAngle = theta + (2*Math.PI*i/airs.size());
            // sinewave radius
            airs.get(i).update((int) (centerx - AIRWIDTH/2 + radius + RADIUSFLUX*radius*Math.sin(4.5*airAngle)), centery - AIRWIDTH/2, power);
            ((Graphics2D) g).rotate(airAngle, centerx, centery);
            airs.get(i).draw(g);
            ((Graphics2D) g).rotate(-1*airAngle, centerx, centery);
        }
    }
    public void updateAirs(int time) {
        System.out.println("updating");
        ArrayList<ArrayList<ArrayList<Air>>> validMoves = new ArrayList<ArrayList<ArrayList<Air>>>();
        for (Space[] row : board) {
            for (Space s : row) {
                if (s.c != null && moves.containsKey(s.c.type)) {
                    for (int[] m : moves.get(s.c.type)) { // add valid moves
                        int[] move = m.clone();
                        if (s.c.owner == "enemy") {
                            move[2] *= -1;
                            move[3] *= -1;
                        }
                        move[0] += s.col;
                        move[1] += s.row;
                        move[2] += s.col;
                        move[3] += s.row;
                        if (move[0] >= 0 && move[0] < 4 && move[2] >= 0 && move[2] < 4) {
                            ArrayList<ArrayList<Air>> validMove = new ArrayList<ArrayList<Air>>();
                            if (move[1] < 0) {
                                validMove.add(playerAirs);
                            } else if (move[1] >= 4) {
                                validMove.add(enemyAirs);
                            } else {
                                validMove.add(board[move[1]][move[0]].airs);
                            }
                            if (move[3] < 0) {
                                validMove.add(playerAirs);
                            } else if (move[1] >= 4) {
                                validMove.add(enemyAirs);
                            } else {
                                validMove.add(board[move[3]][move[2]].airs);
                            }
                            validMoves.add(validMove);
                        }
                    }
                }
            }
        }
        // shuffle moves
        for (int i=validMoves.size()-1; i>=0; i--) {
            int swap = (int) (Math.random() * (i+1));
            ArrayList<ArrayList<Air>> temp = validMoves.get(i);
            validMoves.set(i, validMoves.get(swap));
            validMoves.set(swap, temp);
        }
        for (ArrayList<ArrayList<Air>> move : validMoves) {
            if (move.get(0).size() != 0) {
                Air air = move.get(0).remove(move.get(0).size()-1);
                move.get(1).add(0, air);
            }
        }
        javax.swing.Timer timer = new javax.swing.Timer(time * 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameState = SWITCH;
                javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (playerHand.size() == 0) {
                            gameState = PLAYERROLL;
                        } else {
                            gameState = PLAYERPICK;
                        }
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
        timer.setRepeats(false);
        timer.start();
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
            x += Math.signum(endx-x) * Math.max(Math.abs(endx-x)*power, 1);
            y += Math.signum(endy-y) * Math.max(Math.abs(endy-y)*power, 1);
        }
        
    }

    private class Image extends Box{
        ImageIcon img;
        public Image(String src, int WIDTH, int HEIGHT) {
            super(0, 0, WIDTH, HEIGHT);
            img = new ImageIcon(src);
        }
        public Image(String src, int X, int Y, int WIDTH, int HEIGHT) {
            super(X, Y, WIDTH, HEIGHT);
            img = new ImageIcon(src);
        }
        public void draw(Graphics g) {
            g.drawImage(img.getImage(), x, y, width, height, Frame.self);
        }
    }

    private class StartButton extends Image {
        public StartButton() {
            super("startbutton.png",Frame.WIDTH/2 - STARTBUTTONWIDTH/2, Frame.HEIGHT*3/5 - STARTBUTTONHEIGHT/2, STARTBUTTONWIDTH, STARTBUTTONHEIGHT);
            addMouseListener(new MouseAdapter() { // listens to press the button
                public void mousePressed(MouseEvent e) {
                    if (inside()) {
                        gameState = PLAYERROLL; 
                    }
                } 
            });
        }
        public void draw(Graphics g) {
            if (inside()) {
                width = (int) (STARTBUTTONWIDTH * 1.1);
                height = (int) (STARTBUTTONHEIGHT * 1.1);
            } else {
                width = STARTBUTTONWIDTH;
                height = STARTBUTTONHEIGHT;
            }
            x=Frame.WIDTH/2 - width/2;
            y=Frame.HEIGHT*3/5 - height/2;
            super.draw(g);
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
                    if (inside()) {
                        if (gameState == PLAYERVIEW) {
                            gameState = SWITCH;
                            javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    if (enemyHand.size() == 0) {
                                        gameState = ENEMYROLL;
                                    } else {
                                        gameState = ENEMYPICK;
                                    }
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();
                        } else if (gameState == ENEMYVIEW) {
                            gameState = UPDATEAIR;
                            updateAirs(5);
                        }
                    }
                }
            });
        }
    }

    private class Card extends Image {
        String type, owner;
        public Card(String TYPE, String OWNER) {
            super(TYPE + "_" + "player"+ ".png", CARDWIDTH, CARDHEIGHT);
            // super("fillerred.png", CARDWIDTH, CARDHEIGHT);
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
        double theta = 0;
        double speed = (-18 + Math.random() * 6)/60;
        public Air() {
            super("air.png", AIRWIDTH, AIRWIDTH);
        }
        public void draw(Graphics g) {
            theta += speed;
            theta %= 2*Math.PI; // rotate self
            ((Graphics2D) g).rotate(theta, x + AIRWIDTH/2, y + AIRWIDTH/2);
            super.draw(g);
            ((Graphics2D) g).rotate(-1*theta, x + AIRWIDTH/2, y + AIRWIDTH/2);
        }
    }

    private class Space extends Image {
        ArrayList<Air> airs = new ArrayList<Air>();
        int row, col;
        double theta = 0;
        double speed = (3.14 + Math.random() * 0.5)/60;
        Card c;
        public Space(int ROW, int COL) {
            super("", (int) (0.5*Frame.WIDTH - 2*SPACEWIDTH + COL * SPACEWIDTH),(int) (0.5*Frame.HEIGHT + SPACEHEIGHT - ROW * SPACEHEIGHT), SPACEWIDTH, SPACEHEIGHT);
            row = ROW;
            col = COL;
            speed = ((row+col)%2==0)?speed:-1*speed; // every other spins the opposite direction
            int num = 2 + (int) (Math.random()*5); // random air count from 2-6
            for (int k=0; k<num; k++) {
                Air a = new Air();
                a.x =  AIRSRADIUS;
                a.y = y + SPACEHEIGHT/2;
                airs.add(a);
            }
        }
        public boolean inside() {
            boolean ans;
            if (gameState == ENEMYPLACE) { // enemy place the board is flipped, so mouse position is interpreted flipped
                mouseX = Frame.WIDTH - mouseX;
                mouseY = Frame.HEIGHT - mouseY;
            }
            ans = super.inside();
            if (gameState == ENEMYPLACE) {
                mouseX = Frame.WIDTH - mouseX;
                mouseY = Frame.HEIGHT - mouseY;
            }
            return ans;
        }
        public void assignCard(Card card) {
            card.width = SPACEWIDTH - 2*SPACEPADDING;
            card.height = SPACEHEIGHT - 2*SPACEPADDING;
            c = card;
        }
        public void draw(Graphics g, double power) {
            if (c != null) { // if there is a card in the space
                c.x = x + SPACEPADDING;
                c.y = y + SPACEPADDING;
                if (c.owner.equals("enemy")) { // rotate card 180 around center
                    ((Graphics2D) g).rotate(Math.PI, x + width/2, y + height/2);
                } 
                c.draw(g); // draws card
                if (c.owner.equals("enemy")) { // rotate back
                    ((Graphics2D) g).rotate(Math.PI, x + width/2, y + height/2);
                } 
            }

            //display airs
            theta += speed; //rotate airs
            displayAirs(g, airs, AIRSRADIUS, theta, x + width/2, y+height/2, power);
            
        }
        public void setListener() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (inside()) {
                        if (gameState == PLAYERPLACE && playerHolding != null && row < 2) {
                            playerHand.remove(playerHand.indexOf(playerHolding));
                            assignCard(playerHolding);
                            playerHolding = null;
                        } else if (gameState == ENEMYPLACE && enemyHolding != null && row > 1) {
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