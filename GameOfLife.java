package gameoflife;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;


public class GameOfLife extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener, KeyListener {

    // WORLD SIZE
    int ROWS = 1000;
    int COLS = 1000;


    // CELL SIZE
    int CELL_SIZE = 5;



    volatile HashSet<Point> grid;


    HashSet<Point>[] saves;



    // =========================
    // CAMERA MOVEMENT
    // =========================

    int cameraX;
    int cameraY;

    double zoom = 5.0;
    
 // GRID SYSTEM

    boolean showGrid = true;

    JButton gridButton;

    int lastMouseX;
    int lastMouseY;


    boolean draggingCamera = false;



    // =========================
    // PAINT SYSTEM
    // =========================

    // 1 = create alive cells
    // -1 = create dead cells
    // 0 = no paint mode

    int paintMode = 0;


    boolean leftDragging = false;
    boolean rightDragging = false;



    // Mouse grid position

    int mouseX = 0;
    int mouseY = 0;




    // =========================
    // COPY SYSTEM
    // =========================

    HashSet<Point> clipboard =
            new HashSet<>();


    boolean copyMode = false;

    boolean pasteMode = false;


    Point copyStart = null;

    Point copyEnd = null;
    
    boolean showCopiedLocation = false;

    Point copiedStartLocation = null;



    HashSet<Point> selectedArea =
            new HashSet<>();


    boolean showSelection = false;




    // =========================
    // ROTATION SYSTEM
    // =========================

    // 0 = normal
    // 1 = 90 degrees
    // 2 = 180 degrees
    // 3 = 270 degrees

    int rotation = 0;





    Timer timer;



    JButton startButton;
    JButton clearButton;
    JButton saveButton;
    JButton loadButton;
    JButton speedButton;

    JButton copyButton;
    JButton pasteButton;



    JComboBox<String> saveMenu;



    boolean running = false;




    // Speed

    int[] speeds = {
    		
    		3,
            16,
            66,
            200

    };



    String[] speedNames = {
    		
    		"300/s",
            "60/s",
            "15/s",
            "5/s"

    };



    int speedIndex = 2;

    
    boolean enterHeld = false;
    Timer stepTimer;
    
    boolean enterStarted = false;

    Timer enterDelayTimer;



    public void centerCamera(){

        cameraX = getWidth() / 2;
        cameraY = getHeight() / 2;

        repaint();

    }
    
    Thread gameThread;
    
    
    public void startGameLoop(){

        gameThread = new Thread(() -> {

        	while(running){

        	    long start = System.nanoTime();


        	    synchronized(grid){

        	        nextGeneration();

        	    }


        	    repaint();


        	    int delay = speeds[speedIndex];


        	    long elapsed =
        	            (System.nanoTime() - start) / 1000000;


        	    long sleepTime =
        	            delay - elapsed;


        	    if(sleepTime > 0){

        	        try{

        	            Thread.sleep(sleepTime);

        	        }
        	        catch(Exception e){}

        	    }

        	}

        });


        gameThread.start();

    }
    
    
    public GameOfLife(){


        grid =
                new HashSet<>();



        saves =
                new HashSet[10];



        for(int i=0;i<10;i++){

            saves[i] =
                    new HashSet<>();

        }



        updateSize();


        setBackground(
                Color.WHITE
        );



        addMouseListener(this);

        addMouseMotionListener(this);
        
        addMouseWheelListener(this);



        addKeyListener(this);

        setFocusable(true);


        
        
        
        
        
        
        timer = new Timer(1000, this);
        
        
        stepTimer = new Timer(speeds[speedIndex], e -> {

            if(!running && enterHeld){

                nextGeneration();
                repaint();

            }

        });


        enterDelayTimer = new Timer(500, e -> {

            if(enterHeld){

                stepTimer.start();

            }

        });

        enterDelayTimer.setRepeats(false);

    }







    public void updateSize(){



        setPreferredSize(

            new Dimension(

                COLS * CELL_SIZE,

                ROWS * CELL_SIZE

            )

        );



        revalidate();

        repaint();


    }








