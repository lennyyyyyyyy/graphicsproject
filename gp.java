import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.*;

class MAIN
{	
	public static void main(String[] args) {
		Frame frame = new Frame();
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
    static int CARDWIDTH = 200, CARDHEIGHT = 380, CARDSPACING = 10;
    static int AIRWIDTH = 20, AIRSRADIUS = 26, PLAYERAIRSRADIUS = 50;
    static double RADIUSFLUX = 0.25;
    static int PASSTURNWIDTH = 250, UPDATEAIRTIME = 5, MOVETOADSTIME = 3, GENERATEAIRTIME = 3, APPLYACTIONCARDSTIME = 2;

    Card cards[] = new Card[] {new Card("cat", "domain"),
                            // new Card("dog", "domain"),
                            // new Card("crab", "domain"),
                            // new Card("toad", "domain"),
                            // new Card("plant", "domain"),
                            // new Card("clam", "domain"),
                            // new Card("worm", "domain"),
                            // new Card("rat", "domain"),
                            // new Card("mouse", "domain"),
                            new Card("mole", "domain"),
                            // new Card("snake", "domain"),
                            // new Card("blank", "domain"),
                            // new Card("suffocation", "any"),
                            // new Card("max", "any"),
                            // new Card("evan", "any"),
                            // new Card("sendback", "card"),
                            // new Card("block", "card")
                            };

    // String[] cards = new String[]{"cat", "dog", "crab", "toad", "suffocation", "plant", "clam", "worm", "rat", "mouse", "mole", "snake"};
    static HashMap<String, int[][]> moves = new HashMap<String, int[][]>();
    static {
        moves.put("cat", new int[][] {{0,0,-1,0}, {1,0,0,0}});
        moves.put("dog", new int[][] {{0,0,1,0}, {-1,0,0,0}});
        moves.put("worm", new int[][] {{0,2,0,0}});
        moves.put("rat", new int[][] {{1,1,0,0}});
        moves.put("mouse", new int[][] {{-1,1,0,0}});
        moves.put("snake", new int[][] {{0,3,0,0}});
    }
    static HashMap<String, JLabel> labels = new HashMap<String, JLabel>();
    static {
        labels.put("cat", new JLabel("Moves air around it to the left."));
        labels.put("dog", new JLabel("Moves air around it to the right."));
        labels.put("crab", new JLabel("Protects its own air from all other cards."));
        labels.put("toad", new JLabel("Hops forward every turn, stomping on other cards."));
        labels.put("suffocation", new JLabel("Removes all air from any square on the board."));
        labels.put("plant", new JLabel("Generates one air every turn."));
        labels.put("clam", new JLabel("Blocks all enemy currents that pass it."));
        labels.put("worm", new JLabel("Pulls air two spaces ahead of itself."));
        labels.put("rat", new JLabel("Pulls air northeast from itself."));
        labels.put("mouse", new JLabel("Pulls air northwest from itself."));
        labels.put("mole", new JLabel("Pulls air from corresponding enemy square."));
        labels.put("snake", new JLabel("Pulls air three spaces from itself."));
        labels.put("blank", new JLabel("Pick a card, 2 to K."));
        labels.put("sendback", new JLabel("Modifies a card to send air back."));
        labels.put("block", new JLabel("Gives a 50% chance to protect its air."));
        labels.put("max", new JLabel("Switches your air and the enemy's air."));
        labels.put("evan", new JLabel("Flips all player and enemy cards around."));
    }

    Space[][] board = new Space[4][4];
    Font font; 
    JLabel choiceLabel = new JLabel();
    Clip playerAmbient = assignSound("playerAmbient.wav", 0), enemyAmbient = assignSound("enemyAmbient.wav", 0);
    Clip playerBreathing = assignSound("playerBreathing.wav", 1), enemyBreathing = assignSound("enemyBreathing.wav", -6);
    ArrayList<Card> playerHand = new ArrayList<Card>(), enemyHand = new ArrayList<Card>(), cardChoices = new ArrayList<Card>(), toBeAdded = new ArrayList<Card>();
    Card playerHolding, enemyHolding;
    ArrayList<Air> playerAirs = new ArrayList<Air>(), enemyAirs = new ArrayList<Air>();
    // possible states
    int STARTSCREEN = 0, PLAYERWINSCREEN = 6, ENEMYWINSCREEN = 7;
    int PLAYERROLL = 8, PLAYERPICK = 1, PLAYERPLACE = 2, PLAYERVIEW = 10, ENEMYROLL = 9, ENEMYPICK = 3, ENEMYPLACE = 4, ENEMYVIEW = 11;
    int SWITCH = 12, UPDATEBOARD = 13, PLAYERWIN = 14, ENEMYWIN = 15, PLAYERHIDE = 16, ENEMYHIDE = 17;

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
            displayChoices(g, playerHand, PLAYERHIDE, PLAYERPICK);
            
        } else if (gameState == PLAYERHIDE) {
            Image background = new Image("playerpick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            hideChoices(g, playerHand);
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
                playerHolding.drawHolding(g);
            }
            ExitPlace e = new ExitPlace();
        } else if (gameState == ENEMYROLL) {
            Image background = new Image("enemypick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            displayChoices(g, enemyHand, ENEMYHIDE, ENEMYPICK);
            
        } else if (gameState == ENEMYHIDE) {
            Image background = new Image("enemypick.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
            hideChoices(g, enemyHand);
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
                enemyHolding.drawHolding(g);
            }
            ExitPlace e = new ExitPlace();
        } else if (gameState == UPDATEBOARD) {
            ((Graphics2D)g).rotate(-1*Math.PI/2, Frame.WIDTH/2, Frame.HEIGHT/2); // tilt board sideways
            Image background1 = new Image("enemyboard.png", Frame.WIDTH, Frame.HEIGHT);
            background1.draw(g);
            Image background2 = new Image("playerboard.png",0, Frame.HEIGHT/2, Frame.WIDTH, Frame.HEIGHT);
            background2.draw(g);
            displayBoard(g, 0.012);
            ((Graphics2D)g).rotate(Math.PI/2, Frame.WIDTH/2, Frame.HEIGHT/2);
        } else if (gameState == SWITCH) {
            Image background = new Image("black.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
        } else if (gameState == PLAYERWIN) {
            Image background = new Image("fillerred.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
        } else if (gameState == ENEMYWIN) {
            Image background = new Image("fillerblue.png", Frame.WIDTH, Frame.HEIGHT);
            background.draw(g);
        }    
    }
    public void startGame() {
        font = new Font("font", Font.PLAIN, 25); // default font
        try{
            Font font1 = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("font.ttf")); // try custom font
            font = font1.deriveFont(Font.PLAIN, 25);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {}
        for (JLabel jl : labels.values()) { // adjust label settings for every card
            jl.setFont(font);
            jl.setForeground(new Color(65, 105, 225));
            jl.setText("<html><div style=\"text-align: center;\">" + jl.getText() + "</div></html>");
            jl.setVisible(false);
            add(jl);
        }
        choiceLabel.setFont(font);
        choiceLabel.setForeground(Color.WHITE);
        choiceLabel.setBounds(Frame.WIDTH/2, Frame.HEIGHT/2, 100, 100);
        add(choiceLabel);
        Frame.self.show();

        playerAmbient.loop(-1); // start ocean sounds

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
    public void fillChoices(String owner) { // chooses 5 random cards to pick from
        toBeAdded.clear();
        cardChoices.clear();
        for (int i=0; i<5; i++) {
            Card c = new Card(cards[(int) (Math.random()*cards.length)]);
            c.width = (int)(0.8 * CARDWIDTH);
            c.height = (int)(0.8 * CARDHEIGHT);
            c.owner = owner;
            c.flip = 1; // flipped over
            cardChoices.add(c);
        }
        playSound("carddeal.wav", 0, -1);
    }
    public void displayChoices(Graphics g, ArrayList<Card> hand, int hidestate, int pickstate) {
        for (int i=0; i<cardChoices.size(); i++) {
            int slot = i + toBeAdded.size();
            if (cardChoices.get(i).inside()) {
                cardChoices.get(i).raise(slot);
            } else {
                cardChoices.get(i).lower(slot);
            }
            cardChoices.get(i).setListener();
            cardChoices.get(i).flip(1, 0.2);
            cardChoices.get(i).draw(g);
        }
        for (int i=0; i<toBeAdded.size(); i++) {
            toBeAdded.get(i).lower(i);
            toBeAdded.get(i).flip(0, 0.2);
            toBeAdded.get(i).draw(g);
        }
        choiceLabel.setVisible(true);
        choiceLabel.setText("<html><div style=\"text-align: center;\">Choose " + (3-hand.size()) + " card"+(hand.size()==2?"":"s")+".</div></html>");
        if (hand.size() == 3) { // when hand is full move on to playing
            gameState = hidestate;
            choiceLabel.setVisible(false);
            javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gameState = pickstate;
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
    public void hideChoices(Graphics g, ArrayList<Card> hand) {
        for (int i=0; i<toBeAdded.size(); i++) {
            toBeAdded.get(i).hide(i);
            toBeAdded.get(i).flip(0, 0.2);
            toBeAdded.get(i).draw(g);
        }
        for (int i=0; i<cardChoices.size(); i++) {
            int slot = i + toBeAdded.size();
            cardChoices.get(i).hide(slot);
            cardChoices.get(i).flip(1, 0.2);
            cardChoices.get(i).draw(g);
        }
        for (int i=0; i<hand.size(); i++) {
            hand.get(i).hide(i);
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
            if (pick) { // reset card listeners if on pick state
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
    public <T> void shuffle(T[] array) {
        // shuffle moves
        for (int i=array.length-1; i>=0; i--) {
            int swap = (int) (Math.random() * (i+1));
            T temp = array[i];
            array[i] = array[swap];
            array[swap] = temp;
        }
    }
    public <T> void shuffle(ArrayList<T> array) {
        for (int i=array.size()-1; i>=0; i--) {
            int swap = (int) (Math.random() * (i+1));
            T temp = array.get(i);
            array.set(i, array.get(swap));
            array.set(swap, temp);
        }
    }
    public void updateAirs() {
        ArrayList<ArrayList<ArrayList<Air>>> validMoves = new ArrayList<ArrayList<ArrayList<Air>>>();
        for (Space[] row : board) {
            for (Space s : row) {
                if (s.c != null) {
                    ArrayList<int[]> relevantmoves;
                    if (moves.containsKey(s.c.type)) {
                        relevantmoves = new ArrayList<int[]>(Arrays.asList(moves.get(s.c.type)));
                    } else {
                        relevantmoves = new ArrayList<int[]>();
                    }

                    if (s.c.type == "mole") { // mole takes air from corresponding enemy square
                        relevantmoves.add(new int[]{0,Math.abs(3-2*s.row), 0, 0});
                    }

                    // this is where air card modifiers take place
                    for (Card modifier : s.c.modifiers) {
                        if (modifier.type == "sendback") {
                            relevantmoves.add(new int[] {0,0,0,-1});
                        }
                    }

                    for (int[] m : relevantmoves) { // add valid moves
                        int[] move = m.clone();
                        if (s.c.owner == "enemy") {
                            move[0] *= -1;
                            move[1] *= -1;
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

                            boolean cancelMove = false;

                            if (board[move[3]][move[2]].c != null) {
                                Card card = board[move[3]][move[2]].c;
                                if (card.type == "crab") {// crab blocks anything from taking air
                                    cancelMove = true;
                                }
                                for (Card modifier : s.c.modifiers) { // block modifier protects 50% of time
                                    if (modifier.type == "block" && Math.random() < 0.5) {
                                        cancelMove = true;
                                    }
                                }
                            } 

                            if (!cancelMove) validMoves.add(validMove);
                        }
                    }
                }
            }
        }
        // shuffle moves
        shuffle(validMoves);
        for (ArrayList<ArrayList<Air>> move : validMoves) {
            if (move.get(0).size() != 0) {
                Air air = move.get(0).remove(move.get(0).size()-1);
                move.get(1).add(0, air);
            }
        }
        // check for game end, else, remove one air from both enemy and player
        if (playerAirs.size() == 0) {
            animateToState(ENEMYWIN);
        } else if (enemyAirs.size() == 0) {
            animateToState(PLAYERWIN);
        } else {
            playerAirs.remove(0);
            enemyAirs.remove(0);
        }
        javax.swing.Timer timer = new javax.swing.Timer((validMoves.size()!=0?UPDATEAIRTIME:2) * 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveToads();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    public void moveToads() {
        Set<Card> movedToads = new HashSet<Card>();
        boolean hadToad = false;
        while (true) {
            ArrayList<Space> toads = new ArrayList<Space>();
            for (Space[] row : board) {
                for (Space s : row) {
                    if (s.c != null && s.c.type == "toad" && !s.c.toadStopped && !(s.c.owner == "player" && s.row == 3) && !(s.c.owner == "enemy" && s.row == 0) && !movedToads.contains(s.c)) {
                        hadToad = true;
                        toads.add(s);
                    }
                }
            }
            if (toads.size() == 0) break;
            shuffle(toads);
            Space toad = toads.get(0);
            movedToads.add(toad.c);
            int add = toad.c.owner == "player"?1:-1;
            if (board[toad.row+add][toad.col].c != null) {
                toad.c.toadStopped = true;
            }
            board[toad.row+add][toad.col].assignCard(toad.c);
            System.out.println(toad.row+add);
            System.out.println(toad.col);

            toad.c = null;
        }
        javax.swing.Timer timer = new javax.swing.Timer((hadToad?MOVETOADSTIME:0)*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateAir();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    public void generateAir() {
        boolean hadPlant = false;
        for (Space[] row : board) {
            for (Space s : row) {
                if (s.c != null && s.c.type == "plant") {
                    hadPlant = true;
                    s.airs.add(new Air());
                }
            }
        }
        javax.swing.Timer timer = new javax.swing.Timer((hadPlant?GENERATEAIRTIME:0)*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyActionCards();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    public void applyActionCards() {
        boolean hadActionCard = false;
        for (Space[] row : board) {
            for (Space s : row) {
                for (int i=s.actionCards.size()-1; i>=0; i--) {
                    hadActionCard = true;
                    String type = s.actionCards.get(i).type;
                    if (type == "suffocation") {
                        s.airs = new ArrayList<Air>(); // suffocation removes all air
                    } else if (type == "max") { // switches player and enemy air
                        ArrayList<Air> temp = playerAirs;
                        playerAirs = enemyAirs;
                        enemyAirs = temp;
                    } else if (type == "evan") { // flips all the cards of the board around
                        for (int j=0; j<4; j++) {
                            for (int k=0; k<2; k++) {
                                Card temp = board[j][k].c;
                                board[j][k].assignCard(board[3-j][3-k].c);
                                board[3-j][3-k].assignCard(temp);
                            }
                        }
                        for (int j=0; j<4; j++) {
                            for (int k=0; k<4; k++) {
                                System.out.println(j + ", " + k);
                                if (board[j][k].c != null) {
                                    board[j][k].c.owner = board[j][k].c.owner == "player" ? "enemy" : "player"; // switch owners
                                }
                            }
                        }
                    }
                    s.actionCards.remove(i);
                }
            }
        }
        javax.swing.Timer timer = new javax.swing.Timer((hadActionCard?GENERATEAIRTIME:0)*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animateToPlayer();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    public void animateToPlayer() {
        gameState = SWITCH;
        javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (playerHand.size() == 0) {
                    fillChoices("player");
                    gameState = PLAYERROLL;
                } else {
                    gameState = PLAYERPICK;
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
        playSound("crack.wav", 0, 1);
        enemyAmbient.stop();
        playerAmbient.setMicrosecondPosition(0);
        playerAmbient.loop(-1);
        enemyBreathing.stop();
        playerBreathing.setMicrosecondPosition(0);
        playerBreathing.loop(-1);
    }
    public void animateToState(int state) {
        gameState = SWITCH;
        javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameState = state;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    public Clip assignSound(String url, double gain) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream(url));
            clip.open(inputStream);
            ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue((float)gain);
            return clip;
        } catch (Exception e) {}
        return null;
    }
    public void playSound(String url, double delay, double gain) {
        Clip clip = assignSound(url, gain);
        javax.swing.Timer timer = new javax.swing.Timer((int)(delay*1000), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clip.start();
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
        public void updateSize(int endwidth, int endheight, double power) {
            width += Math.signum(endwidth-width) * Math.max(Math.abs(endwidth-width)*power, 1);
            height += Math.signum(endheight-height) * Math.max(Math.abs(endheight-height)*power, 1);
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
                        fillChoices("player");
                        playerBreathing.loop(-1);
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

    private class AbstractButton extends Box {
        public AbstractButton(int X, int Y, int WIDTH, int HEIGHT) {
            super(X, Y, WIDTH, HEIGHT);
        }
        public void draw(Graphics g) {
            if (inside()) {
                g.setColor(new Color(255, 255, 255, 25));
                g.fillRect(x, y, width, height);
            }
        }
    }

    private class ExitPlace extends AbstractButton {
        public ExitPlace() {
            super(0, 0, Frame.WIDTH, Frame.HEIGHT/7);
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
                        playSound("dink.wav", 0, 0);
                    }
                }
            });
        }
    }

    private class PassTurn extends AbstractButton {
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
                                        fillChoices("enemy");
                                        gameState = ENEMYROLL;
                                    } else {
                                        gameState = ENEMYPICK;
                                    }
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();
                            playSound("crack.wav", 0, 1);
                            playerAmbient.stop();
                            enemyAmbient.setMicrosecondPosition(0);
                            enemyAmbient.loop(-1);
                            playerBreathing.stop();
                            enemyBreathing.setMicrosecondPosition(0);
                            enemyBreathing.loop(-1);
                        } else if (gameState == ENEMYVIEW) {
                            gameState = UPDATEBOARD;
                            updateAirs();
                        }
                    }
                }
            });
        }
    }

    private class Card extends Image {
        public static ImageIcon back = new ImageIcon("back.png");
        double flip = 0;
        String type, owner, canPlace;
        ArrayList<Card> modifiers = new ArrayList<Card>();
        boolean toadStopped = false;
        public Card(String TYPE, String CANPLACE) {
            super(TYPE + "_" + "player"+ ".png", CARDWIDTH, CARDHEIGHT);
            type = TYPE;
            canPlace = CANPLACE;
        }
        public Card(String TYPE, String OWNER, String CANPLACE) {
            this(TYPE, CANPLACE);
            owner = OWNER;
        }
        public Card(Card card) {
            this(card.type, card.owner, card.canPlace);
        }
        public boolean inside() {
            return mouseX >= x && mouseX <= x+width && mouseY >= y && mouseY <= y+2*height; // card collider is two times taller
        }
        public void hide(int slot) {
            update((slot+1)*CARDSPACING + slot*width, Frame.HEIGHT, 0.2);
        }
        public void raise(int slot) {
            update((slot+1)*CARDSPACING + slot*width, Frame.HEIGHT - 3*height/2 - 20, 0.3);
        }
        public void lower(int slot) {
            update((slot+1)*CARDSPACING + slot*width, Frame.HEIGHT - height - 20, 0.3);
        }
        public void drawFaceup(Graphics g) {
            super.draw(g);
            for (int i=0; i<modifiers.size(); i++) {
                if (i == 0) {
                    modifiers.get(i).x = x + SPACEPADDING;
                } else {
                    modifiers.get(i).x = x + SPACEWIDTH/2;
                }
                modifiers.get(i).draw(g);
            }
        }
        public void drawFacedown(Graphics g) {
            g.drawImage(back.getImage(), x, y, width, height, Frame.self);
        }
        public void draw(Graphics g) {
            AffineTransform at = ((Graphics2D)g).getTransform();
            if (flip < 0.5) {
                g.translate(x+width/2, y+height/2);
                ((Graphics2D)g).scale(2*(0.5-flip), 1);
                g.translate(-x-width/2, -y-height/2);
                drawFaceup(g);
            } else {
                g.translate(x+width/2, y+height/2);
                ((Graphics2D)g).scale(2*(flip-0.5), 1);
                g.translate(-x-width/2, -y-height/2);
                drawFacedown(g);
            }
            ((Graphics2D)g).setTransform(at);
        }
        public void flip(double endFlip, double power) {
            flip += Math.signum(endFlip - flip) * Math.max(Math.abs(endFlip - flip) * power, 1.0/120); // base speed of one flip/2secs
            if (Math.abs(endFlip - flip) < 0.01) { // if its close then just set it 
                flip = endFlip;
            }
        }
        public void drawHolding(Graphics g) {
            JLabel jl = labels.get(type);
            if (inside()) {
                jl.setBounds(x + 5, y + 5, width - 10, height - 10);
                jl.setVisible(true);
                g.setColor(Color.BLACK);
                g.fillRect(x, y, width, height);
            } else {
                super.draw(g);
                jl.setVisible(false);
            }
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
                        playSound("cardgrab.wav", 0, 6);
                    } else if (gameState == ENEMYPICK) {
                        gameState = ENEMYPLACE;
                        enemyHolding = c;
                        playSound("cardgrab.wav", 0, 6);
                    } else if (gameState == PLAYERROLL) {
                        cardChoices.remove(c);
                        toBeAdded.add(c);
                        playerHand.add(new Card(c)); // add copy of card to the actual hand
                        playSound("cardflip.wav", 0, 6);
                    } else if (gameState == ENEMYROLL) {
                        cardChoices.remove(c);
                        toBeAdded.add(c);
                        enemyHand.add(new Card(c));
                        playSound("cardflip.wav", 0, 6);
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
        ArrayList<Card> actionCards = new ArrayList<Card>();
        
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
            c = card;
        }
        public void assignActionCard(Card card) {
            card.x = x + SPACEPADDING;
            card.y = y + SPACEPADDING;
            card.width = SPACEWIDTH - 2*SPACEPADDING;
            card.height = SPACEHEIGHT - 2*SPACEPADDING;
            card.img = new ImageIcon(card.type + "_action.png");
            actionCards.add(card);
        }
        public void assignModifier(Card card) {
            
            card.y = y + SPACEPADDING + SPACEHEIGHT/2;
            card.width = SPACEWIDTH/2 - SPACEPADDING;
            card.height = SPACEHEIGHT/2 - SPACEPADDING;
            card.img = new ImageIcon(card.type + "_modifier.png");
            c.modifiers.add(card);
        }
        public void drawCardOnBoard(Card card, Graphics g) {
            if (card != null) { // if there is a card in the space
                card.update(x + SPACEPADDING, y + SPACEPADDING, 0.5);
                card.updateSize(SPACEWIDTH - 2*SPACEPADDING, SPACEHEIGHT - 2*SPACEPADDING, 0.5);
                if (card.owner.equals("enemy")) { // rotate card 180 around center
                    ((Graphics2D) g).rotate(Math.PI, x + width/2, y + height/2);
                } 
                card.draw(g); // draws card
                if (card.owner.equals("enemy")) { // rotate back
                    ((Graphics2D) g).rotate(Math.PI, x + width/2, y + height/2);
                } 
            }
        }
        public void draw(Graphics g, double power) {
            drawCardOnBoard(c, g);
            //display action cards
            for (Card ac : actionCards) {
                drawCardOnBoard(ac, g);
            }
            //display airs
            theta += speed; //rotate airs
            displayAirs(g, airs, AIRSRADIUS, theta, x + width/2, y+height/2, power);
            
        }
        public void setListener() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (inside()) {
                        ArrayList<Card> hand = null;
                        Card holding = null;
                        if (gameState == PLAYERPLACE) {
                            hand = playerHand;
                            holding = playerHolding;
                        } else if (gameState == ENEMYPLACE) {
                            hand = enemyHand;
                            holding = enemyHolding;
                        }
                        if (holding != null) {
                            boolean valid = (holding.canPlace == "any" || 
                                            (holding.canPlace == "domain" && 
                                                    ((gameState == PLAYERPLACE && row < 2) || (gameState == ENEMYPLACE && row > 1))) ||
                                            (holding.canPlace == "card" && c != null));
                            if (valid) {
                                if (holding.canPlace == "any") {
                                    assignActionCard(holding);
                                } else if (holding.canPlace == "card") {
                                    assignModifier(holding);
                                } else {
                                    assignCard(holding);
                                    playSound("cardplace.wav", 0, 6);
                                }
                                hand.remove(hand.indexOf(holding));
                                holding = null;
                            }
                        }
                        if (gameState == PLAYERPLACE) {
                            playerHolding = holding;
                        } else if (gameState == ENEMYPLACE) {
                            enemyHolding = holding;
                        }
                    }
                }
            });
        }
    }
}