    public void saveGame(int slot){


        saves[slot] =
            new HashSet<>(grid);


    }









    public void loadGame(int slot){


        grid =
            new HashSet<>(saves[slot]);



        repaint();


    }









    // =========================
    // COPY SELECTED AREA
    // =========================


    public void copySelected(){



        clipboard.clear();



        if(copyStart == null ||
           copyEnd == null)

            return;





        int minX =
            Math.min(
                copyStart.x,
                copyEnd.x
            );



        int maxX =
            Math.max(
                copyStart.x,
                copyEnd.x
            );





        int minY =
            Math.min(
                copyStart.y,
                copyEnd.y
            );



        int maxY =
            Math.max(
                copyStart.y,
                copyEnd.y
            );







        for(Point p:grid){



            if(p.x >= minX &&
               p.x <= maxX &&
               p.y >= minY &&
               p.y <= maxY){



                clipboard.add(

                    new Point(

                        p.x-minX,

                        p.y-minY

                    )

                );


            }


        }


    }







    // =========================
    // ROTATE COPY 90 DEGREE
    // =========================


    public void rotateClipboard(){


        HashSet<Point> rotated =
            new HashSet<>();


        for(Point p:clipboard){


            rotated.add(

                new Point(

                    -p.y,

                    p.x

                )

            );


        }



        // move back so preview starts at 0,0

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;



        for(Point p:rotated){


            if(p.x < minX)
                minX = p.x;


            if(p.y < minY)
                minY = p.y;

        }




        clipboard.clear();



        for(Point p:rotated){


            clipboard.add(

                new Point(

                    p.x-minX,

                    p.y-minY

                )

            );


        }



        rotation++;


        if(rotation >= 4)

            rotation = 0;


    }







    // =========================
    // PASTE PREVIEW
    // =========================


    public void drawPastePreview(Graphics g){


        if(!pasteMode)

            return;



        g.setColor(

                new Color(

                        0,

                        150,

                        255,

                        120

                )

        );



        for(Point p:clipboard){



        	g.fillRect(

        	        (int)((mouseX + p.x) * CELL_SIZE * zoom + cameraX),

        	        (int)((-mouseY + p.y) * CELL_SIZE * zoom + cameraY),

        	        (int)(CELL_SIZE * zoom),

        	        (int)(CELL_SIZE * zoom)

        	);


        }


    }
    
    
    public void drawCopyPreview(Graphics g){


        if(!copyMode)

            return;



        int x =
                (int)(mouseX * CELL_SIZE * zoom + cameraX);


        int y =
                (int)(-mouseY * CELL_SIZE * zoom + cameraY);



        g.setColor(

                new Color(

                        0,

                        150,

                        255,

                        80

                )

        );



        g.fillRect(

                x,

                y,

                (int)(CELL_SIZE * zoom),
                (int)(CELL_SIZE * zoom)

        );



        g.setColor(Color.BLUE);


        g.drawRect(

                x,

                y,

                (int)(CELL_SIZE * zoom),

                (int)(CELL_SIZE * zoom)

        );


    }
    
    
    public void drawCopiedStart(Graphics g){


        if(!showCopiedLocation ||
           copiedStartLocation == null)

            return;



        int x =
        		(int)(copiedStartLocation.x * CELL_SIZE * zoom
        				+ cameraX);


        int y =
        		(int)(copiedStartLocation.y * CELL_SIZE * zoom
        				+ cameraY);



        g.setColor(

                new Color(

                        0,

                        150,

                        255,

                        150

                )

        );


        g.fillRect(

                x,

                y,

                (int)(CELL_SIZE * zoom),
                (int)(CELL_SIZE * zoom)

        );


        g.setColor(Color.BLUE);


        g.drawRect(

                x,

                y,

                (int)(CELL_SIZE * zoom),

                (int)(CELL_SIZE * zoom)

        );


    }
    
    
 // =========================
 // DRAW GRID
 // =========================

 // =========================
 // DRAW GRID
 // =========================

    public void drawGrid(Graphics g){

        if(!showGrid)
            return;


        int spacing;


        // Change these values for zoom switching
        if(zoom >= 3){

            spacing = 1;

        }
        else if(zoom >= 0.5){

            spacing = 5;

        }
        else{

            spacing = 25;

        }



        double scaledCell =
                CELL_SIZE * zoom;



        // Find visible world area

        int startX =
                (int)Math.floor(
                        (-cameraX) / scaledCell
                );

        int startY =
                (int)Math.floor(
                        (-cameraY) / scaledCell
                );


        int endX =
                startX +
                (int)(getWidth() / scaledCell)
                + 2;


        int endY =
                startY +
                (int)(getHeight() / scaledCell)
                + 2;



        // Snap to grid spacing

        startX =
                Math.floorDiv(startX, spacing)
                * spacing;


        startY =
                Math.floorDiv(startY, spacing)
                * spacing;



        g.setColor(new Color(200,200,200,120));



        // Vertical lines

        for(int x = startX; x <= endX; x += spacing){


            int screenX =
                    (int)(
                        x * scaledCell
                        + cameraX
                    );


            g.drawLine(
                    screenX,
                    0,
                    screenX,
                    getHeight()
            );

        }




        // Horizontal lines

        for(int y = startY; y <= endY; y += spacing){


            int screenY =
                    (int)(
                        y * scaledCell
                        + cameraY
                    );


            g.drawLine(
                    0,
                    screenY,
                    getWidth(),
                    screenY
            );

        }

    }
    
    
    
    @Override
    protected void paintComponent(Graphics g){


        super.paintComponent(g);
        
        



        g.setColor(Color.WHITE);

        g.fillRect(

                0,

                0,

                getWidth(),

                getHeight()

        );

     // DRAW GRID + ORIGIN

        drawGrid(g);


        if(showGrid){

            g.setColor(Color.RED);


            // Vertical axis X = 0

            g.drawLine(
                    cameraX,
                    0,
                    cameraX,
                    getHeight()
            );


            // Horizontal axis Y = 0

            g.drawLine(
                    0,
                    cameraY,
                    getWidth(),
                    cameraY
            );

        }

        // DRAW GRID LAST
        drawGrid(g);

   
     // DRAW ALIVE CELLS

        g.setColor(Color.BLACK);

        synchronized(grid){

            for(Point p:grid){

                g.fillRect(
                    (int)(p.x * CELL_SIZE * zoom + cameraX),
                    (int)(p.y * CELL_SIZE * zoom + cameraY),
                    (int)(CELL_SIZE * zoom),
                    (int)(CELL_SIZE * zoom)
                );

            }

        }








        // BLUE COPY AREA

        if(showSelection){


            g.setColor(

                    new Color(

                            0,

                            100,

                            255,

                            100

                    )

            );



            for(Point p:selectedArea){



            	g.fillRect(

            	        (int)(p.x * CELL_SIZE * zoom + cameraX),

            	        (int)(p.y * CELL_SIZE * zoom + cameraY),

            	        (int)(CELL_SIZE * zoom),

            	        (int)(CELL_SIZE * zoom)

            	);


            }


        }





        drawCopyPreview(g);


        drawCopiedStart(g);


        // PASTE PREVIEW

        drawPastePreview(g);





        // COORDINATES

        g.setColor(Color.RED);


        g.drawString(

                "Mouse: X=" + mouseX +
                " Y=" + mouseY,

                10,

                15

        );





    }




    public void pauseGame(){

        if(running){

            running = false;

            if(startButton != null)
                startButton.setText("Start");

        }

    }


    
    
    
    private void nextGeneration(){

        synchronized(grid){

            HashMap<Point,Integer> neighbors = new HashMap<>();


            for(Point cell : grid){

                for(int x=-1; x<=1; x++){

                    for(int y=-1; y<=1; y++){

                        if(x==0 && y==0)
                            continue;


                        Point p = new Point(
                                cell.x + x,
                                cell.y + y
                        );


                        neighbors.put(
                                p,
                                neighbors.getOrDefault(p,0) + 1
                        );

                    }
                }
            }



            HashSet<Point> next = new HashSet<>();


            for(Map.Entry<Point,Integer> entry : neighbors.entrySet()){

                Point p = entry.getKey();

                int amount = entry.getValue();


                if(amount == 3 ||
                  (amount == 2 && grid.contains(p))){

                    next.add(new Point(p.x,p.y));

                }

            }


            // replace grid safely
            grid = next;

        }

    }







    @Override
    public void mousePressed(MouseEvent e){


        requestFocus();



        // RIGHT CLICK = MOVE CAMERA

        if(SwingUtilities.isRightMouseButton(e)){


            rightDragging = true;

            draggingCamera = true;


            lastMouseX = e.getX();

            lastMouseY = e.getY();


            return;

        }





     // LEFT CLICK

        if(SwingUtilities.isLeftMouseButton(e)){



        	int col =
        	        (int)Math.floor(
        	                (e.getX() - cameraX) / (CELL_SIZE * zoom)
        	        );


        	int row =
        	        (int)Math.floor(
        	                (e.getY() - cameraY) / (CELL_SIZE * zoom)
        	        );


            Point p =
                    new Point(col,row);




            // =====================
            // COPY MODE
            // =====================

            if(copyMode){


            	if(copyStart == null){


            	    copyStart = p;


            	    // show starting position immediately

            	    copiedStartLocation =
            	            new Point(p.x, p.y);


            	    showCopiedLocation = true;


            	    repaint();


            	}

                else{


                    copyEnd = p;


                    selectedArea.clear();



                    int minX =
                            Math.min(copyStart.x, copyEnd.x);

                    int maxX =
                            Math.max(copyStart.x, copyEnd.x);


                    int minY =
                            Math.min(copyStart.y, copyEnd.y);

                    int maxY =
                            Math.max(copyStart.y, copyEnd.y);



                    for(int y=minY;y<=maxY;y++){

                        for(int x=minX;x<=maxX;x++){


                            selectedArea.add(
                                    new Point(x,y)
                            );


                        }

                    }



                    copySelected();

                    copyMode = false;

                    copyButton.setText("Copy");

                    showSelection = true;

                    showCopiedLocation = false;



                    // remove blue selection after 1 second

                    Timer remove = new Timer(

                            1000,

                            event -> {

                                showSelection = false;

                                showCopiedLocation = false;

                                selectedArea.clear();

                                repaint();

                            }

                    );


                    remove.setRepeats(false);

                    remove.start();


                }


                repaint();

                return;


            }





            // =====================
            // PASTE MODE
            // =====================

            if(pasteMode){


                for(Point cell:clipboard){


                    grid.add(

                            new Point(

                                    mouseX + cell.x,

                                    -mouseY + cell.y

                            )

                    );


                }


                pasteMode = false;

                pasteButton.setText("Paste");

                rotation = 0;


                repaint();

                return;


            }






            // =====================
            // NORMAL PAINTING
            // =====================



            pauseGame();

            leftDragging = true;



            if(grid.contains(p)){


                paintMode=-1;

                grid.remove(p);


            }

            else{


                paintMode=1;

                grid.add(p);


            }



            repaint();


        }



    }



    @Override
    public void mouseWheelMoved(MouseWheelEvent e){

        double oldZoom = zoom;


        if(e.getWheelRotation() < 0){

            // scroll forward = zoom in

            zoom += 0.2;

        }
        else{

            // scroll backward = zoom out

            zoom -= 0.2;

        }



        // limits

        if(zoom > 10)

            zoom = 10;


        if(zoom < 0.2)

            zoom = 0.2;



        // keep mouse position fixed

        double worldX =
                (e.getX() - cameraX) / oldZoom;


        double worldY =
                (e.getY() - cameraY) / oldZoom;



        cameraX =
                (int)(e.getX() - worldX * zoom);


        cameraY =
                (int)(e.getY() - worldY * zoom);



        repaint();

    }





    @Override
    public void mouseMoved(MouseEvent e){



    	mouseX =
    	        (int)Math.floor(
    	                (e.getX() - cameraX) / (CELL_SIZE * zoom)
    	        );


    	mouseY =
    	        -(int)Math.floor(
    	                (e.getY() - cameraY) / (CELL_SIZE * zoom)
    	        );



        repaint();


    }
    

    @Override
    public void keyPressed(KeyEvent e){


        // ENTER KEY
        if(e.getKeyCode() == KeyEvent.VK_ENTER){


            // If game is running, pause it
            if(running){

                running = false;

                if(startButton != null)
                    startButton.setText("Start");

                return;

            }


            // Start detecting hold
            enterHeld = true;


            // wait 0.5 seconds before auto generations
            enterDelayTimer.start();


        }



        // ROTATE WHILE PASTING
        if(e.getKeyCode() == KeyEvent.VK_R
            && pasteMode){


            rotateClipboard();

            repaint();

        }


    }




    @Override
    public void keyReleased(KeyEvent e){


        if(e.getKeyCode() == KeyEvent.VK_ENTER){


            // If released before 0.5 sec
            if(enterDelayTimer.isRunning()){

                enterDelayTimer.stop();

                nextGeneration();
                repaint();

            }


            enterHeld = false;

            stepTimer.stop();


        }

    }



    @Override
    public void keyTyped(KeyEvent e){}









    @Override
    public void actionPerformed(ActionEvent e){



        if(e.getSource()==timer){

        	
            nextGeneration();


            repaint();



        }







        else if(e.getSource()==startButton){



            running = !running;




            if(running){

                startGameLoop();

                startButton.setText("Pause");

            }


            else{


            	running = false;


                startButton.setText(
                    "Start"
                );


            }


        }








        else if(e.getSource()==clearButton){



            timer.stop();



            running=false;



            startButton.setText(
                "Start"
            );



            grid.clear();



            repaint();



        }


    }







    @Override
    public void mouseDragged(MouseEvent e){

    	  mouseX =
    	            (int)Math.floor(
    	                    (e.getX() - cameraX) / (CELL_SIZE * zoom)
    	            );


    	    mouseY =
    	            -(int)Math.floor(
    	                    (e.getY() - cameraY) / (CELL_SIZE * zoom)
    	            );
        // CAMERA DRAG

        if(rightDragging){



            int dx =
                    e.getX() - lastMouseX;


            int dy =
                    e.getY() - lastMouseY;



            cameraX += dx;

            cameraY += dy;



            lastMouseX = e.getX();

            lastMouseY = e.getY();



            repaint();

            return;

        }






        // PAINT DRAG

        if(leftDragging){



        	int col =
        	        (int)Math.floor(
        	                (e.getX() - cameraX) / (CELL_SIZE * zoom)
        	        );


        	int row =
        	        (int)Math.floor(
        	                (e.getY() - cameraY) / (CELL_SIZE * zoom)
        	        );



            Point p =
                    new Point(col,row);




            synchronized(grid){

                if(paintMode == 1){

                    // only create alive cells
                    grid.add(p);

                }

                else if(paintMode == -1){

                    // only remove alive cells
                    grid.remove(p);

                }

            }



            repaint();

        }


    }



    @Override
    public void mouseClicked(MouseEvent e){}



    @Override
    public void mouseReleased(MouseEvent e){



        if(SwingUtilities.isRightMouseButton(e)){


            rightDragging = false;

            draggingCamera = false;


        }



        if(SwingUtilities.isLeftMouseButton(e)){


            leftDragging = false;

            paintMode = 0;


        }


    }



    @Override
    public void mouseEntered(MouseEvent e){}



    @Override
    public void mouseExited(MouseEvent e){}










    public static void main(String[] args){


        SwingUtilities.invokeLater(() -> {



            JFrame frame =
                    new JFrame(
                            "Conway's Game of Life"
                    );




            GameOfLife game =
                    new GameOfLife();





            JPanel buttons =
                    new JPanel();







            game.startButton =
                    new JButton("Start");


            game.clearButton =
                    new JButton("Clear");


            game.saveButton =
                    new JButton("Save");


            game.loadButton =
                    new JButton("Load");


            game.speedButton =
                    new JButton(
                            "Speed: 15/s"
                    );



            game.copyButton =
                    new JButton(
                            "Copy"
                    );


            game.pasteButton =
                    new JButton(
                            "Paste"
                    );

            game.gridButton =
                    new JButton("✓ Grid");





            game.saveMenu =
                    new JComboBox<>();



            for(int i=1;i<=10;i++){


                game.saveMenu.addItem(

                        "Save " + i

                );


            }







            game.startButton.addActionListener(game);


            game.clearButton.addActionListener(game);
            
            
            

            game.gridButton.addActionListener(e -> {


                game.showGrid = !game.showGrid;


                if(game.showGrid)

                    game.gridButton.setText("✓ Grid");

                else

                    game.gridButton.setText("Grid");


                game.repaint();


            });




            game.copyButton.addActionListener(e -> {

                // Turn off paste if copy is activated
                if(!game.copyMode){

                    game.pasteMode = false;
                    game.pasteButton.setText("Paste");

                }


                game.copyMode = !game.copyMode;


                if(game.copyMode){

                    game.copyStart = null;
                    game.copyEnd = null;
                    game.rotation = 0;

                    game.copyButton.setText("✓ Copy");

                }
                else{

                    game.copyStart = null;
                    game.copyEnd = null;
                    game.showCopiedLocation = false;
                    game.selectedArea.clear();

                    game.copyButton.setText("Copy");

                }


                game.repaint();

            });






            game.pasteButton.addActionListener(e -> {


                if(game.clipboard.isEmpty())
                    return;


                // Turn off copy if paste is activated
                if(!game.pasteMode){

                    game.copyMode = false;
                    game.copyButton.setText("Copy");

                    game.copyStart = null;
                    game.copyEnd = null;
                    game.showCopiedLocation = false;
                    game.selectedArea.clear();

                }


                game.pasteMode = !game.pasteMode;


                if(game.pasteMode){

                    game.rotation = 0;

                    game.requestFocus();

                    game.pasteButton.setText("✓ Paste");

                }
                else{

                    game.pasteButton.setText("Paste");

                }


                game.repaint();

            });







            game.speedButton.addActionListener(e -> {



                game.speedIndex++;


                if(game.speedIndex >= game.speeds.length)

                    game.speedIndex = 0;



                game.timer.setDelay(
                        game.speeds[game.speedIndex]
                );

                game.stepTimer.setDelay(
                        game.speeds[game.speedIndex]
                );



                game.speedButton.setText(

                        "Speed: "
                        +
                        game.speedNames[game.speedIndex]

                );


            });








            game.saveButton.addActionListener(e -> {


                game.saveGame(

                        game.saveMenu.getSelectedIndex()

                );


            });






            game.loadButton.addActionListener(e -> {


                game.loadGame(

                        game.saveMenu.getSelectedIndex()

                );


            });








            buttons.add(game.startButton);

            buttons.add(game.clearButton);

            buttons.add(game.copyButton);

            buttons.add(game.pasteButton);
            
            buttons.add(game.gridButton);

            buttons.add(game.saveButton);

            buttons.add(game.loadButton);

            buttons.add(game.speedButton);

            buttons.add(game.saveMenu);




            frame.setLayout(

                    new BorderLayout()

            );






            frame.add(

                    game,

                    BorderLayout.CENTER

            );






            frame.add(

                    buttons,

                    BorderLayout.SOUTH

            );






            frame.setSize(

                    1000,

                    700

            );



            frame.setResizable(true);



            frame.setDefaultCloseOperation(

                    JFrame.EXIT_ON_CLOSE

            );



            frame.setLocationRelativeTo(null);



            frame.setVisible(true);
            
            game.cameraX = game.getWidth()/2;
            game.cameraY = game.getHeight()/2;
            game.repaint();
            
            
            game.centerCamera();


        });


    }
    
    

}